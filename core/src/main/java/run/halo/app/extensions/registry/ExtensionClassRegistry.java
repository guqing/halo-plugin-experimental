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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author guqing
 * @since 2021-11-15
 */
public class ExtensionClassRegistry {
    private static final ExtensionClassRegistry INSTANCE = new ExtensionClassRegistry();

    private final Map<String, List<ClassDescriptor>> registry = new HashMap<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public Map<String, List<ClassDescriptor>> getRegistrations() {
        return this.registry;
    }

    public static ExtensionClassRegistry getInstance() {
        return INSTANCE;
    }

    private ExtensionClassRegistry() {
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

    public void register(String pluginId, Class<?> clazz) {
        ClassDescriptor classDescriptor = new ClassDescriptor(clazz);
        this.register(pluginId, classDescriptor);
    }

    public void register(String pluginId, String beanName, Class<?> clazz) {
        ClassDescriptor classDescriptor = new ClassDescriptor(beanName, clazz);
        this.register(pluginId, classDescriptor);
    }

    private void register(String pluginId, ClassDescriptor classDescriptor) {
        this.readWriteLock.writeLock().lock();
        try {
            List<ClassDescriptor> classList =
                registry.computeIfAbsent(pluginId, key -> new LinkedList<>());
            classList.add(classDescriptor);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public void unregister(String pluginId, Class<?> bean) {
        this.readWriteLock.writeLock().lock();
        try {
            List<ClassDescriptor> classes = registry.get(pluginId);
            if (CollectionUtils.isEmpty(classes)) {
                return;
            }
            classes.remove(new ClassDescriptor(bean));
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public List<ClassDescriptor> unregister(String pluginId) {
        this.readWriteLock.writeLock().lock();
        try {
            List<ClassDescriptor> removed = registry.remove(pluginId);
            if (CollectionUtils.isEmpty(removed)) {
                return Collections.emptyList();
            }
            return ImmutableList.copyOf(removed);
        } finally {
            this.readWriteLock.writeLock().unlock();
        }
    }

    public Set<Class<?>> getAllExtPoints() {
        acquireReadLock();
        try {
            Set<Class<?>> classes = getRegistrations().values()
                .stream()
                .flatMap(Collection::stream)
                .filter(ClassDescriptor::isExtPoint)
                .map(ClassDescriptor::getTargetClass)
                .collect(Collectors.toSet());

            return ImmutableSet.copyOf(classes);
        } finally {
            releaseReadLock();
        }
    }

    public List<Class<?>> findClassesWithAnnotation(String pluginId,
        Class<? extends Annotation> annotationType) {
        this.acquireReadLock();
        try {
            if (!this.getRegistrations().containsKey(pluginId)) {
                return Collections.emptyList();
            }
            return this.getRegistrations().get(pluginId)
                .stream()
                .map(ClassDescriptor::getTargetClass)
                .filter(clazz -> AnnotatedElementUtils.hasAnnotation(clazz, annotationType))
                .collect(Collectors.toList());
        } finally {
            this.releaseReadLock();
        }
    }

    public List<Class<?>> findClasses(String pluginId, Predicate<ClassDescriptor> filter) {
        this.acquireReadLock();
        try {
            if (!containsPlugin(pluginId)) {
                return Collections.emptyList();
            }
            List<Class<?>> classes = registry.get(pluginId)
                .stream()
                .filter(filter)
                .map(ClassDescriptor::getTargetClass)
                .collect(Collectors.toList());
            return ImmutableList.copyOf(classes);
        } finally {
            this.releaseReadLock();
        }
    }

    public boolean containsPlugin(String pluginId) {
        return registry.containsKey(pluginId);
    }

    public static class ClassDescriptor {

        final Class<?> clazz;
        String name;
        boolean isController;
        boolean isListener;
        boolean isComponent;
        boolean isExtPoint;

        public ClassDescriptor(Class<?> targetClass) {
            this(targetClass.getName(), targetClass);
        }

        public ClassDescriptor(String beanName, Class<?> targetClass) {
            Assert.notNull(beanName, "The beanName must not be null.");
            Assert.notNull(targetClass, "The targetClass must not be null.");
            this.clazz = targetClass;
            this.name = beanName;
            init();
        }

        static String getSimpleName(final String className) {
            return className.substring(className.lastIndexOf('.') + 1);
        }

        public static boolean isComponent(Class<?> clazz) {
            if (AnnotatedElementUtils.hasAnnotation(clazz, Component.class)) {
                return true;
            }
            return AnnotatedElementUtils.hasAnnotation(clazz, Extension.class);
        }

        private void init() {
            // Is it a controller?
            this.isController = AnnotatedElementUtils.hasAnnotation(this.clazz, Controller.class);

            // Specialized classes of @component include @service,@repository,@controller and etc.
            this.isComponent = isComponent(this.clazz);

            this.isExtPoint = ExtensionPoint.class.isAssignableFrom(this.clazz);

            // Is it a listener?
            if (ApplicationListener.class.isAssignableFrom(this.clazz)) {
                this.isListener = true;
            } else {
                for (Method declaredMethod : clazz.getDeclaredMethods()) {
                    if (AnnotatedElementUtils.hasAnnotation(declaredMethod, EventListener.class)) {
                        this.isListener = true;
                        break;
                    }
                }
            }
        }

        public String getSimpleName() {
            return getSimpleName(name);
        }

        public Class<?> getTargetClass() {
            return clazz;
        }

        public String getName() {
            return name;
        }

        public boolean isController() {
            return isController;
        }

        public boolean isListener() {
            return isListener;
        }

        public boolean isComponent() {
            return isComponent;
        }

        public boolean isExtPoint() {
            return isExtPoint;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ClassDescriptor that = (ClassDescriptor) o;
            return clazz.equals(that.clazz);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz);
        }
    }
}
