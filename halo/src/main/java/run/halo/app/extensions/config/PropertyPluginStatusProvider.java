package run.halo.app.extensions.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.pf4j.PluginStatusProvider;

/**
 * @author Hank CP
 * @author guqing
 */
public class PropertyPluginStatusProvider implements PluginStatusProvider {

    private final List<String> enabledPlugins;
    private final List<String> disabledPlugins;

    public PropertyPluginStatusProvider(PluginProperties pluginProperties) {
        this.enabledPlugins = pluginProperties.getEnabledPlugins() != null
            ? Arrays.asList(pluginProperties.getEnabledPlugins()) : new ArrayList<>();
        this.disabledPlugins = pluginProperties.getDisabledPlugins() != null
            ? Arrays.asList(pluginProperties.getDisabledPlugins()) : new ArrayList<>();
    }

    public static boolean isPropertySet(PluginProperties pluginProperties) {
        return pluginProperties.getEnabledPlugins() != null
            && pluginProperties.getEnabledPlugins().length > 0
            || pluginProperties.getDisabledPlugins() != null
            && pluginProperties.getDisabledPlugins().length > 0;
    }

    @Override
    public boolean isPluginDisabled(String pluginId) {
        if (disabledPlugins.contains(pluginId)) return true;
        return !enabledPlugins.isEmpty() && !enabledPlugins.contains(pluginId);
    }

    @Override
    public void disablePlugin(String pluginId) {
        if (isPluginDisabled(pluginId)) return;
        disabledPlugins.add(pluginId);
        enabledPlugins.remove(pluginId);
    }

    @Override
    public void enablePlugin(String pluginId) {
        if (!isPluginDisabled(pluginId)) return;
        enabledPlugins.add(pluginId);
        disabledPlugins.remove(pluginId);
    }
}
