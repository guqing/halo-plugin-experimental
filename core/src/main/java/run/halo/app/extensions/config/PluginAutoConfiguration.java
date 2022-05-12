package run.halo.app.extensions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.ClassLoadingStrategy;
import org.pf4j.CompoundPluginLoader;
import org.pf4j.DevelopmentPluginLoader;
import org.pf4j.JarPluginLoader;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginLoader;
import org.pf4j.PluginManager;
import org.pf4j.PluginStateListener;
import org.pf4j.PluginStatusProvider;
import org.pf4j.RuntimeMode;
import org.pf4j.update.DefaultUpdateRepository;
import org.pf4j.update.UpdateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import run.halo.app.extensions.SpringPluginManager;
import run.halo.app.extensions.internal.PluginRequestMappingManager;

/**
 * Plugin auto-configuration for Spring Boot
 *
 * @author guqing
 * @see PluginProperties
 */
@Slf4j
@Configuration
@ConditionalOnClass({PluginManager.class, SpringPluginManager.class})
@ConditionalOnProperty(prefix = PluginProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({PluginProperties.class})
public class PluginAutoConfiguration {

    @Autowired
    private PluginProperties pluginProperties;

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
    public SpringPluginManager pluginManager() {
        // Setup RuntimeMode
        System.setProperty("pf4j.mode", pluginProperties.getRuntimeMode().toString());

        // Setup Plugin folder
        String pluginsRoot =
            StringUtils.hasText(pluginProperties.getPluginsRoot())
                ? pluginProperties.getPluginsRoot()
                : "plugins";
        System.setProperty("pf4j.pluginsDir", pluginsRoot);
        String appHome = System.getProperty("app.home");
        if (RuntimeMode.DEPLOYMENT == pluginProperties.getRuntimeMode()
            && StringUtils.hasText(appHome)) {
            System.setProperty("pf4j.pluginsDir", appHome + File.separator + pluginsRoot);
        }

        SpringPluginManager pluginManager =
            new SpringPluginManager(new File(pluginsRoot).toPath()) {
                @Override
                protected PluginLoader createPluginLoader() {
                    if (pluginProperties.getCustomPluginLoader() != null) {
                        Class<PluginLoader> clazz = pluginProperties.getCustomPluginLoader();
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
                            .add(new DevelopmentPluginLoader(this) {
                                @Override
                                public ClassLoader loadPlugin(Path pluginPath,
                                    PluginDescriptor pluginDescriptor) {
                                    PluginClassLoader pluginClassLoader =
                                        new PluginClassLoader(pluginManager, pluginDescriptor,
                                            getClass().getClassLoader());

                                    loadClasses(pluginPath, pluginClassLoader);
                                    loadJars(pluginPath, pluginClassLoader);

                                    return pluginClassLoader;
                                }
                            }, this::isDevelopment)
                            .add(new JarPluginLoader(this) {
                                @Override
                                public ClassLoader loadPlugin(Path pluginPath,
                                    PluginDescriptor pluginDescriptor) {
                                    PluginClassLoader pluginClassLoader =
                                        new PluginClassLoader(pluginManager, pluginDescriptor,
                                            getClass().getClassLoader(), ClassLoadingStrategy.APD);
                                    pluginClassLoader.addFile(pluginPath.toFile());
                                    return pluginClassLoader;

                                }
                            }, this::isNotDevelopment);
                    }
                }

                @Override
                protected PluginStatusProvider createPluginStatusProvider() {
                    if (PropertyPluginStatusProvider.isPropertySet(pluginProperties)) {
                        return new PropertyPluginStatusProvider(pluginProperties);
                    }
                    return super.createPluginStatusProvider();
                }
            };

        pluginManager.setAutoStartPlugin(pluginProperties.isAutoStartPlugin());
        pluginManager.setExactVersionAllowed(pluginProperties.isExactVersionAllowed());
        pluginManager.setSystemVersion(pluginProperties.getSystemVersion());

        return pluginManager;
    }

    @Bean
    UpdateManager updateManager() throws IOException {
        Gson gson = new GsonBuilder().create();
        InputStream stream = new ClassPathResource("repositories.json").getInputStream();
        DefaultUpdateRepository[] repositories = gson.fromJson(
            new InputStreamReader(stream, StandardCharsets.UTF_8), DefaultUpdateRepository[].class);
        return new UpdateManager(pluginManager(), Arrays.asList(repositories));
    }
}
