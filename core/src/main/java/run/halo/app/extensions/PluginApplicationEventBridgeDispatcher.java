package run.halo.app.extensions;

import java.lang.reflect.AnnotatedElement;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/**
 * 主应用将标记了 {@code SharedEvent}注解的事件桥接到已启用的插件，使其可以被插件监听到。
 *
 * @author guqing
 * @see SharedEvent
 * @see PluginApplicationContext
 * @since 2.0.0
 */
@Slf4j
@Component
@ConditionalOnClass(SpringPluginManager.class)
public class PluginApplicationEventBridgeDispatcher
    implements ApplicationListener<ApplicationEvent> {
    private final SpringPluginManager springPluginManager;

    public PluginApplicationEventBridgeDispatcher(SpringPluginManager springPluginManager) {
        this.springPluginManager = springPluginManager;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!isSharedEventAnnotationPresent(event.getClass())) {
            return;
        }

        for (PluginWrapper startedPlugin : springPluginManager.getStartedPlugins()) {
            PluginApplicationContext pluginApplicationContext =
                springPluginManager.getPluginApplicationContext(startedPlugin.getPluginId());
            log.debug("Bridging broadcast event [{}] to plugin [{}]", event,
                startedPlugin.getPluginId());
            pluginApplicationContext.publishEvent(event);
        }
    }

    private boolean isSharedEventAnnotationPresent(AnnotatedElement annotatedElement) {
        return AnnotationUtils.findAnnotation(annotatedElement, SharedEvent.class) != null;
    }
}
