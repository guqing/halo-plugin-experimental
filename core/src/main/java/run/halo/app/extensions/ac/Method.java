package run.halo.app.extensions.ac;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Comparator;

/**
 * @author guqing
 * @since 2021-11-04
 */
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public interface Method {

    Comparator<Method> COMPARATOR = (a, b) -> {
        final int comparison = a.getClassName().compareTo(b.getClassName());
        if (comparison == 0) {
            return a.getMethodName().compareTo(b.getMethodName());
        }
        return comparison;
    };

    /**
     * Fully qualified class name
     * e.g.: foo.bar.MyClass
     *
     * @return The class name.
     */
    String getClassName();

    /**
     * Method name in the class
     * e.g.: doThat
     *
     * @return The method name.
     */
    String getMethodName();

    /**
     * e.g.: foo.bar.MyClass#doThat
     *
     * @return The short toString value.
     */
    @JsonIgnore
    default String toStringShort() {
        return getClassName() + "#" + getMethodName();
    }

    /**
     * Simple class name
     * e.g.: For "foo.bar.MyClass", returns "MyClass"
     *
     * @return The simple class name.
     */
    default String getSimpleClassName() {
        final String fullName = getClassName();
        final int index = fullName.lastIndexOf('.');
        return index > 0 ? fullName.substring(index + 1) : fullName;
    }

    /**
     * Package name of the class
     * e.g.: For "foo.bar.MyClass", returns "foo.bar"
     *
     * @return The package name.
     */
    default String getPackageName() {
        final String fullName = getClassName();
        final int index = fullName.lastIndexOf('.');
        return index > 0 ? fullName.substring(0, index) : "";
    }

    class Builder {
        String className;
        String methodName;

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Method build() {
            return new Method() {
                @Override
                public String getClassName() {
                    return className;
                }

                @Override
                public String getMethodName() {
                    return methodName;
                }
            };
        }
    }

}