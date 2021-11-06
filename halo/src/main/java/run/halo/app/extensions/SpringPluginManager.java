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
import org.pf4j.ExtensionWrapper;
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
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.lang.NonNull;
import run.halo.app.extensions.config.model.PluginStartingError;
import run.halo.app.extensions.event.HaloPluginStateChangedEvent;
import run.halo.app.extensions.internal.PluginListenerRegistry;

/**
 * PluginManager to hold the main ApplicationContext
 *
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
    private PluginListenerRegistry listenerRegistry;

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

    public PluginListenerRegistry getListenerRegistry() {
        return listenerRegistry;
    }

    public void setListenerRegistry(PluginListenerRegistry listenerRegistry) {
        this.listenerRegistry = listenerRegistry;
    }

    @Override
    public void afterPropertiesSet() {
        // This method load, start plugins and inject extensions in Spring
        loadPlugins();
        this.extensionsInjector = new ExtensionsInjector(this);
        this.extensionsInjector.injectExtensions();

        // create listener registry
        this.listenerRegistry = new PluginListenerRegistry(this.applicationContext);
    }

    public PluginStartingError getPluginStartingError(String pluginId) {
        return startingErrors.get(pluginId);
    }

    @Override
    public <T> List<T> getExtensions(Class<T> type) {
        return this.getExtensions(extensionFinder.find(type));
    }

    @Override
    public <T> List<T> getExtensions(Class<T> type, String pluginId) {
        return this.getExtensions(extensionFinder.find(type, pluginId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List getExtensions(String pluginId) {
        List<ExtensionWrapper> extensionsWrapper = extensionFinder.find(pluginId);
        List extensions = new ArrayList<>(extensionsWrapper.size());
        for (ExtensionWrapper extensionWrapper : extensionsWrapper) {
            try {
                Object extension = getExtension(extensionWrapper);
                extensions.add(extension);
            } catch (PluginRuntimeException e) {
                log.error("Cannot retrieve extension", e);
            }
        }

        return extensions;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private synchronized <T> T getExtension(ExtensionWrapper<T> extensionWrapper) {
        T extension = extensionWrapper.getExtension();
        if (extension == null) {
            Class<?> extensionClass = extensionWrapper.getDescriptor().extensionClass;
            extension = (T) applicationContext.getBean(extensionClass);
        }
        return extension;
    }

    protected <T> List<T> getExtensions(List<ExtensionWrapper<T>> extensionsWrapper) {
        List<T> extensions = new ArrayList<>(extensionsWrapper.size());
        for (ExtensionWrapper<T> extensionWrapper : extensionsWrapper) {
            try {
                T extension = getExtension(extensionWrapper);
                extensions.add(extension);
            } catch (PluginRuntimeException e) {
                log.error("Cannot retrieve extension", e);
            }
        }

        return extensions;
    }


    public Set<Class<?>> getControllers(String pluginId) {
        return this.extensionsInjector.getControllers(pluginId);
    }

    public void registerListenerBy(String pluginId) {
        Set<Class<?>> listeners = this.extensionsInjector.getListenerClasses(pluginId);
        for (Class<?> listener : listeners) {
            listenerRegistry.addPluginListener(pluginId, listener);
        }
    }

    public void unregisterListenerBy(String pluginId) {
        this.listenerRegistry.removePluginListener(pluginId);
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

    @Override
    protected synchronized void firePluginStateEvent(PluginStateEvent event) {
        applicationContext.publishEvent(
            new HaloPluginStateChangedEvent(this, event.getPlugin(), event.getOldState()));
        super.firePluginStateEvent(event);
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
                    new HaloPluginStateChangedEvent(this, plugin, previousState));
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
                    new HaloPluginStateChangedEvent(this, plugin, previousState));
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
    }

    @Override
    public PluginState startPlugin(String pluginId) {
        return doStartPlugin(pluginId, true);
    }

    @Override
    public void stopPlugins() {
        doStopPlugins();
    }

    @Override
    public PluginState stopPlugin(String pluginId) {
        AbstractApplicationContext context = (AbstractApplicationContext)getApplicationContext();
        for (ApplicationListener<?> applicationListener : context.getApplicationListeners()) {
            System.out.println(applicationListener);
        }
        return doStopPlugin(pluginId, true);
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
        } else {
            startPlugins();
        }
    }

    public PluginState reloadPlugin(String pluginId) {
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
