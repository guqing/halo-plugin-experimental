package run.halo.app.extensions.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import run.halo.app.extensions.SpringPluginManager;

/**
 * Listener registrar for plugins.
 *
 * @author guqing
 * @since 2021-11-05
 */
@Slf4j
public class PluginListenerRegistry {

    private final ConfigurableApplicationContext applicationContext;


    public PluginListenerRegistry(ApplicationContext context) {
        this.applicationContext = (ConfigurableApplicationContext) context;
    }

    /**
     * Add a new ApplicationListener that will be notified on context events such as context refresh
     * and context shutdown.
     * <p>Note that any ApplicationListener registered here will be applied
     * on refresh if the context is not active yet, or on the fly with the current event multicaster
     * in case of a context that is already active.
     *
     * @param listener the ApplicationListener to register
     * @see org.springframework.context.event.ContextRefreshedEvent
     * @see org.springframework.context.event.ContextClosedEvent
     */
    public void addApplicationListener(ApplicationListener<?> listener) {
        applicationContext.addApplicationListener(listener);
    }

    public void addApplicationListener(List<ApplicationListener<?>> listeners) {
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        for (ApplicationListener<?> listener : listeners) {
            applicationContext.addApplicationListener(listener);
        }
    }

    public void addApplicationListener(Class<?> targetType) {
        Assert.notNull(targetType, "The targetType must not be null.");
        addApplicationListener(targetType.getName(), targetType);
    }

    public void addApplicationListener(String beanName, Class<?> targetType) {
        // is the ApplicationListener type
        if (ApplicationListener.class.isAssignableFrom(targetType)) {
            ApplicationListener<?> listener =
                (ApplicationListener<?>) applicationContext.getBean(beanName);
            addApplicationListener(listener);
        } else {
            Map<Method, EventListener> annotatedMethods = Collections.emptyMap();
            try {
                annotatedMethods = MethodIntrospector.selectMethods(targetType,
                    (MethodIntrospector.MetadataLookup<EventListener>) method ->
                        AnnotatedElementUtils.findMergedAnnotation(method, EventListener.class));
            } catch (Throwable ex) {
                // An unresolvable type in a method signature, probably from a lazy bean - let's ignore it.
                if (log.isDebugEnabled()) {
                    log.debug("Could not resolve methods for bean with name '" + beanName + "'",
                        ex);
                }
            }
            List<EventListenerFactory> factories = getBeansOfType(EventListenerFactory.class);
            Assert.state(CollectionUtils.isEmpty(factories),
                "EventListenerFactory List not initialized");
            for (Method method : annotatedMethods.keySet()) {
                for (EventListenerFactory factory : factories) {
                    if (factory.supportsMethod(method)) {
                        Method methodToUse = AopUtils.selectInvocableMethod(method,
                            applicationContext.getType(beanName));
                        ApplicationListener<?> applicationListener =
                            factory.createApplicationListener(beanName, targetType, methodToUse);
                        addApplicationListener(applicationListener);
                        break;
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(annotatedMethods.size() + " @EventListener methods processed on bean '" +
                    beanName + "': " + annotatedMethods);
            }
        }
    }

    private <T> List<T> getBeansOfType(@NonNull Class<T> type) {
        Assert.notNull(type, "The 'type' must not be null.");
        try {
            Map<String, T> factoriesMap = applicationContext.getBeansOfType(type);
            return new ArrayList<>(factoriesMap.values());
        } catch (BeansException e) {
            log.debug("Could not find any beans of [{}]", type.getName());
        }
        return Collections.emptyList();
    }
}
