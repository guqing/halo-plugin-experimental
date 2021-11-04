package run.halo.app.extensions.ac;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * The coupling filter specifies the RegEx patterns to filter the couplings found at analysis,
 * before generating the output data.
 *
 * @author guqing
 * @date 2021-11-04
 * @see <a href="https://github.com/ExpediaGroup/jarviz">jarviz</a>
 */
public interface CouplingFilter {

    static boolean verifyPatternAvailable(final Callable<Optional<Pattern>> callable) {
        try {
            return callable.call().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Optional RegEx pattern to match with the package name (of the source class) in the coupling.
     * e.g.: To match any source package name starting with com.xyz.foo or com.xyz.bar (including
     * sub-packages):
     * <code>"^(com\\.xyz\\.foo|com\\.xyz\\.bar).*$"</code>
     *
     * @return The source package name.
     */
    Optional<String> getSourcePackage();

    /**
     * Optional RegEx pattern to match with the name of the source class in the coupling. Note that
     * this matches with only the simple class name, not the fully qualified class name. For the
     * class "com.xyz.foo.MyClass" this will math the pattern against "MyClass". e.g.: To match any
     * source class name to ABC, Xyz or Hello:
     * <code>"^(ABC|Xyz|Hello)$"</code>
     *
     * @return The source class name.
     */
    Optional<String> getSourceClass();

    /**
     * Optional RegEx pattern to match with the method name (of the source class) in the coupling.
     * e.g.: To match a method name to getToken, setToken or isToken:
     * <code>"^(get|set|is)Token$"</code>
     *
     * @return The source method name.
     */
    Optional<String> getSourceMethod();

    /**
     * Optional RegEx pattern to match with the package name (of the target class) in the coupling.
     * e.g.: To exactly match any target package to com.xyz.foo or com.xyz.bar (excluding
     * sub-packages):
     * <code>"^(com\\.xyz\\.foo|com\\.xyz\\.bar)$"</code>
     *
     * @return The target package name.
     */
    Optional<String> getTargetPackage();

    /**
     * Optional RegEx pattern to match with the name of the target class in the coupling. Note that
     * this matches only the simple class name, not the fully qualified class name. For the class
     * "com.xyz.foo.MyClass" this will match the pattern against "MyClass". e.g.: To match any
     * target class name to MyClass1, MyClass2, MyClass3 or MyClass4:
     * <code>"^MyClass[1-4]$"</code>
     *
     * @return The target class name.
     */
    Optional<String> getTargetClass();

    /**
     * Optional RegEx pattern to match with the method name (of the target class) in the coupling.
     * e.g.: To exactly match a method name to myMethod:
     * <code>"^myMethod$"</code>
     *
     * @return The target method name.
     */
    Optional<String> getTargetMethod();

    @JsonIgnore
    default Pattern getSourcePackagePattern() {
        return getSourcePackage().map(Pattern::compile).orElse(null);
    }

    @JsonIgnore
    default Pattern getSourceClassPattern() {
        return getSourceClass().map(Pattern::compile).orElse(null);
    }

    @JsonIgnore
    default Pattern getSourceMethodPattern() {
        return getSourceMethod().map(Pattern::compile).orElse(null);
    }

    @JsonIgnore
    default Pattern getTargetPackagePattern() {
        return getTargetPackage().map(Pattern::compile).orElse(null);
    }

    @JsonIgnore
    default Pattern getTargetClassPattern() {
        return getTargetClass().map(Pattern::compile).orElse(null);
    }

    @JsonIgnore
    default Pattern getTargetMethodPattern() {
        return getTargetMethod().map(Pattern::compile).orElse(null);
    }

    class Builder {

        String sourcePackage;
        String sourceClass;
        String sourceMethod;
        String targetPackage;
        String targetClass;
        String targetMethod;

        public Builder setSourcePackage(String sourcePackage) {
            this.sourcePackage = sourcePackage;
            return this;
        }

        public Builder setSourceClass(String sourceClass) {
            this.sourceClass = sourceClass;
            return this;
        }

        public Builder setSourceMethod(String sourceMethod) {
            this.sourceMethod = sourceMethod;
            return this;
        }

        public Builder setTargetPackage(String targetPackage) {
            this.targetPackage = targetPackage;
            return this;
        }

        public Builder setTargetClass(String targetClass) {
            this.targetClass = targetClass;
            return this;
        }

        public Builder setTargetMethod(String targetMethod) {
            this.targetMethod = targetMethod;
            return this;
        }

        public CouplingFilter build() {
            return new CouplingFilter() {
                @Override
                public Optional<String> getSourcePackage() {
                    return Optional.ofNullable(sourcePackage);
                }

                @Override
                public Optional<String> getSourceClass() {
                    return Optional.ofNullable(sourceClass);
                }

                @Override
                public Optional<String> getSourceMethod() {
                    return Optional.ofNullable(sourceMethod);
                }

                @Override
                public Optional<String> getTargetPackage() {
                    return Optional.ofNullable(targetPackage);
                }

                @Override
                public Optional<String> getTargetClass() {
                    return Optional.ofNullable(targetClass);
                }

                @Override
                public Optional<String> getTargetMethod() {
                    return Optional.ofNullable(targetMethod);
                }
            };
        }
    }
}