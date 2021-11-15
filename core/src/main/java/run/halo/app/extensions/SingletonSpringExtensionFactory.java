package run.halo.app.extensions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pf4j.PluginManager;
import run.halo.app.extensions.internal.SpringExtensionFactory;

/**
 * A {@link SpringExtensionFactory} that always returns a specific instance.
 * Optional you can specify the extension classes for which you want singletons.
 *
 * @author Decebal Suiu
 * @author guqing
 * @since 2021-11-01
 */
public class SingletonSpringExtensionFactory extends SpringExtensionFactory {
    private final List<String> extensionClassNames;

    private final Map<String, Object> cache;

    public SingletonSpringExtensionFactory(PluginManager pluginManager) {
        this(pluginManager, true);
    }

    public SingletonSpringExtensionFactory(PluginManager pluginManager, String... extensionClassNames) {
        this(pluginManager, true, extensionClassNames);
    }

    public SingletonSpringExtensionFactory(PluginManager pluginManager, boolean autowire, String... extensionClassNames) {
        super(pluginManager, autowire);

        this.extensionClassNames = Arrays.asList(extensionClassNames);

        // simple cache implementation
        cache = new HashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> extensionClass) {
        String extensionClassName = extensionClass.getName();
        if (cache.containsKey(extensionClassName)) {
            return (T) cache.get(extensionClassName);
        }

        T extension = super.create(extensionClass);
        if (extensionClassNames.isEmpty() || extensionClassNames.contains(extensionClassName)) {
            cache.put(extensionClassName, extension);
        }

        return extension;
    }


}
