package run.halo.app.extensions.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.update.UpdateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import run.halo.app.extensions.SpringPluginManager;
import run.halo.app.extensions.TestExtPoint;
import run.halo.app.extensions.config.model.PluginInfo;

/**
 * Plugin manager controller.
 *
 * @author guqing
 * @date 2021-11-02
 */
@Slf4j
@RestController
public class PluginManagerController {

    @Autowired
    private SpringPluginManager pluginManager;
    @Autowired
    private UpdateManager updateManager;

    @GetMapping(value = "${halo.plugin.controller.base-path:/plugins}/list")
    public List<PluginInfo> list() {
        for (TestExtPoint extension : pluginManager.getExtensions(TestExtPoint.class)) {
            System.out.println("--->" + extension.getName());
        }
        List<PluginWrapper> loadedPlugins = pluginManager.getPlugins();

        // loaded plugins
        List<PluginInfo> plugins = loadedPlugins.stream().map(pluginWrapper -> {
            PluginDescriptor descriptor = pluginWrapper.getDescriptor();
            PluginDescriptor latestDescriptor = null;
            try {
                latestDescriptor = pluginManager.getPluginDescriptorFinder()
                    .find(pluginWrapper.getPluginPath());
            } catch (PluginRuntimeException ignored) {
            }
            String newVersion = null;
            if (latestDescriptor != null && !descriptor.getVersion()
                .equals(latestDescriptor.getVersion())) {
                newVersion = latestDescriptor.getVersion();
            }

            return PluginInfo.build(descriptor,
                pluginWrapper.getPluginState(), newVersion,
                pluginManager.getPluginStartingError(pluginWrapper.getPluginId()),
                latestDescriptor == null);
        }).collect(Collectors.toList());

        // yet not loaded plugins
        List<Path> pluginPaths = pluginManager.getPluginRepository().getPluginPaths();
        plugins.addAll(pluginPaths.stream().filter(path ->
            loadedPlugins.stream().noneMatch(plugin -> plugin.getPluginPath().equals(path))
        ).map(path -> {
            PluginDescriptor descriptor = pluginManager
                .getPluginDescriptorFinder().find(path);
            return PluginInfo.build(descriptor, null, null, null, false);
        }).collect(Collectors.toList()));

        return plugins;
    }

    @PostMapping(value = "${halo.plugin.controller.base-path:/plugins}/start/{pluginId}")
    public int start(@PathVariable String pluginId) {
        pluginManager.startPlugin(pluginId);
        return 0;
    }

    @PostMapping(value = "${halo.plugin.controller.base-path:/plugins}/stop/{pluginId}")
    public int stop(@PathVariable String pluginId) {
        pluginManager.stopPlugin(pluginId);
        return 0;
    }

    @PostMapping(value = "${halo.plugin.controller.base-path:/plugins}/reload/{pluginId}")
    public int reload(@PathVariable String pluginId) {
        PluginState pluginState = pluginManager.reloadPlugin(pluginId);
        return pluginState == PluginState.STARTED ? 0 : 1;
    }

    @PostMapping(value = "${halo.plugin.controller.base-path:/plugins}/reload-all")
    public int reloadAll() {
        pluginManager.reloadPlugins(false);
        return 0;
    }

    @PostMapping(value = "${halo.plugin.controller.base-path:/plugins}/unload/{pluginId}")
    public int unload(@PathVariable String pluginId) {
        pluginManager.unloadPlugin(pluginId);
        return 0;
    }

    @PostMapping(value = "${halo.plugin.controller.base-path:/plugins}/resolve/{pluginId}")
    public int resolve(@PathVariable String pluginId) {
        pluginManager.loadPlugin(pluginManager.getPluginsRoot().resolve(pluginId));
        return 0;
    }

    @GetMapping("${halo.plugin.controller.base-path:/plugins}/check-updates")
    public List<org.pf4j.update.PluginInfo> checkUpdates() {
        if (!updateManager.hasUpdates()) {
            return Collections.emptyList();
        }
        // >> keep system up-to-date <<
        boolean systemUpToDate = true;
        List<org.pf4j.update.PluginInfo> updates = updateManager.getUpdates();
        for (org.pf4j.update.PluginInfo plugin : updates) {
            log.debug("Found update for plugin '{}'", plugin.id);
            org.pf4j.update.PluginInfo.PluginRelease lastRelease =
                updateManager.getLastPluginRelease(plugin.id);
            String lastVersion = lastRelease.version;
            String installedVersion =
                pluginManager.getPlugin(plugin.id).getDescriptor().getVersion();
            log.debug("Update plugin '{}' from version {} to version {}", plugin.id,
                installedVersion, lastVersion);
            boolean updated = updateManager.updatePlugin(plugin.id, lastVersion);
            if (updated) {
                log.debug("Updated plugin '{}'", plugin.id);
            } else {
                log.error("Cannot update plugin '{}'", plugin.id);
                systemUpToDate = false;
            }
        }
        // check for available (new) plugins
        if (updateManager.hasAvailablePlugins()) {
            List<org.pf4j.update.PluginInfo> availablePlugins = updateManager.getAvailablePlugins();
            log.debug("Found {} available plugins", availablePlugins.size());
            for (org.pf4j.update.PluginInfo plugin : availablePlugins) {
                log.debug("Found available plugin '{}'", plugin.id);
                org.pf4j.update.PluginInfo.PluginRelease lastRelease =
                    updateManager.getLastPluginRelease(plugin.id);
                String lastVersion = lastRelease.version;
                log.debug("Install plugin '{}' with version {}", plugin.id, lastVersion);
                boolean installed = updateManager.installPlugin(plugin.id, lastVersion);
                if (installed) {
                    log.debug("Installed plugin '{}'", plugin.id);
                } else {
                    log.error("Cannot install plugin '{}'", plugin.id);
                    systemUpToDate = false;
                }
            }
        } else {
            log.debug("No available plugins found");
        }

        if (systemUpToDate) {
            log.debug("System up-to-date");
        }
        return updates;
    }
}
