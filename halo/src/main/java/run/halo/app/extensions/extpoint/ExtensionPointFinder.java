package run.halo.app.extensions.extpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.halo.app.extensions.SpringPluginManager;

/**
 * @author guqing
 * @since 2021-11-08
 */
@Component
public class ExtensionPointFinder {

    @SuppressWarnings("rawtypes")
    private final transient Map<Class<?>, ExtensionList> extensionLists = new ConcurrentHashMap<>();

    @Autowired
    private SpringPluginManager pluginManager;

    @SuppressWarnings("unchecked")
    public <T> ExtensionList<T> lookup(Class<T> extensionType) {
        ExtensionList<T> extensionList = extensionLists.get(extensionType);
        return extensionList != null ? extensionList : extensionLists.computeIfAbsent(extensionType,
            key -> new ExtensionList<>(pluginManager, key));
    }

    /**
     * Refresh {@link ExtensionList}s by adding all the newly discovered extensions.
     * <p>
     * Exposed only for {@link PluginManager}.
     */
    public void refreshExtensions() {
        this.extensionLists.clear();
    }
}
