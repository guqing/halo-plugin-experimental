package run.halo.app.extensions.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author guqing
 * @since 2021-11-15
 */
public class ExtensionClassLoadedEvent extends ApplicationEvent {

    public ExtensionClassLoadedEvent(Object source) {
        super(source);
    }
}
