package org.laxture.sbp.spring.boot;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.guqing.plugin.core.SpringBootPlugin;

/**
 * @author <a href="https://github.com/hank-cp">Hank CP</a>
 */
@Configuration
@AutoConfigureAfter(FlywayAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
@ConditionalOnBean(SpringBootPlugin.class)
public class FlywayClassLoaderConfiguration {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SpringBootPlugin plugin;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @ConditionalOnMissingBean
    public FlywayMigrationStrategy migrationStrategy() {
        return flyway -> {
            FluentConfiguration alterConf = Flyway.configure(plugin.getWrapper().getPluginClassLoader());
            alterConf.configuration(flyway.getConfiguration());
            new Flyway(alterConf).migrate();
        };
    }

}
