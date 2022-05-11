package run.halo.app.extensions;

/**
 * @author guqing
 * @since 2.0.0
 */
public interface EventPublisher {
    /**
     * Notify all <strong>matching</strong> listeners registered with this
     * application of an event.
     * <p>Such an event publication step is effectively a hand-off to the
     * multicaster and does not imply synchronous/asynchronous execution
     * or even immediate execution at all. Event listeners are encouraged
     * to be as efficient as possible, individually using asynchronous
     * execution for longer-running and potentially blocking operations.
     *
     * @param event the event to publish
     */
    void publishEvent(Object event);

    /**
     * Registers all subscriber methods on object to receive events.
     *
     * @param object object whose subscriber methods should be registered.
     */
    void register(Object object);

    /**
     * Unregisters all subscriber methods on a registered {@code object}.
     *
     * @param object object whose subscriber methods should be unregistered.
     * @throws IllegalArgumentException if the object was not previously registered.
     */
    void unregister(Object object);
}
