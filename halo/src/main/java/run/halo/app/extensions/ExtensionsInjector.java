package run.halo.app.extensions;

import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author guqing
 * @since 2021-11-01
 */
public class ExtensionsInjector {

    private static final Logger log = LoggerFactory.getLogger(ExtensionsInjector.class);

    protected final SpringPluginManager springPluginManager;
    private final InjectedBeanRegistry injectedBeanRegistry = new InjectedBeanRegistry();

    public ExtensionsInjector(SpringPluginManager springPluginManager) {
        this.springPluginManager = springPluginManager;
    }

    public void unregisterExtensions(String pluginId) {
        Assert.notNull(pluginId, "pluginId must not be null");
        ApplicationContext applicationContext = springPluginManager.getApplicationContext();
        injectedBeanRegistry.unregister(pluginId).forEach(aClass -> {
            Object existingBean = applicationContext.getBean(aClass);
            applicationContext.getAutowireCapableBeanFactory()
                .destroyBean(existingBean);
            log.debug("Destroyed plugin bean [{}] from application",
                existingBean.getClass().getName());
        });
    }

    public void unregisterExtension(String pluginId, Object existingBean) {
        Assert.notNull(pluginId, "pluginId must not be null");
        Assert.notNull(existingBean, "existingBean must not be null");
        springPluginManager.getApplicationContext()
            .getAutowireCapableBeanFactory()
            .destroyBean(existingBean);
        injectedBeanRegistry.unregister(pluginId, existingBean.getClass());
        log.debug("Removed bean [{}] from application", existingBean.getClass().getName());
    }

