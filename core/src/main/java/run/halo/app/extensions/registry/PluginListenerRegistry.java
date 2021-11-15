package run.halo.app.extensions.registry;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.DefaultEventListenerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import run.halo.app.extensions.event.PluginApplicationListenerMethodAdapter;
import run.halo.app.extensions.event.PluginEventExpressionEvaluator;

/**
 * Listener registrar for plugins.
 *
 * @author guqing
 * @since 2021-11-05
 */
@Slf4j
@Component
public class PluginListenerRegistry implements ApplicationContextAware {

    private final Map<String, List<ApplicationListener<?>>> listenerRegistrations =
        new ConcurrentHashMap<>();
    private final PluginEventExpressionEvaluator evaluator = new PluginEventExpressionEvaluator();
    private AbstractApplicationContext applicationContext;

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
    private synchronized void addPluginListener(String pluginId,
        ApplicationListener<?> listener) {
        applicationContext.addApplicationListener(listener);
        List<ApplicationListener<?>> applicationListeners =
            listenerRegistrations.computeIfAbsent(pluginId, computeIfAbsent -> new LinkedList<>());
        applicationListeners.add(listener);
    }

    public void addPluginListener(String pluginId, Class<?> targetType) {
        Assert.notNull(targetType, "The targetType must not be null.");
        addPluginListener(pluginId, targetType.getName(), targetType);
    }

    public List<ApplicationListener<?>> getPluginListeners(String pluginId) {
        return this.listenerRegistrations.get(pluginId);
    }

    public Map<String, List<ApplicationListener<?>>> getListenerRegistrations() {
        return this.listenerRegistrations;
    }

    public void removePluginListener(String pluginId) {
        List<ApplicationListener<?>> pluginListeners = getPluginListeners(pluginId);
        if (CollectionUtils.isEmpty(pluginListeners)) {
            return;
        }
        for (ApplicationListener<?> pluginListener : pluginListeners) {
            if (applicationContext.getApplicationListeners().remove(pluginListener)) {
                log.debug("Removed listener [{}] for plugin [{}].", pluginListener, pluginId);
            }
        }
    }

    public void addPluginListener(String pluginId, String beanName, Class<?> targetType) {
        // is the ApplicationListener type
        if (ApplicationListener.class.isAssignableFrom(targetType)) {
            ApplicationListener<?> listener =
                (ApplicationListener<?>) applicationContext.getBean(beanName);
            addPluginListener(pluginId, listener);
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
            List<EventListenerFactory> factories = getPluginListenerFactoryBeans();
            Assert.state(!CollectionUtils.isEmpty(factories),
                "EventListenerFactory List not initialized");
            for (Method method : annotatedMethods.keySet()) {
                for (EventListenerFactory factory : factories) {
                    if (factory.supportsMethod(method)) {
                        Method methodToUse = AopUtils.selectInvocableMethod(method,
                            applicationContext.getType(beanName));
                        ApplicationListener<?> applicationListener =
                            factory.createApplicationListener(beanName, targetType, methodToUse);
                        if (applicationListener instanceof PluginApplicationListenerMethodAdapter) {
                            ((PluginApplicationListenerMethodAdapter) applicationListener)
                                .init(applicationContext, this.evaluator);
                        }
                        addPluginListener(pluginId, applicationListener);
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

    private List<EventListenerFactory> getPluginListenerFactoryBeans() {
        try {
            Map<String, EventListenerFactory> factoriesMap =
                applicationContext.getBeansOfType(EventListenerFactory.class);
            return factoriesMap.values()
                .stream()
                .filter(t -> !(t instanceof DefaultEventListenerFactory))
                .collect(Collectors.toList());
        } catch (BeansException e) {
            log.debug("Could not find any beans of [EventListenerFactory]");
        }
        return Collections.emptyList();
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = (AbstractApplicationContext) applicationContext;
    }
}
