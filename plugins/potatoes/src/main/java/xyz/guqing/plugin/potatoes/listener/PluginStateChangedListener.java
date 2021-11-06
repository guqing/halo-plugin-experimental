package xyz.guqing.plugin.potatoes.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import run.halo.app.extensions.event.HaloPluginStateChangedEvent;

/**
 * @author guqing
 * @since 2021-11-06
 */
@Slf4j
@Component
public class PluginStateChangedListener implements ApplicationListener<HaloPluginStateChangedEvent> {

    @Override
    public void onApplicationEvent(HaloPluginStateChangedEvent event) {
        log.info("Plugin [{}] state [{}] changed to [{}]", event.getPlugin().getPluginId(),
            event.getOldState(), event.getState());
    }
}
