package run.halo.app.extensions;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Plugin;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import run.halo.app.extensions.event.HaloPluginRestartedEvent;
import run.halo.app.extensions.event.HaloPluginStartedEvent;
import run.halo.app.extensions.event.HaloPluginStoppedEvent;
import run.halo.app.extensions.internal.PluginRequestMappingManager;

@Slf4j
public abstract class SpringPlugin extends Plugin {

    private final ApplicationContext applicationContext;
    private final Set<String> injectedExtensionNames = new HashSet<>();

    public SpringPlugin(PluginWrapper wrapper) {
        super(wrapper);
        this.applicationContext = getPluginManager().getApplicationContext();
    }

    private PluginRequestMappingManager getRequestMappingManager() {
        return getApplicationContext().getBean(PluginRequestMappingManager.class);
    }

    /**
     * Release plugin holding release on stop.
     */
    public void releaseAdditionalResources() {
    }

    @Override
    public void start() {
        if (getWrapper().getPluginState() == PluginState.STARTED) {
            return;
        }

        long startTs = System.currentTimeMillis();
        log.debug("Starting plugin {} ......", getWrapper().getPluginId());

        getRequestMappingManager().registerControllers(this);

        applicationContext.publishEvent(new HaloPluginStartedEvent(applicationContext));
        // if main application context is not ready, don't send restart event
        applicationContext.publishEvent(new HaloPluginRestartedEvent(applicationContext));

        log.debug("Plugin {} is started in {}ms", getWrapper().getPluginId(),
            System.currentTimeMillis() - startTs);
    }

    @Override
    public void stop() {
        if (getWrapper().getPluginState() != PluginState.STARTED) {
            return;
        }

        log.debug("Stopping plugin {} ......", getWrapper().getPluginId());
        releaseAdditionalResources();
        // unregister Extension beans
        for (String extensionName : injectedExtensionNames) {
            log.debug("Unregister extension <{}> to main ApplicationContext", extensionName);
            ExtensionsInjector.unregisterBeanFromContext(applicationContext, extensionName);
        }

        getRequestMappingManager().unregisterControllers(this);
        applicationContext.publishEvent(new HaloPluginStoppedEvent(applicationContext));
        injectedExtensionNames.clear();
        ((ConfigurableApplicationContext) applicationContext).close();

        log.debug("Plugin {} is stopped", getWrapper().getPluginId());
    }

    public final ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public SpringPluginManager getPluginManager() {
        return (SpringPluginManager) getWrapper().getPluginManager();
    }
}
