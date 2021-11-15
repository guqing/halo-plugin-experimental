package run.halo.app.extensions.event;

import java.lang.reflect.Method;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @author guqing
 * @since 2021-11-06
 */
@Component
public class PluginEventListenerFactory implements EventListenerFactory, Ordered {

    private int order = LOWEST_PRECEDENCE;

    @Override
    public boolean supportsMethod(@NonNull Method method) {
        return true;
    }

    /**
     * Create an {@link ApplicationListener} for the specified method.
     *
     * @param beanName the name of the bean
     * @param type     the target type of the instance
     * @param method   the {@link EventListener} annotated method
     * @return an application listener, suitable to invoke the specified method
     */
    @NonNull
    public ApplicationListener<?> createApplicationListener(@NonNull String beanName,
        @NonNull Class<?> type, @NonNull Method method) {
        return new PluginApplicationListenerMethodAdapter(beanName, type, method);
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
