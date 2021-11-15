package run.halo.app.extensions.internal;

import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationEvent;

/**
 * @author guqing
 * @since 2021-11-15
 */
public class ExtensionInjectedEvent extends ApplicationEvent {

    private final PluginWrapper pluginWrapper;

    public ExtensionInjectedEvent(Object source, PluginWrapper pluginWrapper) {
        super(source);
        this.pluginWrapper = pluginWrapper;
    }

    public PluginWrapper getPluginWrapper() {
        return pluginWrapper;
    }
}
