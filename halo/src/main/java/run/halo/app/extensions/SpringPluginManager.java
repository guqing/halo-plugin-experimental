package run.halo.app.extensions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ExtensionFactory;
import org.pf4j.ExtensionFinder;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginRepository;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.PluginStateEvent;
import org.pf4j.PluginWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import run.halo.app.extensions.config.model.PluginStartingError;
import run.halo.app.extensions.event.HaloPluginStateChangedEvent;

/**
 * PluginManager to hold the main ApplicationContext
 *
 * @author Hank CP
 * @author guqing
 */
@Slf4j
public class SpringPluginManager extends DefaultPluginManager
    implements ApplicationContextAware, InitializingBean {

    private final Map<String, PluginStartingError> startingErrors = new HashMap<>();
    private ApplicationContext applicationContext;
    private boolean autoStartPlugin = true;
    private PluginRepository pluginRepository;
    private ExtensionsInjector extensionsInjector;

    public SpringPluginManager() {
        super();
    }

    public SpringPluginManager(Path pluginsRoot) {
        super(pluginsRoot);
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new SingletonSpringExtensionFactory(this);
    }

    @Override
    public PluginDescriptorFinder getPluginDescriptorFinder() {
        return super.getPluginDescriptorFinder();
    }

    @Override
    protected ExtensionFinder createExtensionFinder() {
        return new ScanningExtensionFinder(this);
    }

    @Override
    protected PluginRepository createPluginRepository() {
        this.pluginRepository = super.createPluginRepository();
        return this.pluginRepository;
    }

    public ExtensionsInjector getExtensionsInjector() {
        return extensionsInjector;
    }

    public PluginRepository getPluginRepository() {
        return pluginRepository;
    }

    public boolean isAutoStartPlugin() {
        return autoStartPlugin;
    }

    public void setAutoStartPlugin(boolean autoStartPlugin) {
        this.autoStartPlugin = autoStartPlugin;
    }

    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // This method load, start plugins and inject extensions in Spring
        loadPlugins();
        this.extensionsInjector = new ExtensionsInjector(this);
        this.extensionsInjector.injectExtensions();
    }

    public PluginStartingError getPluginStartingError(String pluginId) {
        return startingErrors.get(pluginId);
    }


    public Set<Class<?>> getControllers(String pluginId) {
        return this.extensionsInjector.getControllers(pluginId);
    }

    // region Plugin State Manipulation
    private void doStartPlugins() {
        startingErrors.clear();
        long ts = System.currentTimeMillis();

        for (PluginWrapper pluginWrapper : resolvedPlugins) {
            PluginState pluginState = pluginWrapper.getPluginState();
            if ((PluginState.DISABLED != pluginState) && (PluginState.STARTED != pluginState)) {
                try {
                    pluginWrapper.getPlugin().start();
                    pluginWrapper.setPluginState(PluginState.STARTED);
                    startedPlugins.add(pluginWrapper);

                    firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    startingErrors.put(pluginWrapper.getPluginId(), PluginStartingError.of(
                        pluginWrapper.getPluginId(), e.getMessage(), e.toString()));
                    releaseRegisteredResources(pluginWrapper.getPluginId());
                }
            }
        }

        log.info("[Halo] {} plugins are started in {}ms. {} failed",
            getPlugins(PluginState.STARTED).size(),
            System.currentTimeMillis() - ts, startingErrors.size());
    }

    public void releaseRegisteredResources(String pluginId) {
        try {
            extensionsInjector.unregisterExtensions(pluginId);
        } catch (Exception e) {
            log.trace("Release registered resources failed. " + e.getMessage(), e);
        }
    }

    private void doStopPlugins() {
        startingErrors.clear();
        // stop started plugins in reverse order
        Collections.reverse(startedPlugins);
        Iterator<PluginWrapper> itr = startedPlugins.iterator();
        while (itr.hasNext()) {
            PluginWrapper pluginWrapper = itr.next();
            PluginState pluginState = pluginWrapper.getPluginState();
            if (PluginState.STARTED == pluginState) {
                try {
                    log.info("Stop plugin '{}'", getPluginLabel(pluginWrapper.getDescriptor()));
                    pluginWrapper.getPlugin().stop();
                    pluginWrapper.setPluginState(PluginState.STOPPED);
                    itr.remove();

                    firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
                } catch (PluginRuntimeException e) {
                    log.error(e.getMessage(), e);
                    startingErrors.put(pluginWrapper.getPluginId(), PluginStartingError.of(
                        pluginWrapper.getPluginId(), e.getMessage(), e.toString()));
                }
            }
        }
    }

    private PluginState doStartPlugin(String pluginId, boolean sendEvent) {
        PluginWrapper plugin = getPlugin(pluginId);
        PluginState previousState = plugin.getPluginState();
        try {
            // inject bean
            extensionsInjector.injectExtensionByPluginId(pluginId);

            PluginState pluginState = super.startPlugin(pluginId);
            if (sendEvent && previousState != pluginState) {
                applicationContext.publishEvent(
                    new HaloPluginStateChangedEvent(applicationContext));
            }
            return pluginState;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            startingErrors.put(plugin.getPluginId(), PluginStartingError.of(
                plugin.getPluginId(), e.getMessage(), e.toString()));
            releaseRegisteredResources(pluginId);
        }
        return plugin.getPluginState();
    }

    private PluginState doStopPlugin(String pluginId, boolean sendEvent) {
        PluginWrapper plugin = getPlugin(pluginId);
        PluginState previousState = plugin.getPluginState();
        try {
            PluginState pluginState = super.stopPlugin(pluginId);
            if (sendEvent && previousState != pluginState) {
                applicationContext.publishEvent(
                    new HaloPluginStateChangedEvent(applicationContext));
            }
            return pluginState;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            startingErrors.put(plugin.getPluginId(), PluginStartingError.of(
                plugin.getPluginId(), e.getMessage(), e.toString()));
        }
        return plugin.getPluginState();
    }

    @Override
    public void startPlugins() {
        doStartPlugins();
        applicationContext.publishEvent(new HaloPluginStateChangedEvent(applicationContext));
    }

    @Override
    public PluginState startPlugin(String pluginId) {
        return doStartPlugin(pluginId, true);
    }

    @Override
    public void stopPlugins() {
        doStopPlugins();
        applicationContext.publishEvent(new HaloPluginStateChangedEvent(applicationContext));
    }

    @Override
    public PluginState stopPlugin(String pluginId) {
        return doStopPlugin(pluginId, true);
    }

    public void restartPlugins() {
        doStopPlugins();
        startPlugins();
    }

    public PluginState restartPlugin(String pluginId) {
        PluginState pluginState = doStopPlugin(pluginId, false);
        if (pluginState != PluginState.STARTED) {
            doStartPlugin(pluginId, false);
        }
        doStartPlugin(pluginId, false);
        applicationContext.publishEvent(new HaloPluginStateChangedEvent(applicationContext));
        return pluginState;
    }

    public void reloadPlugins(boolean restartStartedOnly) {
        doStopPlugins();
        List<String> startedPluginIds = new ArrayList<>();
        getPlugins().forEach(plugin -> {
            if (plugin.getPluginState() == PluginState.STARTED) {
                startedPluginIds.add(plugin.getPluginId());
            }
            unloadPlugin(plugin.getPluginId());
        });
        loadPlugins();
        if (restartStartedOnly) {
            startedPluginIds.forEach(pluginId -> {
                // restart started plugin
                if (getPlugin(pluginId) != null) {
                    doStartPlugin(pluginId, false);
                }
            });
            applicationContext.publishEvent(new HaloPluginStateChangedEvent(applicationContext));
        } else {
            startPlugins();
        }
    }

    public PluginState reloadPlugins(String pluginId) {
        PluginWrapper plugin = getPlugin(pluginId);
        doStopPlugin(pluginId, false);
        unloadPlugin(pluginId, false);
        try {
            loadPlugin(plugin.getPluginPath());
        } catch (Exception ex) {
            return null;
        }

        return doStartPlugin(pluginId, true);
    }

    // end-region
}
