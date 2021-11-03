package run.halo.app.extensions;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * @author guqing
 * @since 2021-11-01
 */
public class ExtensionsInjector {
    private static final Logger log = LoggerFactory.getLogger(ExtensionsInjector.class);

    protected final SpringPluginManager springPluginManager;

    public ExtensionsInjector(SpringPluginManager springPluginManager) {
        this.springPluginManager = springPluginManager;
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
                    Class<?> extensionClass = plugin.getPluginClassLoader().loadClass(extensionClassName);
                    registerExtension(extensionClass);
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public void injectExtensionByPluginId(String pluginId) {
        Set<String> extensionClassNames = springPluginManager.getExtensionClassNames(pluginId);
        // add extensions for each started plugin
        PluginWrapper plugin = springPluginManager.getPlugin(pluginId);
        System.out.println(extensionClassNames);
        log.debug("Registering extensions of the plugin '{}' as beans", pluginId);
        List<Class<?>> extensionClasses = new LinkedList<>();
        for (String extensionClassName : extensionClassNames) {
            try {
                log.debug("Load extension class '{}'", extensionClassName);
                Class<?> extensionClass = plugin.getPluginClassLoader().loadClass(extensionClassName);
                extensionClasses.add(extensionClass);
            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        }

        for (Class<?> extensionClass : extensionClasses) {
            log.debug("Register a extension class '{}' as a bean", extensionClass.getName());
            registerExtension(extensionClass);
        }
    }

    /**
     * Register an extension as bean.
     * Current implementation register extension as singleton using {@code beanFactory.registerSingleton()}.
     * The extension instance is created using {@code pluginManager.getExtensionFactory().create(extensionClass)}.
     * The bean name is the extension class name.
     * Override this method if you wish other register strategy.
     */
    protected void registerExtension(Class<?> extensionClass) {
        Map<String, ?> extensionBeanMap = springPluginManager.getApplicationContext().getBeansOfType(extensionClass);
        if (extensionBeanMap.isEmpty()) {
            springPluginManager.getExtensionFactory().create(extensionClass);
        } else {
            log.debug("Bean registration aborted! Extension '{}' already existed as bean!", extensionClass.getName());
        }
    }

    public static void unregisterBeanFromContext(ApplicationContext ctx,
        String beanName) {
        Assert.notNull(beanName, "bean must not be null");
        ctx.getAutowireCapableBeanFactory().destroyBean(beanName);
    }

    public static void unregisterBeanFromContext(ApplicationContext ctx,
        Object existingBean) {
        Assert.notNull(existingBean, "existingBean must not be null");
        ctx.getAutowireCapableBeanFactory().destroyBean(existingBean);
    }
}
