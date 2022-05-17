package xyz.guqing.plugin.potatoes.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author guqing
 * @since 2.0.0
 */
@Component
public class PotatoesVisitListener implements ApplicationListener<PotatoesVisitEvent> {
    @Override
    public void onApplicationEvent(PotatoesVisitEvent event) {
        System.out.println("Potatoes 被访问了...");
    }
}
