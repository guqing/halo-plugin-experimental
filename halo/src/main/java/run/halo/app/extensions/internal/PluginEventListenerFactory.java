package run.halo.app.extensions.internal;

import java.lang.reflect.Method;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

/**
 * Creating ApplicationListener for methods annotated with EventListener.
 *
 * @author guqing
 * @since 2021-11-05
 */
public class PluginEventListenerFactory implements EventListenerFactory, Ordered {

    private int order = LOWEST_PRECEDENCE;

    @Override
    public boolean supportsMethod(@NonNull Method method) {
        return false;
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
        return new ApplicationListenerMethodAdapter(beanName, type, method);
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
