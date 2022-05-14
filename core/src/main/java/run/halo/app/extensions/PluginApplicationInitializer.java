package run.halo.app.extensions;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
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
        PluginWrapper plugin = springPluginManager.getPlugin(pluginId);
        ClassLoader pluginClassLoader = plugin.getPluginClassLoader();

        PluginApplicationContext pluginApplicationContext = new PluginApplicationContext();
        pluginApplicationContext.setParent(getRootApplicationContext());
        pluginApplicationContext.setClassLoader(pluginClassLoader);

        DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader(pluginClassLoader);
        pluginApplicationContext.setResourceLoader(defaultResourceLoader);

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
}
