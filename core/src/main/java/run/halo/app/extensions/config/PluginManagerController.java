package run.halo.app.extensions.config;

import com.google.common.collect.Maps;
import freemarker.template.TemplateException;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginRuntimeException;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;
import run.halo.app.exception.NotFoundException;
import run.halo.app.exception.ServiceException;
import run.halo.app.extensions.SpringPluginManager;
import run.halo.app.extensions.config.model.PluginInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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
    @Autowired
    private FreeMarkerConfig freeMarkerConfig;

    @GetMapping(value = "${halo.plugin.controller.base-path:/plugins}/list")
    public List<PluginInfo> list() {
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

    @GetMapping(value = "${halo.plugin.controller.base-path:/plugins}/web/{pluginId}/**")
    public String web(@PathVariable String pluginId, HttpServletRequest req) {
        PluginWrapper plugin = pluginManager.getPlugin(pluginId);
        if (plugin == null || PluginState.STARTED.equals(plugin.getPluginState())) {
            throw new ServiceException(pluginId + " 插件未启动");
        }
        var url = req.getRequestURI();
        var str = StringUtils.substringAfter(url, pluginId + "/");
        var template = "";
        if (str.length() <= 1) {
            template = pluginId + "/index.ftl";
        } else {
            template = pluginId + "/" + str;
            if (!str.contains(".")) {
                template += ".ftl";
            }
        }
        if (StringUtils.isEmpty(template)) {
            throw new NotFoundException("NOT FOUND " + template);
        }
        try (var writer = new StringBuilderWriter()) {
            freeMarkerConfig.getConfiguration().getTemplate(template).process(Maps.newHashMap(), writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new NotFoundException("NOT FOUND " + template);
        }
    }

}
