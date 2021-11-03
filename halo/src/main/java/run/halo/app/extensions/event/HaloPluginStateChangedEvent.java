package run.halo.app.extensions.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * This event will be published to <b>main app application context</b> when any plugin is changed in
 * batch. Plugins' state might be manipulated in batch, like start up with main app/restart all,
 * etc. This event is useful if you need to do something after all plugin manipulation is done.
 * <br>
 * For example. When plugin jar file get updated, the previous register classloader will not be able
 * to access its resource file anymore. For batch plugin jar files updating, refreshing stuffs could
 * only be done after all plugins reloaded and new plugin classloaders provided.
 *
 * @author <a href="https://github.com/hank-cp">Hank CP</a>
 */
public class HaloPluginStateChangedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1653148906452766719L;

    public HaloPluginStateChangedEvent(ApplicationContext mainApplicationContext) {
        super(mainApplicationContext);
    }
}
