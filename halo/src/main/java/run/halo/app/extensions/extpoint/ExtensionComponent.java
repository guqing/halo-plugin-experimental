package run.halo.app.extensions.extpoint;

import org.pf4j.Extension;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * @author guqing
 * @since 2021-11-08
 */
public class ExtensionComponent<T> implements Comparable<ExtensionComponent<T>> {

    private final T instance;
    private final double ordinal;

    public ExtensionComponent(T instance, double ordinal) {
        this.instance = instance;
        this.ordinal = ordinal;
    }

    public ExtensionComponent(T instance) {
        this(instance, 0);
    }

    public ExtensionComponent(T instance, Extension annotation) {
        this(instance, annotation.ordinal());
    }

    public static <T> ExtensionComponent<T> create(T instance) {
        Extension annotation =
            AnnotationUtils.findAnnotation(instance.getClass(), Extension.class);
        if (annotation != null) {
            return new ExtensionComponent<T>(instance, annotation);
        }
        return new ExtensionComponent<T>(instance);
    }

    /**
     * See {@link Extension#ordinal()}. Used to sort extensions.
     */
    public double ordinal() {
        return ordinal;
    }


    /**
     * The instance of the discovered extension.
     *
     * @return never null.
     */
    public T getInstance() {
        return instance;
    }

    /**
     * Sort {@link ExtensionComponent}s in the descending order of {@link #ordinal()}.
     */
    @Override
    public int compareTo(ExtensionComponent<T> that) {
        double a = this.ordinal();
        double b = that.ordinal();
        if (Double.compare(a, b) > 0) {
            return -1;
        }
        if (Double.compare(a, b) < 0) {
            return 1;
        }

        String thisLabel = this.instance.getClass().getName();

        String thatLabel = that.instance.getClass().getName();

        return thisLabel.compareTo(thatLabel);
    }
}
