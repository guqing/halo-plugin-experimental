package run.halo.app.extensions.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * This event will be published to <b>plugin application context</b> once plugin is restarted.
 * <p>
 * Note that this event will not be fired duaring <b>main app application context</b> starting
 * phase.
 *
 * @author guqing
 * @date 2021-11-02
 */
public class HaloPluginRestartedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1651490578605729784L;

    public HaloPluginRestartedEvent(ApplicationContext pluginApplicationContext) {
        super(pluginApplicationContext);
    }
}
