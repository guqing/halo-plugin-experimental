package run.halo.app.extensions.event;

import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationEvent;

/**
 * @author grant
 */
public class HaloPluginWebStartedEvent extends ApplicationEvent {

    private final PluginWrapper plugin;

    public HaloPluginWebStartedEvent(Object source, PluginWrapper plugin) {
        super(source);
        this.plugin = plugin;
    }

    public PluginWrapper getPlugin() {
        return plugin;
    }
}
