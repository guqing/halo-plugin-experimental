package run.halo.app.extensions.config;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import run.halo.app.extensions.SpringPluginManager;
import run.halo.app.extensions.event.HaloPluginStateChangedEvent;

/**
 * Halo plugin started listener for Spring Boot.
 *
 * @author guqing
 * @see PluginProperties
 */
@Slf4j
@Configuration
@ConditionalOnClass({PluginManager.class, SpringPluginManager.class})
@ConditionalOnProperty(prefix = PluginProperties.PREFIX, value = "enabled", havingValue = "true")
public class PluginStartedListener {

    @EventListener(HaloPluginStateChangedEvent.class)
    public void onPluginStarted() {
        log.info("The plugin starts successfully.");
    }
}