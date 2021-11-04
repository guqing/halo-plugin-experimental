package run.halo.app.extensions;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import run.halo.app.extensions.annotation.ExtController;
import run.halo.app.extensions.annotation.ExtRestController;

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
            log.debug("Destroyed plugin bean [{}] from application", existingBean);
        });
    }

    public void unregisterExtension(String pluginId, Object existingBean) {
        Assert.notNull(pluginId, "pluginId must not be null");
        Assert.notNull(existingBean, "existingBean must not be null");
        springPluginManager.getApplicationContext()
            .getAutowireCapableBeanFactory()
            .destroyBean(existingBean);
        injectedBeanRegistry.unregister(pluginId, existingBean.getClass());
        log.debug("Removed bean [{}] from application", existingBean);
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
        Set<Class<?>> classes = this.injectedBeanRegistry.getControllerClassesByPluginId(pluginId);
        if (CollectionUtils.isEmpty(classes)) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(classes);
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

        private final Map<String, Set<Class<?>>> registry = new HashMap<>();
        private final Map<String, Set<Class<?>>> controllerLookup = new ConcurrentHashMap<>();
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        public void register(String pluginId, Class<?> clazz) {
            this.readWriteLock.writeLock().lock();
            try {
                Set<Class<?>> classSet =
                    registry.computeIfAbsent(pluginId, key -> new LinkedHashSet<>());
                classSet.add(clazz);

                if (isController(clazz)) {
                    Set<Class<?>> controllerClasses =
                        controllerLookup.computeIfAbsent(pluginId, key -> new LinkedHashSet<>());
                    controllerClasses.add(clazz);
                }
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

        private boolean isController(Class<?> clazz) {
            Annotation[] declaredAnnotations = clazz.getDeclaredAnnotations();
            for (Annotation annotation : declaredAnnotations) {
                Class<?> aClass = annotation.annotationType();
                if (aClass.isAssignableFrom(ExtController.class)) {
                    return true;
                }
                if (aClass.isAssignableFrom(ExtRestController.class)) {
                    return true;
                }
            }
            return false;
        }

        public void unregister(String pluginId, Class<?> bean) {
            this.readWriteLock.writeLock().lock();
            try {
                Set<Class<?>> classes = registry.get(pluginId);
                if (CollectionUtils.isEmpty(classes)) {
                    return;
                }
                classes.remove(bean);
                Set<Class<?>> controllers = controllerLookup.get(pluginId);
                if (CollectionUtils.isEmpty(controllers)) {
                    return;
                }
                controllers.remove(bean);
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

        public Set<Class<?>> unregister(String pluginId) {
            this.readWriteLock.writeLock().lock();
            try {
                Set<Class<?>> removed = registry.remove(pluginId);
                if (CollectionUtils.isEmpty(removed)) {
                    return Collections.emptySet();
                }
                controllerLookup.remove(pluginId);
                return removed;
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

        public Set<Class<?>> getControllerClassesByPluginId(String pluginId) {
            return controllerLookup.get(pluginId);
        }

        public Set<Class<?>> getClassesByPluginId(String pluginId) {
            return registry.get(pluginId);
        }

        public boolean containsPlugin(String pluginId) {
            return registry.containsKey(pluginId);
        }
    }
}
