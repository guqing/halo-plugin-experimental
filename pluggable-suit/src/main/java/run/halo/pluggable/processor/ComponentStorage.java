package run.halo.pluggable.processor;

import java.util.Map;
import java.util.Set;

/**
 * It's a storage (database) that persists components of spring framework.
 * The standard operations supported by storage are {@link #read} and {@link #write}.
 * The storage is populated by {@link PluggableAnnotationProcessor}.
 *
 * @author guqing
 */
public abstract class ComponentStorage {

    protected final PluggableAnnotationProcessor processor;

    public ComponentStorage(PluggableAnnotationProcessor processor) {
        this.processor = processor;
    }

    public abstract Map<String, Set<String>> read();

    public abstract void write(Map<String, Set<String>> extensions);
}
