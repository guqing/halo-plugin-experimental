package run.halo.app.extensions.config;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.CompoundPluginLoader;
import org.pf4j.DefaultPluginLoader;
import org.pf4j.JarPluginLoader;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginLoader;
import org.pf4j.PluginManager;
import org.pf4j.PluginStateListener;
import org.pf4j.PluginStatusProvider;
import org.pf4j.RuntimeMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import run.halo.app.extensions.SpringPluginManager;
import run.halo.app.extensions.internal.PluginRequestMappingManager;
import run.halo.app.extensions.internal.SpringPluginClassLoader;

/**
 * Plugin auto-configuration for Spring Boot
 *
 * @author guqing
 * @see PluginProperties
 */
@Configuration
@ConditionalOnClass({PluginManager.class, SpringPluginManager.class})
@ConditionalOnProperty(prefix = PluginProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({PluginProperties.class, PluginProperties.class})
@Slf4j
public class PluginAutoConfiguration {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Bean
    public PluginRequestMappingManager pluginRequestMappingManager() {
        return new PluginRequestMappingManager(requestMappingHandlerMapping);
    }

    @Bean
    @ConditionalOnMissingBean(PluginStateListener.class)
    public PluginStateListener pluginStateListener() {
        return event -> {
            PluginDescriptor descriptor = event.getPlugin().getDescriptor();
            if (log.isDebugEnabled()) {
                log.debug("Plugin [{}（{}）]({}) {}", descriptor.getPluginId(),
                    descriptor.getVersion(), descriptor.getPluginDescription(),
                    event.getPluginState().toString());
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(PluginManagerController.class)
    @ConditionalOnProperty(name = "halo.plugin.controller.base-path")
    public PluginManagerController pluginManagerController() {
        return new PluginManagerController();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringPluginManager pluginManager(PluginProperties properties) {
        // Setup RuntimeMode
        System.setProperty("pf4j.mode", properties.getRuntimeMode().toString());

        // Setup Plugin folder
        String pluginsRoot =
            StringUtils.hasText(properties.getPluginsRoot()) ? properties.getPluginsRoot()
                : "plugins";
        System.setProperty("pf4j.pluginsDir", pluginsRoot);
        String appHome = System.getProperty("app.home");
        if (RuntimeMode.DEPLOYMENT == properties.getRuntimeMode()
            && StringUtils.hasText(appHome)) {
            System.setProperty("pf4j.pluginsDir", appHome + File.separator + pluginsRoot);
        }

        SpringPluginManager pluginManager = new SpringPluginManager(
            new File(pluginsRoot).toPath()) {
            @Override
            protected PluginLoader createPluginLoader() {
                if (properties.getCustomPluginLoader() != null) {
                    Class<PluginLoader> clazz = properties.getCustomPluginLoader();
                    try {
                        Constructor<?> constructor = clazz.getConstructor(PluginManager.class);
                        return (PluginLoader) constructor.newInstance(this);
                    } catch (Exception ex) {
                        throw new IllegalArgumentException(
                            String.format("Create custom PluginLoader %s failed. Make sure" +
                                    "there is a constructor with one argument that accepts PluginLoader",
                                clazz.getName()));
                    }
                } else {
                    return new CompoundPluginLoader()
                        .add(new DefaultPluginLoader(this) {
                            @Override
                            protected PluginClassLoader createPluginClassLoader(Path pluginPath,
                                PluginDescriptor pluginDescriptor) {
                                if (properties.getClassesDirectories() != null
                                    && properties.getClassesDirectories().size() > 0) {
                                    for (String classesDirectory : properties.getClassesDirectories()) {
                                        pluginClasspath.addClassesDirectories(classesDirectory);
                                    }
                                }
                                if (properties.getLibDirectories() != null
                                    && properties.getLibDirectories().size() > 0) {
                                    for (String libDirectory : properties.getLibDirectories()) {
                                        pluginClasspath.addJarsDirectories(libDirectory);
                                    }
                                }
                                return new SpringPluginClassLoader(pluginManager,
                                    pluginDescriptor, getClass().getClassLoader());
                            }
                        }, this::isDevelopment)
                        .add(new JarPluginLoader(this) {
                            @Override
                            public ClassLoader loadPlugin(Path pluginPath,
                                PluginDescriptor pluginDescriptor) {
                                PluginClassLoader pluginClassLoader =
                                    new PluginClassLoader(pluginManager, pluginDescriptor,
                                        getClass().getClassLoader());
                                pluginClassLoader.addFile(pluginPath.toFile());
                                return pluginClassLoader;

                            }
                        }, this::isNotDevelopment);
                }
            }

            @Override
            protected PluginStatusProvider createPluginStatusProvider() {
                if (PropertyPluginStatusProvider.isPropertySet(properties)) {
                    return new PropertyPluginStatusProvider(properties);
                }
                return super.createPluginStatusProvider();
            }
        };

        pluginManager.setAutoStartPlugin(properties.isAutoStartPlugin());
        pluginManager.setExactVersionAllowed(properties.isExactVersionAllowed());
        pluginManager.setSystemVersion(properties.getSystemVersion());

        return pluginManager;
    }
}
