package run.halo.app.extensions.ac;

/**
 * @author guqing
 * @since 2021-11-04
 */
public final class NamingUtils {

    private NamingUtils() {}

    /**
     * Converts a class name from bytecode format to source code format:
     * com/foo/bar/MyClass to com.foo.bar.MyClass
     *
     * @param className The class name.
     * @return Formatted class name.
     */
    public static String toSourceCodeFormat(final String className) {
        if (className == null) {
            return null;
        }

        return className.replaceAll("/", ".");
    }

    /**
     * Converts a class name from bytecode format to source code format:
     * com.foo.bar.MyClass to com/foo/bar/MyClass
     *
     * @param className The class name.
     * @return Formatted class name.
     */
    public static String toBytecodeFormat(final String className) {
        if (className == null) {
            return null;
        }

        return className.replaceAll("\\.", "/");
    }
}