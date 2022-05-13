package run.halo.app.extensions.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author guqing
 * @since 2021-11-15
 */
public class ExtensionContextRegistry {
    private static final ExtensionContextRegistry INSTANCE = new ExtensionContextRegistry();

    private final Map<String, GenericApplicationContext> registry = new ConcurrentHashMap<>();

    public static ExtensionContextRegistry getInstance() {
        return INSTANCE;
    }

    private ExtensionContextRegistry() {
    }

    public void register(String pluginId, GenericApplicationContext context) {
        if (!context.isActive()) {
            context.refresh();
        }
        registry.put(pluginId, context);
    }

    public void unregister(String pluginId) {
        GenericApplicationContext context = registry.remove(pluginId);
        context.close();
    }

    public synchronized GenericApplicationContext getByPluginId(String pluginId) {
        GenericApplicationContext context = registry.get(pluginId);
        if (context == null) {
            throw new IllegalArgumentException(
                String.format("No corresponding plugin [%s] was found.", pluginId));
        }
        return context;
    }

    public boolean containsContext(String pluginId) {
        return registry.containsKey(pluginId);
    }
}
