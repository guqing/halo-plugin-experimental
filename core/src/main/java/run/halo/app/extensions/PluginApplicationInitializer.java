package run.halo.app.extensions;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import run.halo.app.extensions.registry.ExtensionContextRegistry;

/**
 * @author guqing
 * @since 2021-11-01
 */
@Slf4j
public class PluginApplicationInitializer {
    protected final SpringPluginManager springPluginManager;
    private final ExtensionContextRegistry contextRegistry = ExtensionContextRegistry.getInstance();

    public PluginApplicationInitializer(SpringPluginManager springPluginManager) {
        this.springPluginManager = springPluginManager;
    }

    public ApplicationContext getRootApplicationContext() {
        return this.springPluginManager.getRootApplicationContext();
    }

    private PluginApplicationContext createPluginApplicationContext(String pluginId) {
        PluginApplicationContext pluginApplicationContext = new PluginApplicationContext();
        pluginApplicationContext.setParent(getRootApplicationContext());

        DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) pluginApplicationContext.getBeanFactory();

        AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor =
            new AutowiredAnnotationBeanPostProcessor();
        autowiredAnnotationBeanPostProcessor.setBeanFactory(beanFactory);

        beanFactory.addBeanPostProcessor(autowiredAnnotationBeanPostProcessor);

        contextRegistry.register(pluginId, pluginApplicationContext);
        return pluginApplicationContext;
    }

    private void initApplicationContext(String pluginId) {
        if (contextRegistry.containsContext(pluginId)) {
            log.debug("Plugin application context for [{}] has bean initialized.", pluginId);
            return;
        }
        PluginApplicationContext pluginApplicationContext =
            createPluginApplicationContext(pluginId);
        for (Class<?> component : findCandidateComponents(pluginId)) {
            log.debug("Register a plugin component class [{}] to context", component);
            pluginApplicationContext.registerBean(component);
        }
    }

    public void onStartUp(String pluginId) {
        initApplicationContext(pluginId);
    }

    @NonNull
    public PluginApplicationContext getPluginApplicationContext(String pluginId) {
        return contextRegistry.getByPluginId(pluginId);
    }

    public void contextDestroyed(String pluginId) {
        Assert.notNull(pluginId, "pluginId must not be null");
        PluginApplicationContext removed = contextRegistry.remove(pluginId);
        if (removed != null) {
            removed.close();
        }
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

    private Set<Class<?>> findCandidateComponents(String pluginId) {
        Set<String> extensionClassNames = springPluginManager.getExtensionClassNames(pluginId);
        // add extensions for each started plugin
        PluginWrapper plugin = springPluginManager.getPlugin(pluginId);
        log.debug("Registering extensions of the plugin '{}' as beans", pluginId);
        Set<Class<?>> candidateComponents = new HashSet<>();
        for (String extensionClassName : extensionClassNames) {
            log.debug("Load extension class '{}'", extensionClassName);
            try {
                Class<?> extensionClass =
                    plugin.getPluginClassLoader().loadClass(extensionClassName);
                candidateComponents.add(extensionClass);
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }
        return candidateComponents;
    }

    /**
     * Register an extension as bean. Current implementation register extension as singleton using
     * {@code beanFactory.registerSingleton()}. The extension instance is created using {@code
     * pluginManager.getExtensionFactory().create(extensionClass)}. The bean name is the extension
     * class name. Override this method if you wish other register strategy.
     */
    protected void registerExtensionAsBean(Class<?> extensionClass) {
        Map<String, ?> extensionBeanMap =
            springPluginManager.getRootApplicationContext().getBeansOfType(extensionClass);
        if (extensionBeanMap.isEmpty()) {
            springPluginManager.getExtensionFactory().create(extensionClass);
        } else {
            log.debug("Bean registration aborted! Extension '{}' already existed as bean!",
                extensionClass.getName());
        }
    }
}
