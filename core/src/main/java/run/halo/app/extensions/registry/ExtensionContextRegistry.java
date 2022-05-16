package run.halo.app.extensions.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import run.halo.app.extensions.PluginApplicationContext;

/**
 * @author guqing
 * @since 2021-11-15
 */
public class ExtensionContextRegistry {
    private static final ExtensionContextRegistry INSTANCE = new ExtensionContextRegistry();

    private final Map<String, PluginApplicationContext> registry = new ConcurrentHashMap<>();

    public static ExtensionContextRegistry getInstance() {
        return INSTANCE;
    }

    private ExtensionContextRegistry() {
    }

    public void register(String pluginId, PluginApplicationContext context) {
        registry.put(pluginId, context);
    }

    public PluginApplicationContext remove(String pluginId) {
        return registry.remove(pluginId);
    }

    public PluginApplicationContext getByPluginId(String pluginId) {
        PluginApplicationContext context = registry.get(pluginId);
        if (context == null) {
            throw new IllegalArgumentException(
                String.format("The plugin [%s] can not be found.", pluginId));
        }
        return context;
    }

    public boolean containsContext(String pluginId) {
        return registry.containsKey(pluginId);
    }
}
