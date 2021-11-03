package run.halo.app.extensions.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * This event will be published to <b>plugin application context</b> once plugin is stopped.
 *
 * @author guqing
 * @date 2021-11-02
 */
public class HaloPluginStoppedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1048404352252169025L;

    public HaloPluginStoppedEvent(ApplicationContext pluginApplicationContext) {
        super(pluginApplicationContext);
    }
}
