package run.halo.app.extensions;

import com.google.common.eventbus.EventBus;

/**
 * @author guqing
 * @since 2.0.0
 */
public class DefaultEventPublisher implements EventPublisher {
    private static final EventBus eventBus;

    static {
        eventBus = new EventBus();
    }

    @Override
    public void publishEvent(Object event) {
        eventBus.post(event);
    }

    @Override
    public void register(Object event) {
        eventBus.register(event);
    }

    @Override
    public void unregister(Object event) {
        eventBus.unregister(event);
    }
}
