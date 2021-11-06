package xyz.guqing.plugin.potatoes.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import run.halo.app.event.post.PostVisitEvent;
import run.halo.app.extensions.event.HaloPluginStateChangedEvent;

/**
 * @author guqing
 * @since 2021-11-04
 */
@Slf4j
@Component
public class HaloPostVisitListener {

    @EventListener(PostVisitEvent.class)
    public void onPluginStarted(PostVisitEvent event) {
        System.out.println("\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47");
        log.info("Plugin state [{}] is changed", event);
    }
}
