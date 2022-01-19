package run.halo.app.extensions.event;

import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationEvent;

/**
 * 19/1/2022 2:13 下午
 * 描述：
 *
 * @author grant
 */
public class HaloPluginWebStoppedEvent extends ApplicationEvent {

    private final PluginWrapper plugin;

    public HaloPluginWebStoppedEvent(Object source, PluginWrapper plugin) {
        super(source);
        this.plugin = plugin;
    }

    public PluginWrapper getPlugin() {
        return plugin;
    }

    public PluginState getPluginState() {
        return plugin.getPluginState();
    }
}
