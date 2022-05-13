package run.halo.app.extensions.registry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author guqing
 * @since 2021-11-15
 */
public class ExtensionContextRegistry {
    private static final ExtensionContextRegistry INSTANCE = new ExtensionContextRegistry();

    private final Map<String, GenericApplicationContext> registry = new HashMap<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public static ExtensionContextRegistry getInstance() {
        return INSTANCE;
    }

    private ExtensionContextRegistry() {
    }

    /**
     * Acquire the read lock when using getMappings and getMappingsByUrl.
     */
    public void acquireReadLock() {
        this.readWriteLock.readLock().lock();
    }

    /**
     * Release the read lock after using getMappings and getMappingsByUrl.
     */
    public void releaseReadLock() {
        this.readWriteLock.readLock().unlock();
    }

    public void register(String pluginId, GenericApplicationContext context) {
        this.readWriteLock.writeLock().lock();
        try {
            if (!context.isActive()) {
                context.refresh();
            }
            registry.put(pluginId, context);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public void unregister(String pluginId) {
        this.readWriteLock.writeLock().lock();
        try {
            GenericApplicationContext context = registry.remove(pluginId);
            context.close();
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public GenericApplicationContext getByPluginId(String pluginId) {
        this.readWriteLock.readLock().lock();
        try {
            GenericApplicationContext context = registry.get(pluginId);
            if(context == null) {
                throw new IllegalArgumentException(pluginId  + "不存在 context");
            }
            return context;
        } finally {
            this.readWriteLock.readLock().unlock();
        }
    }

    public boolean containsContext(String pluginId) {
        return registry.containsKey(pluginId);
    }
}