    public void injectExtensions() {
        // add extensions from classpath (non plugin)
        Set<String> extensionClassNames = springPluginManager.getExtensionClassNames(null);
        for (String extensionClassName : extensionClassNames) {
            try {
                log.debug("Register extension '{}' as bean", extensionClassName);
                Class<?> extensionClass = getClass().getClassLoader().loadClass(extensionClassName);
                registerExtension(extensionClass);
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }

        // add extensions for each started plugin
        List<PluginWrapper> startedPlugins = springPluginManager.getStartedPlugins();
        for (PluginWrapper plugin : startedPlugins) {
            log.debug("Registering extensions of the plugin '{}' as beans", plugin.getPluginId());
            extensionClassNames = springPluginManager.getExtensionClassNames(plugin.getPluginId());
            for (String extensionClassName : extensionClassNames) {
                try {
                    log.debug("Register extension '{}' as bean", extensionClassName);
                    Class<?> extensionClass =
                        plugin.getPluginClassLoader().loadClass(extensionClassName);
                    registerExtension(extensionClass);
                    injectedBeanRegistry.register(plugin.getPluginId(), extensionClass);
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public void injectExtensionByPluginId(String pluginId) {
        if (injectedBeanRegistry.containsPlugin(pluginId)) {
            return;
        }
        Set<String> extensionClassNames = springPluginManager.getExtensionClassNames(pluginId);
        // add extensions for each started plugin
        PluginWrapper plugin = springPluginManager.getPlugin(pluginId);
        log.debug("Registering extensions of the plugin '{}' as beans", pluginId);
        for (String extensionClassName : extensionClassNames) {
            try {
                log.debug("Load extension class '{}'", extensionClassName);
                Class<?> extensionClass =
                    plugin.getPluginClassLoader().loadClass(extensionClassName);
                log.debug("Register a extension class '{}' as a bean", extensionClassName);
                registerExtension(extensionClass);
                injectedBeanRegistry.register(pluginId, extensionClass);
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public Set<Class<?>> getControllers(String pluginId) {
        injectedBeanRegistry.acquireReadLock();
        try {
            Map<String, Set<ClassDescriptor>> registrations =
                this.injectedBeanRegistry.getRegistrations();
            if (!registrations.containsKey(pluginId)) {
                return Collections.emptySet();
            }
            Set<Class<?>> classes = registrations.get(pluginId)
                .stream()
                .filter(clazz -> !clazz.isComponent())
                .filter(ClassDescriptor::isController)
                .map(ClassDescriptor::getTargetClass)
                .collect(Collectors.toSet());
            return ImmutableSet.copyOf(classes);
        } finally {
            injectedBeanRegistry.releaseReadLock();
        }
    }

    public Set<Class<?>> getListenerClasses(String pluginId) {
        injectedBeanRegistry.acquireReadLock();
        try {
            Map<String, Set<ClassDescriptor>> registrations =
                this.injectedBeanRegistry.getRegistrations();
            if (!registrations.containsKey(pluginId)) {
                return Collections.emptySet();
            }
            Set<Class<?>> classes = registrations.get(pluginId)
                .stream()
                .filter(ClassDescriptor::isListener)
                .map(ClassDescriptor::getTargetClass)
                .collect(Collectors.toSet());
            return ImmutableSet.copyOf(classes);
        } finally {
            injectedBeanRegistry.releaseReadLock();
        }
    }

    /**
     * Register an extension as bean. Current implementation register extension as singleton using
     * {@code beanFactory.registerSingleton()}. The extension instance is created using {@code
     * pluginManager.getExtensionFactory().create(extensionClass)}. The bean name is the extension
     * class name. Override this method if you wish other register strategy.
     */
    protected void registerExtension(Class<?> extensionClass) {
        Map<String, ?> extensionBeanMap =
            springPluginManager.getApplicationContext().getBeansOfType(extensionClass);
        if (extensionBeanMap.isEmpty()) {
            springPluginManager.getExtensionFactory().create(extensionClass);
        } else {
            log.debug("Bean registration aborted! Extension '{}' already existed as bean!",
                extensionClass.getName());
        }
    }

    static class InjectedBeanRegistry {

        private final Map<String, Set<ClassDescriptor>> registry = new HashMap<>();
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        public Map<String, Set<ClassDescriptor>> getRegistrations() {
            return this.registry;
        }

        /**
         * Acquire the read lock when using getMappings and getMappingsByUrl.
         */
        public void acquireReadLock() {
            this.readWriteLock.readLock().lock();
        }

        /**
         * Release the read lock after using getMappings and getMappingsByUrl.
         */
        public void releaseReadLock() {
            this.readWriteLock.readLock().unlock();
        }

        public void register(String pluginId, Class<?> clazz) {
            this.readWriteLock.writeLock().lock();
            try {
                ClassDescriptor classDescriptor = new ClassDescriptor(clazz);
                Set<ClassDescriptor> classSet =
                    registry.computeIfAbsent(pluginId, key -> new LinkedHashSet<>());
                classSet.add(classDescriptor);
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

        public void unregister(String pluginId, Class<?> bean) {
            this.readWriteLock.writeLock().lock();
            try {
                Set<ClassDescriptor> classes = registry.get(pluginId);
                if (CollectionUtils.isEmpty(classes)) {
                    return;
                }
                classes.remove(new ClassDescriptor(bean));
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

        public Set<Class<?>> unregister(String pluginId) {
            this.readWriteLock.writeLock().lock();
            try {
                Set<ClassDescriptor> removed = registry.remove(pluginId);
                if (CollectionUtils.isEmpty(removed)) {
                    return Collections.emptySet();
                }
                return removed.stream()
                    .map(ClassDescriptor::getTargetClass)
                    .collect(Collectors.toSet());
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

        public boolean containsPlugin(String pluginId) {
            return registry.containsKey(pluginId);
        }
    }

    static class ClassDescriptor {

        final Class<?> clazz;
        String name;
        boolean isController;
        boolean isListener;
        boolean isComponent;

        public ClassDescriptor(Class<?> targetClass) {
            Assert.notNull(targetClass, "The targetClass must not be null.");
            this.clazz = targetClass;
            init();
        }

        static String getSimpleName(final String className) {
            return className.substring(className.lastIndexOf('.') + 1);
        }

        private void init() {
            this.name = this.clazz.getName();

            // Is it a controller?
            this.isController = AnnotatedElementUtils.hasAnnotation(this.clazz, Controller.class);

            // Specialized classes of @component include @service,@repository,@controller and etc.
            this.isComponent = AnnotatedElementUtils.hasAnnotation(this.clazz, Component.class);

            // Is it a listener?
            if (ApplicationListener.class.isAssignableFrom(this.clazz)) {
                this.isListener = true;
            } else {
                for (Method declaredMethod : clazz.getDeclaredMethods()) {
                    if (AnnotatedElementUtils.hasAnnotation(declaredMethod, EventListener.class)) {
                        this.isListener = true;
                        break;
                    }
                }
            }
        }

        public String getSimpleName() {
            return getSimpleName(name);
        }

        public Class<?> getTargetClass() {
            return clazz;
        }

        public String getName() {
            return name;
        }

        public boolean isController() {
            return isController;
        }

        public boolean isListener() {
            return isListener;
        }

        public boolean isComponent() {
            return isComponent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ClassDescriptor that = (ClassDescriptor) o;
            return clazz.equals(that.clazz);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz);
        }
    }
}
