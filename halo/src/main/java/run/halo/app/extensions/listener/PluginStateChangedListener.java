package run.halo.app.extensions.listener;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import run.halo.app.extensions.SpringPluginManager;
import run.halo.app.extensions.config.PluginProperties;
import run.halo.app.extensions.event.HaloPluginStartedEvent;
import run.halo.app.extensions.event.HaloPluginStoppedEvent;
import run.halo.app.extensions.extpoint.ExtensionPointFinder;
import run.halo.app.handler.file.FileHandler;

/**
 * Halo plugin state changed listener for Spring Boot.
 *
 * @author guqing
 * @see PluginProperties
 */
@Slf4j
@Configuration
@ConditionalOnClass({PluginManager.class, SpringPluginManager.class})
@ConditionalOnProperty(prefix = PluginProperties.PREFIX, value = "enabled", havingValue = "true")
public class PluginStateChangedListener {

    @Autowired
    private ExtensionPointFinder extensionPointFinder;

    @Autowired
    private SpringPluginManager springPluginManager;

    @EventListener(HaloPluginStartedEvent.class)
    public void onPluginStarted(HaloPluginStartedEvent event) {
        System.out.println(event.getPlugin().getPluginManager().getExtensions(FileHandler.class));
        springPluginManager.registerListenerBy(event.getPlugin().getPluginId());
        this.extensionPointFinder.refreshExtensions();
        log.info("The plugin starts successfully.");
    }

    @EventListener(HaloPluginStoppedEvent.class)
    public void onPluginStopped(HaloPluginStoppedEvent event) {
        springPluginManager.unregisterListenerBy(event.getPlugin().getPluginId());
        this.extensionPointFinder.refreshExtensions();
        log.info("Plugin {} is stopped", event.getPlugin().getPluginId());
    }
}