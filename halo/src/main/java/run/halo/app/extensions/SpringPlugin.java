package run.halo.app.extensions;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Plugin;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationContext;
import run.halo.app.extensions.event.HaloPluginRestartedEvent;
import run.halo.app.extensions.event.HaloPluginStartedEvent;
import run.halo.app.extensions.event.HaloPluginStoppedEvent;
import run.halo.app.extensions.internal.PluginRequestMappingManager;

@Slf4j
public abstract class SpringPlugin extends Plugin {

    private final ApplicationContext applicationContext;

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
    public void releaseAdditionalResources(String pluginId) {
        // release request mapping
        getRequestMappingManager()
            .unregisterControllers(getPluginManager(), pluginId);

        // release extension bean
        getPluginManager().releaseRegisteredResources(pluginId);
    }

    @Override
    public void start() {
        if (getWrapper().getPluginState() == PluginState.STARTED) {
            return;
        }

        long startTs = System.currentTimeMillis();
        log.debug("Starting plugin {} ......", getWrapper().getPluginId());

        getRequestMappingManager().registerControllers(this);

        applicationContext.publishEvent(new HaloPluginStartedEvent(this, getWrapper()));

        log.debug("Plugin {} is started in {}ms", getWrapper().getPluginId(),
            System.currentTimeMillis() - startTs);
    }

    @Override
    public void stop() {
        if (getWrapper().getPluginState() != PluginState.STARTED) {
            return;
        }
        String pluginId = getWrapper().getPluginId();
        log.debug("Stopping plugin {} ......", pluginId);

        releaseAdditionalResources(pluginId);

        // send stopped event
        applicationContext.publishEvent(new HaloPluginStoppedEvent(this, getWrapper()));
    }

    public final ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public SpringPluginManager getPluginManager() {
        return (SpringPluginManager) getWrapper().getPluginManager();
    }
}
