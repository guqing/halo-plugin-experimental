package xyz.guqing.plugin.potatoes.listener;

import org.springframework.context.ApplicationEvent;

/**
 * @author guqing
 * @since 2.0.0
 */
public class PotatoesVisitEvent extends ApplicationEvent {
    public PotatoesVisitEvent(Object source) {
        super(source);
    }
}
