package run.halo.app.extensions;

import java.lang.reflect.Field;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

/**
 * The generic IOC container for plugins.
 * The plugin-classes loaded through the same plugin-classloader will be put into the same {@link PluginApplicationContext} for bean creation.
 *
 * @author guqing
 * @since 2.0.0
 */
public class PluginApplicationContext extends GenericApplicationContext {

    /**
     * Synchronization monitor for the "refresh" and "destroy".
     */
    private final Object startupShutdownMonitor = new Object();
    private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

    @Override
    public void refresh() throws BeansException, IllegalStateException {
        StopWatch stopWatch = new StopWatch();
        synchronized (this.startupShutdownMonitor) {
            stopWatch.start("refresh applicationStartup.start");
            StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");
            stopWatch.stop();

            stopWatch.start("prepareRefresh");
            // Prepare this context for refreshing.
            prepareRefresh();
            stopWatch.stop();

            stopWatch.start("obtainFreshBeanFactory");
            // Tell the subclass to refresh the internal bean factory.
            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
            stopWatch.stop();

            stopWatch.start("prepareBeanFactory");
            // Prepare the bean factory for use in this context.
            prepareBeanFactory(beanFactory);
            stopWatch.stop();

            try {
                stopWatch.start("postProcessBeanFactory");
                // Allows post-processing of the bean factory in context subclasses.
                postProcessBeanFactory(beanFactory);
                stopWatch.stop();

                stopWatch.start("post-process this.applicationStartup.start");
                StartupStep beanPostProcess =
                    this.applicationStartup.start("spring.context.beans.post-process");
                stopWatch.stop();

                stopWatch.start("invokeBeanFactoryPostProcessors");
                // Invoke factory processors registered as beans in the context.
                invokeBeanFactoryPostProcessors(beanFactory);
                stopWatch.stop();

                stopWatch.start("registerBeanPostProcessors");
                // Register bean processors that intercept bean creation.
                registerBeanPostProcessors(beanFactory);
                beanPostProcess.end();
                stopWatch.stop();

                stopWatch.start("initMessageSource");
                // Initialize message source for this context.
                initMessageSource();
                stopWatch.stop();

                stopWatch.start("initApplicationEventMulticaster");
                // Initialize event multicaster for this context.
                initApplicationEventMulticaster();
                stopWatch.stop();

                stopWatch.start("onRefresh");
                // Initialize other special beans in specific context subclasses.
                onRefresh();
                stopWatch.stop();

                stopWatch.start("registerListeners");
                // Check for listener beans and register them.
                registerListeners();
                stopWatch.stop();

                stopWatch.start("finishBeanFactoryInitialization");
                // Instantiate all remaining (non-lazy-init) singletons.
                finishBeanFactoryInitialization(beanFactory);
                stopWatch.stop();

                stopWatch.start("finishRefresh");
                // Last step: publish corresponding event.
                finishRefresh();
                stopWatch.stop();
                System.out.println("total millis: " + stopWatch.getTotalTimeMillis() + "ms -> "
                    + stopWatch.prettyPrint());
            } catch (BeansException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Exception encountered during context initialization - " +
                        "cancelling refresh attempt: " + ex);
                }

                // Destroy already created singletons to avoid dangling resources.
                destroyBeans();

                // Reset 'active' flag.
                cancelRefresh(ex);

                // Propagate exception to caller.
                throw ex;
            } finally {
                // Reset common introspection caches in Spring's core, since we
                // might not ever need metadata for singleton beans anymore...
                resetCommonCaches();
                contextRefresh.end();
            }
        }
    }

    protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
        Assert.notNull(event, "Event must not be null");

        // Decorate event as an ApplicationEvent if necessary
        ApplicationEvent applicationEvent;
        if (event instanceof ApplicationEvent) {
            applicationEvent = (ApplicationEvent) event;
        } else {
            applicationEvent = new PayloadApplicationEvent<>(this, event);
            if (eventType == null) {
                eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
            }
        }

        // Multicast right now if possible - or lazily once the multicaster is initialized
        Set<ApplicationEvent> earlyApplicationEvents = getEarlyApplicationEvents();
        if (earlyApplicationEvents != null) {
            earlyApplicationEvents.add(applicationEvent);
        } else {
            getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
        }
    }

    private ApplicationEventMulticaster getApplicationEventMulticaster() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        return beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
            ApplicationEventMulticaster.class);
    }

    public Set<ApplicationEvent> getEarlyApplicationEvents() {
        try {
            Field earlyApplicationEventsField =
                AbstractApplicationContext.class.getDeclaredField("earlyApplicationEvents");
            return (Set<ApplicationEvent>) earlyApplicationEventsField.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // ignore this exception
            return null;
        }
    }
}
