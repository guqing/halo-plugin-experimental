package run.halo.app.extensions.config;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import run.halo.app.extensions.SpringPluginManager;
import run.halo.app.extensions.config.model.PluginInfo;
import run.halo.app.extensions.extpoint.IHaloPlugin;

/**
 * Plugin manager controller.
 *
 * @author guqing
 * @date 2021-11-02
 */
@RestController
public class PluginManagerController {

    @Autowired
    private SpringPluginManager pluginManager;

    @GetMapping(value = "${halo.plugin.controller.base-path:/plugins}/list")
    public List<PluginInfo> list() {
        pluginManager.getExtensions(IHaloPlugin.class)
            .forEach(p -> {
                System.out.println("--------------->" + p.saySomething());
            });
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
        PluginState pluginState = pluginManager.reloadPlugins(pluginId);
        return pluginState == PluginState.STARTED ? 0 : 1;
    }

    @PostMapping(value = "${halo.plugin.controller.base-path:/plugins}/reload-all")
    public int reloadAll() {
        pluginManager.reloadPlugins(false);
        return 0;
    }

}
