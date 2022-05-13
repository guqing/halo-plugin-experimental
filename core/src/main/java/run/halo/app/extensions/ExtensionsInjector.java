package run.halo.app.extensions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;
import run.halo.app.extensions.extpoint.ExtensionPointFinder;
import run.halo.app.extensions.registry.ExtensionClassRegistry;
import run.halo.app.extensions.registry.ExtensionClassRegistry.ClassDescriptor;
import run.halo.app.extensions.internal.ExtensionInjectedEvent;
import run.halo.app.extensions.registry.ExtensionContextRegistry;

/**
 * @author guqing
 * @since 2021-11-01
 */
public class ExtensionsInjector {

    private static final Logger log = LoggerFactory.getLogger(ExtensionsInjector.class);
    protected final SpringPluginManager springPluginManager;
    private final ExtensionContextRegistry contextRegistry = ExtensionContextRegistry.getInstance();

    public ExtensionsInjector(SpringPluginManager springPluginManager) {
        this.springPluginManager = springPluginManager;
    }

    public ApplicationContext getApplicationContext() {
        return this.springPluginManager.getApplicationContext();
    }

    public void unregisterExtensions(String pluginId) {
        Assert.notNull(pluginId, "pluginId must not be null");
        contextRegistry.unregister(pluginId);
    }

    public void unregisterExtension(String pluginId, Object existingBean) {
        Assert.notNull(pluginId, "pluginId must not be null");
        Assert.notNull(existingBean, "existingBean must not be null");
        DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) springPluginManager.getApplicationContext()
                .getAutowireCapableBeanFactory();
        beanFactory.destroySingleton(existingBean.getClass().getName());
//        beanFactory.removeBeanDefinition(existingBean.getClass().getName());
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
                    //classRegistry.register(plugin.getPluginId(), extensionClass);
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public void injectExtensionByPluginId(String pluginId) {
        if (contextRegistry.containsContext(pluginId)) {
            return;
        }
        GenericApplicationContext pluginContext = new GenericApplicationContext();
        pluginContext.setParent(springPluginManager.getApplicationContext());

        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory)pluginContext.getBeanFactory();

        AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor =
            new AutowiredAnnotationBeanPostProcessor();
        autowiredAnnotationBeanPostProcessor.setBeanFactory(beanFactory);

        beanFactory.addBeanPostProcessor(autowiredAnnotationBeanPostProcessor);

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
                pluginContext.registerBean(extensionClass);
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }
        contextRegistry.register(pluginId, pluginContext);
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
