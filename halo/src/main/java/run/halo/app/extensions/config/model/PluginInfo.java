package run.halo.app.extensions.config.model;

import java.util.ArrayList;
import java.util.List;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginState;

/**
 * @author <a href="https://github.com/hank-cp">Hank CP</a>
 */
public class PluginInfo implements PluginDescriptor {

    public String pluginId;

    public String pluginDescription;

    public String pluginClass;

    public String version;

    public String requires;

    public String provider;

    public String license;

    public List<PluginDependency> dependencies;

    public PluginState pluginState;

    public String newVersion;

    public boolean removed;

    public PluginStartingError startingError;

    public static PluginInfo build(PluginDescriptor descriptor,
        PluginState pluginState,
        String newVersion,
        PluginStartingError startingError,
        boolean removed) {
        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.pluginId = descriptor.getPluginId();
        pluginInfo.pluginDescription = descriptor.getPluginDescription();
        pluginInfo.pluginClass = descriptor.getPluginClass();
        pluginInfo.version = descriptor.getVersion();
        pluginInfo.requires = descriptor.getRequires();
        pluginInfo.provider = descriptor.getProvider();
        pluginInfo.license = descriptor.getLicense();
        if (descriptor.getDependencies() != null) {
            pluginInfo.dependencies = new ArrayList<>(descriptor.getDependencies());
        }
        pluginInfo.pluginState = pluginState;
        pluginInfo.startingError = startingError;
        pluginInfo.newVersion = newVersion;
        pluginInfo.removed = removed;
        return pluginInfo;
    }

    @Override
    public String getPluginId() {
        return this.pluginId;
    }

    @Override
    public String getPluginDescription() {
        return this.pluginDescription;
    }

    @Override
    public String getPluginClass() {
        return this.pluginClass;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public String getRequires() {
        return this.requires;
    }

    @Override
    public String getProvider() {
        return this.provider;
    }

    @Override
    public String getLicense() {
        return this.license;
    }

    @Override
    public List<PluginDependency> getDependencies() {
        return this.dependencies;
    }
}
