package run.halo.app.extensions;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.event.EventListenerMethodProcessor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
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

        StopWatch stopWatch = new StopWatch("initialize-plugin-context");
        stopWatch.start("创建 PluginApplicationContext");
        PluginApplicationContext pluginApplicationContext = new PluginApplicationContext();
        pluginApplicationContext.setParent(getRootApplicationContext());
        pluginApplicationContext.setClassLoader(pluginClassLoader);
        stopWatch.stop();

        stopWatch.start("创建插件 DefaultResourceLoader");
        DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader(pluginClassLoader);
        pluginApplicationContext.setResourceLoader(defaultResourceLoader);
        stopWatch.stop();

        DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) pluginApplicationContext.getBeanFactory();

        stopWatch.start("registerAnnotationConfigProcessors");
        AnnotationConfigUtils.registerAnnotationConfigProcessors(beanFactory);
        stopWatch.stop();

        stopWatch.start("刷新插件 Application Context");
        pluginApplicationContext.refresh();
        stopWatch.stop();

        log.debug("Total millis: {} ms -> {}", stopWatch.getTotalTimeMillis(),
            stopWatch.prettyPrint());

        contextRegistry.register(pluginId, pluginApplicationContext);
        return pluginApplicationContext;
    }

    private void initApplicationContext(String pluginId) {
        if (contextRegistry.containsContext(pluginId)) {
            log.debug("Plugin application context for [{}] has bean initialized.", pluginId);
            return;
        }
        StopWatch stopWatch = new StopWatch();

        stopWatch.start("createPluginApplicationContext");
        PluginApplicationContext pluginApplicationContext =
            createPluginApplicationContext(pluginId);
        stopWatch.stop();

        stopWatch.start("findCandidateComponents");
        Set<Class<?>> candidateComponents = findCandidateComponents(pluginId);
        stopWatch.stop();

        stopWatch.start("registerBean");
        for (Class<?> component : candidateComponents) {
            log.debug("Register a plugin component class [{}] to context", component);
            pluginApplicationContext.registerBean(component);
        }
        stopWatch.stop();

        System.out.println(
            "initApplicationContext total millis: " + stopWatch.getTotalTimeMillis() + "ms -> "
                + stopWatch.prettyPrint());
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
        StopWatch stopWatch = new StopWatch("findCandidateComponents");

        stopWatch.start("getExtensionClassNames");
        Set<String> extensionClassNames = springPluginManager.getExtensionClassNames(pluginId);
        stopWatch.stop();

        // add extensions for each started plugin
        PluginWrapper plugin = springPluginManager.getPlugin(pluginId);
        log.debug("Registering extensions of the plugin '{}' as beans", pluginId);
        Set<Class<?>> candidateComponents = new HashSet<>();
        for (String extensionClassName : extensionClassNames) {
            log.debug("Load extension class '{}'", extensionClassName);
            try {
                stopWatch.start("loadClass");
                Class<?> extensionClass =
                    plugin.getPluginClassLoader().loadClass(extensionClassName);
                stopWatch.stop();

                candidateComponents.add(extensionClass);
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }
        System.out.println(
            "total millis: " + stopWatch.getTotalTimeMillis() + "ms ->" + stopWatch.prettyPrint());
        return candidateComponents;
    }
}
