package run.halo.app.extensions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ExtensionFactory;
import org.pf4j.ExtensionFinder;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
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
import org.springframework.util.StopWatch;
import run.halo.app.extensions.config.model.PluginStartingError;
import run.halo.app.extensions.event.HaloPluginStartedEvent;
import run.halo.app.extensions.event.HaloPluginStateChangedEvent;
import run.halo.app.extensions.event.HaloPluginStoppedEvent;
import run.halo.app.extensions.internal.PluginRequestMappingManager;
import run.halo.app.extensions.internal.SpringExtensionFactory;

/**
 * PluginManager to hold the main ApplicationContext
 *
 * @author guqing
 * @date 2021-11-01
 */
@Slf4j
public class SpringPluginManager extends DefaultPluginManager
    implements ApplicationContextAware, InitializingBean {

    private final Map<String, PluginStartingError> startingErrors = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private ApplicationContext rootApplicationContext;
    private PluginApplicationInitializer pluginApplicationInitializer;
    private PluginRequestMappingManager requestMappingManager;

    public SpringPluginManager() {
        super();
    }

    public SpringPluginManager(Path pluginsRoot) {
        super(pluginsRoot);
    }

    public void acquireLock() {
        this.lock.lock();
    }

    public void releaseLock() {
        this.lock.unlock();
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new SpringExtensionFactory(this);
    }

    @Override
    public PluginDescriptorFinder getPluginDescriptorFinder() {
        return super.getPluginDescriptorFinder();
    }

    @Override
    protected ExtensionFinder createExtensionFinder() {
        return new ScanningExtensionFinder(this);
    }

    public PluginRepository getPluginRepository() {
        return super.pluginRepository;
    }

    public ApplicationContext getRootApplicationContext() {
        return this.rootApplicationContext;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext rootApplicationContext)
        throws BeansException {
        this.rootApplicationContext = rootApplicationContext;
    }

    public PluginApplicationContext getPluginApplicationContext(String pluginId) {
        return pluginApplicationInitializer.getPluginApplicationContext(pluginId);
    }

    @Override
    public void afterPropertiesSet() {
        // This method load, start plugins and inject extensions in Spring
        loadPlugins();
        this.pluginApplicationInitializer = new PluginApplicationInitializer(this);

        this.requestMappingManager =
            rootApplicationContext.getBean(PluginRequestMappingManager.class);
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
                    releaseAdditionalResources(pluginWrapper.getPluginId());

                    rootApplicationContext.publishEvent(
                        new HaloPluginStoppedEvent(this, pluginWrapper));
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
    protected void firePluginStateEvent(PluginStateEvent event) {
        rootApplicationContext.publishEvent(
            new HaloPluginStateChangedEvent(this, event.getPlugin(), event.getOldState()));
        super.firePluginStateEvent(event);
    }

    private PluginState doStopPlugin(String pluginId, boolean stopDependents) {
        checkPluginId(pluginId);

        PluginWrapper pluginWrapper = getPlugin(pluginId);
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        PluginState pluginState = pluginWrapper.getPluginState();
        if (PluginState.STOPPED == pluginState) {
            log.debug("Already stopped plugin '{}'", getPluginLabel(pluginDescriptor));
            return PluginState.STOPPED;
        }

        // test for disabled plugin
        if (PluginState.DISABLED == pluginState) {
            // do nothing
            return pluginState;
        }

        if (stopDependents) {
            List<String> dependents = dependencyResolver.getDependents(pluginId);
            while (!dependents.isEmpty()) {
                String dependent = dependents.remove(0);
                doStopPlugin(dependent, false);
                dependents.addAll(0, dependencyResolver.getDependents(dependent));
            }
        }
        try {
            log.info("Stop plugin '{}'", getPluginLabel(pluginDescriptor));
            pluginWrapper.getPlugin().stop();
            pluginWrapper.setPluginState(PluginState.STOPPED);
            // release plugin resources
            releaseAdditionalResources(pluginId);

            startedPlugins.remove(pluginWrapper);

            rootApplicationContext.publishEvent(new HaloPluginStoppedEvent(this, pluginWrapper));
            firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            startingErrors.put(pluginWrapper.getPluginId(), PluginStartingError.of(
                pluginWrapper.getPluginId(), e.getMessage(), e.toString()));
        }
        return pluginWrapper.getPluginState();
    }

    @Override
    public void startPlugins() {
        startingErrors.clear();
        long ts = System.currentTimeMillis();

        for (PluginWrapper pluginWrapper : resolvedPlugins) {
            PluginState pluginState = pluginWrapper.getPluginState();
            if ((PluginState.DISABLED != pluginState) && (PluginState.STARTED != pluginState)) {
                try {
                    log.info("Start plugin '{}'", getPluginLabel(pluginWrapper.getDescriptor()));
                    // inject bean
                    pluginApplicationInitializer.onStartUp(pluginWrapper.getPluginId());

                    pluginWrapper.getPlugin().start();

                    requestMappingManager.registerControllers(pluginWrapper);

                    pluginWrapper.setPluginState(PluginState.STARTED);
                    pluginWrapper.setFailedException(null);
                    startedPlugins.add(pluginWrapper);

                    rootApplicationContext.publishEvent(
                        new HaloPluginStartedEvent(this, pluginWrapper));
                } catch (Exception | LinkageError e) {
                    pluginWrapper.setPluginState(PluginState.FAILED);
                    pluginWrapper.setFailedException(e);
                    startingErrors.put(pluginWrapper.getPluginId(), PluginStartingError.of(
                        pluginWrapper.getPluginId(), e.getMessage(), e.toString()));
                    releaseAdditionalResources(pluginWrapper.getPluginId());
                    log.error("Unable to start plugin '{}'",
                        getPluginLabel(pluginWrapper.getDescriptor()), e);
                } finally {
                    firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
                }
            }
        }

        log.info("[Halo] {} plugins are started in {}ms. {} failed",
            getPlugins(PluginState.STARTED).size(),
            System.currentTimeMillis() - ts, startingErrors.size());
    }

    @Override
    public PluginState startPlugin(String pluginId) {
        return doStartPlugin(pluginId);
    }

    @Override
    public void stopPlugins() {
        doStopPlugins();
    }

    @Override
    public PluginState stopPlugin(String pluginId) {
        return doStopPlugin(pluginId, true);
    }

    private PluginState doStartPlugin(String pluginId) {
        checkPluginId(pluginId);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("根据插件id获取插件");
        PluginWrapper pluginWrapper = getPlugin(pluginId);
        PluginDescriptor pluginDescriptor = pluginWrapper.getDescriptor();
        PluginState pluginState = pluginWrapper.getPluginState();
        if (PluginState.STARTED == pluginState) {
            log.debug("Already started plugin '{}'", getPluginLabel(pluginDescriptor));
            return PluginState.STARTED;
        }

        if (!resolvedPlugins.contains(pluginWrapper)) {
            log.warn("Cannot start an unresolved plugin '{}'", getPluginLabel(pluginDescriptor));
            return pluginState;
        }

        if (PluginState.DISABLED == pluginState) {
            // automatically enable plugin on manual plugin start
            if (!enablePlugin(pluginId)) {
                return pluginState;
            }
        }
        stopWatch.stop();

        stopWatch.start("启动插件依赖项");
        for (PluginDependency dependency : pluginDescriptor.getDependencies()) {
            // start dependency only if it marked as required (non-optional) or if it optional and loaded
            if (!dependency.isOptional() || plugins.containsKey(dependency.getPluginId())) {
                startPlugin(dependency.getPluginId());
            }
        }
        stopWatch.stop();
        log.info("Start plugin '{}'", getPluginLabel(pluginDescriptor));

        try {
            stopWatch.start("创建插件application context");
            // load and inject bean
            pluginApplicationInitializer.onStartUp(pluginId);
            stopWatch.stop();

            // create plugin instance and start it
            pluginWrapper.getPlugin().start();

            stopWatch.start("注册插件APIs");
            requestMappingManager.registerControllers(pluginWrapper);
            stopWatch.stop();
            log.debug("Total millis: {} ms -> {}", stopWatch.getTotalTimeMillis(), stopWatch.prettyPrint());

            pluginWrapper.setPluginState(PluginState.STARTED);
            startedPlugins.add(pluginWrapper);

            rootApplicationContext.publishEvent(new HaloPluginStartedEvent(this, pluginWrapper));
        } catch (Exception e) {
            log.error("Unable to start plugin '{}'",
                getPluginLabel(pluginWrapper.getDescriptor()), e);
            pluginWrapper.setPluginState(PluginState.FAILED);
            startingErrors.put(pluginWrapper.getPluginId(), PluginStartingError.of(
                pluginWrapper.getPluginId(), e.getMessage(), e.toString()));
            releaseAdditionalResources(pluginId);
        } finally {
            firePluginStateEvent(new PluginStateEvent(this, pluginWrapper, pluginState));
        }
        return pluginWrapper.getPluginState();
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
                    doStartPlugin(pluginId);
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

        return doStartPlugin(pluginId);
    }

    /**
     * Release plugin holding release on stop.
     */
    public void releaseAdditionalResources(String pluginId) {
        // release request mapping
        requestMappingManager
            .removeControllerMapping(this, pluginId);
        try {
            pluginApplicationInitializer.contextDestroyed(pluginId);
        } catch (Exception e) {
            log.trace("Plugin application context close failed. ", e);
        }
    }

    // end-region
}
