package run.halo.app.extensions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import run.halo.app.extensions.registry.ExtensionClassRegistry;
import run.halo.app.extensions.registry.ExtensionClassRegistry.ClassDescriptor;
import run.halo.app.extensions.internal.ExtensionInjectedEvent;

/**
 * @author guqing
 * @since 2021-11-01
 */
public class ExtensionsInjector {

    private static final Logger log = LoggerFactory.getLogger(ExtensionsInjector.class);
    protected final SpringPluginManager springPluginManager;
    private final ExtensionClassRegistry classRegistry = ExtensionClassRegistry.getInstance();

    public ExtensionsInjector(SpringPluginManager springPluginManager) {
        this.springPluginManager = springPluginManager;
    }

    public ApplicationContext getApplicationContext() {
        return this.springPluginManager.getApplicationContext();
    }

    public void unregisterExtensions(String pluginId) {
        Assert.notNull(pluginId, "pluginId must not be null");
        ApplicationContext applicationContext = springPluginManager.getApplicationContext();
        for (ClassDescriptor descriptor : classRegistry.unregister(pluginId)) {
            try {
                Object existingBean = applicationContext.getBean(descriptor.getName());
                applicationContext.getAutowireCapableBeanFactory()
                    .destroyBean(existingBean);
                log.debug("Destroyed plugin bean [{}] from application",
                    existingBean.getClass().getName());
            } catch (BeansException e) {
                log.warn(e.getMessage());
            }
        }
    }

    public void unregisterExtension(String pluginId, Object existingBean) {
        Assert.notNull(pluginId, "pluginId must not be null");
        Assert.notNull(existingBean, "existingBean must not be null");
        springPluginManager.getApplicationContext()
            .getAutowireCapableBeanFactory()
            .destroyBean(existingBean);
        classRegistry.unregister(pluginId, existingBean.getClass());
        log.debug("Destroyed plugin bean [{}] from application",
            existingBean.getClass().getName());
    }

    public void injectExtensions() {
        // add extensions from classpath (non plugin)
        Set<String> extensionClassNames = springPluginManager.getExtensionClassNames(null);
        for (String extensionClassName : extensionClassNames) {
            try {
                log.debug("Register extension '{}' as bean", extensionClassName);
                Class<?> extensionClass = getClass().getClassLoader().loadClass(extensionClassName);
                registerExtensionAsBean(extensionClass);
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
                    registerExtensionAsBean(extensionClass);
                    classRegistry.register(plugin.getPluginId(), extensionClass);
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public void injectExtensionByPluginId(String pluginId) {
        if (classRegistry.containsPlugin(pluginId)) {
            return;
        }
        Set<String> extensionClassNames = springPluginManager.getExtensionClassNames(pluginId);
        // add extensions for each started plugin
        PluginWrapper plugin = springPluginManager.getPlugin(pluginId);
        log.debug("Registering extensions of the plugin '{}' as beans", pluginId);
        for (String extensionClassName : extensionClassNames) {
            log.debug("Load extension class '{}'", extensionClassName);
            try {
                Class<?> extensionClass =
                    plugin.getPluginClassLoader().loadClass(extensionClassName);
                log.debug("Register a extension class '{}' as a bean", extensionClassName);
                classRegistry.register(pluginId, extensionClass);
                registerExtensionAsBean(extensionClass);
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }
        getApplicationContext().publishEvent(new ExtensionInjectedEvent(this, plugin));
    }

    /**
     * Register an extension as bean. Current implementation register extension as singleton using
     * {@code beanFactory.registerSingleton()}. The extension instance is created using {@code
     * pluginManager.getExtensionFactory().create(extensionClass)}. The bean name is the extension
     * class name. Override this method if you wish other register strategy.
     */
    protected void registerExtensionAsBean(Class<?> extensionClass) {
        Map<String, ?> extensionBeanMap =
            springPluginManager.getApplicationContext().getBeansOfType(extensionClass);
        if (extensionBeanMap.isEmpty()) {
            springPluginManager.getExtensionFactory().create(extensionClass);
        } else {
            log.debug("Bean registration aborted! Extension '{}' already existed as bean!",
                extensionClass.getName());
        }
    }
}
