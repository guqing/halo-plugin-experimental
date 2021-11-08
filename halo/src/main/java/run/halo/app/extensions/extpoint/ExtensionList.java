package run.halo.app.extensions.extpoint;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import run.halo.app.extensions.SpringPluginManager;

/**
 * @author guqing
 * @since 2021-11-08
 */
public class ExtensionList<T> extends AbstractList<T> {

    public final Class<T> extensionType;
    private final SpringPluginManager pluginManager;
    private volatile List<ExtensionComponent<T>> extensions;

    protected ExtensionList(SpringPluginManager pluginManager, Class<T> extensionType) {
        this.pluginManager = pluginManager;
        this.extensionType = extensionType;
        if (pluginManager == null) {
            extensions = Collections.emptyList();
        }
    }

    /**
     * Looks for the extension instance of the given type (subclasses excluded), or return null.
     */
    public <U extends T> U get(@NonNull Class<U> type) {
        for (T ext : this) {
            if (ext.getClass() == type) {
                return type.cast(ext);
            }
        }

        return null;
    }

    /**
     * Looks for the extension instance of the given type (subclasses excluded), or throws an
     * IllegalStateException.
     * <p>
     * Meant to simplify call inside @Extension annotated class to retrieve their own instance.
     */
    @NonNull
    public <U extends T> U getInstance(@NonNull Class<U> type) throws IllegalStateException {
        for (T ext : this) {
            if (ext.getClass() == type) {
                return type.cast(ext);
            }
        }

        throw new IllegalStateException(
            "The class " + type.getName() + " was not found, potentially not yet loaded");
    }

    @Override
    @NonNull
    public Iterator<T> iterator() {
        // we need to intercept mutation, so for now don't allow Iterator.remove
        return new Iterators<>(Collections.unmodifiableList(ensureLoaded()).iterator()) {
            @Override
            protected T expand(ExtensionComponent<T> item) {
                return item.getInstance();
            }
        };
    }

    /**
     * Gets the same thing as the 'this' list represents, except as {@link ExtensionComponent}s.
     */
    public List<ExtensionComponent<T>> getComponents() {
        return Collections.unmodifiableList(ensureLoaded());
    }


    private List<ExtensionComponent<T>> ensureLoaded() {
        if (extensions != null) {
            return extensions; // already loaded
        }
        pluginManager.acquireLock();
        try {
            List<ExtensionComponent<T>> collect = pluginManager.getExtensions(extensionType)
                .stream()
                .map(ExtensionComponent::create)
                .collect(Collectors.toList());
            this.extensions = sort(collect);
            return this.extensions;
        } finally {
            pluginManager.releaseLock();
        }
    }

    @Override
    public T get(int index) {
        return ensureLoaded().get(index).getInstance();
    }

    @Override
    public int size() {
        return ensureLoaded().size();
    }

    @Override
    public final synchronized T remove(int index) {
        T t = get(index);
        remove(t);
        return t;
    }

    private synchronized void addSync(T t) {
        // if we've already filled extensions, add it
        if (extensions != null) {
            List<ExtensionComponent<T>> r = new ArrayList<>(extensions);
            r.add(ExtensionComponent.create(t));
            extensions = sort(r);
        }
    }

    protected List<ExtensionComponent<T>> sort(List<ExtensionComponent<T>> r) {
        r = new ArrayList<>(r);
        Collections.sort(r);
        return r;
    }

    @Override
    public void add(int index, T element) {
        addSync(element);
    }

    /**
     * Gets the read-only view of this {@link ExtensionList} where components are reversed.
     */
    public List<T> reverseView() {
        return new AbstractList<T>() {
            @Override
            public T get(int index) {
                return ExtensionList.this.get(size() - index - 1);
            }

            @Override
            public int size() {
                return ExtensionList.this.size();
            }
        };
    }
}
