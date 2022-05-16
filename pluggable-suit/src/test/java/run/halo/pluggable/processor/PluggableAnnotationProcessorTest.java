package run.halo.pluggable.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;
import org.pf4j.processor.ExtensionAnnotationProcessor;
import org.pf4j.processor.LegacyExtensionStorage;

/**
 * @author Mario Franco
 * @author Decebal Suiu
 */
public class PluggableAnnotationProcessorTest {

    public static final JavaFileObject Greeting = JavaFileObjects.forSourceLines(
        "Greeting",
        "package test;",
        "import org.pf4j.ExtensionPoint;",
        "",
        "public interface Greeting extends ExtensionPoint {",
        "   String getGreeting();",
        "}");

    public static final JavaFileObject WhazzupGreeting = JavaFileObjects.forSourceLines(
        "WhazzupGreeting",
        "package test;",
        "import org.springframework.stereotype.Service;",
        "",
        "@Service",
        "public class WhazzupGreeting implements Greeting {",
        "   @Override",
        "    public String getGreeting() {",
        "       return \"Whazzup\";",
        "    }",
        "}");

    public static final JavaFileObject WhazzupGreeting_NoExtensionPoint =
        JavaFileObjects.forSourceLines(
            "WhazzupGreeting",
            "package test;",
            "import org.springframework.stereotype.Component;",
            "",
            "@Component",
            "public class WhazzupGreeting {",
            "   @Override",
            "    public String getGreeting() {",
            "       return \"Whazzup\";",
            "    }",
            "}");

    public static final JavaFileObject SpinnakerExtension = JavaFileObjects.forSourceLines(
        "SpinnakerExtension",
        "package test;",
        "",
        "import org.pf4j.Extension;",
        "import java.lang.annotation.Documented;",
        "import java.lang.annotation.ElementType;",
        "import java.lang.annotation.Retention;",
        "import java.lang.annotation.RetentionPolicy;",
        "import java.lang.annotation.Target;",
        "",
        "@Extension",
        "@Retention(RetentionPolicy.RUNTIME)",
        "@Target(ElementType.TYPE)",
        "@Documented",
        "public @interface SpinnakerExtension {",
        "}");

    public static final JavaFileObject WhazzupGreeting_SpinnakerExtension =
        JavaFileObjects.forSourceLines(
            "WhazzupGreeting",
            "package test;",
            "",
            "@SpinnakerExtension",
            "public class WhazzupGreeting implements Greeting {",
            "   @Override",
            "    public String getGreeting() {",
            "       return \"Whazzup\";",
            "    }",
            "}");

    /**
     * The same like {@link #SpinnakerExtension} but without {@code Extension} annotation.
     */
    public static final JavaFileObject SpinnakerExtension_NoExtension =
        JavaFileObjects.forSourceLines(
            "SpinnakerExtension",
            "package test;",
            "",
            "import org.pf4j.Extension;",
            "import java.lang.annotation.Documented;",
            "import java.lang.annotation.ElementType;",
            "import java.lang.annotation.Retention;",
            "import java.lang.annotation.RetentionPolicy;",
            "import java.lang.annotation.Target;",
            "",
//        "@Extension",
            "@Retention(RetentionPolicy.RUNTIME)",
            "@Target(ElementType.TYPE)",
            "@Documented",
            "public @interface SpinnakerExtension {",
            "}");

    @Test
    public void getSupportedAnnotationTypes() {
        PluggableAnnotationProcessor instance = new PluggableAnnotationProcessor();
        Set<String> result = instance.getSupportedAnnotationTypes();
        assertEquals(1, result.size());
        assertEquals("*", result.iterator().next());
    }

    @Test
    public void getSupportedOptions() {
        PluggableAnnotationProcessor instance = new PluggableAnnotationProcessor();
        Set<String> result = instance.getSupportedOptions();
        assertEquals(2, result.size());
    }

    @Test
    public void options() {
        PluggableAnnotationProcessor processor = new PluggableAnnotationProcessor();
        Compilation compilation = javac().withProcessors(processor).withOptions("-Ab=2", "-Ac=3")
            .compile(Greeting, WhazzupGreeting);
        assertEquals(compilation.status(), Compilation.Status.SUCCESS);
        Map<String, String> options = new HashMap<>();
        options.put("b", "2");
        options.put("c", "3");
        assertEquals(options, processor.getProcessingEnvironment().getOptions());
    }

    @Test
    public void storage() {
        PluggableAnnotationProcessor processor = new PluggableAnnotationProcessor();
        Compilation compilation =
            javac().withProcessors(processor).compile(Greeting, WhazzupGreeting);
        assertEquals(compilation.status(), Compilation.Status.SUCCESS);
        assertEquals(processor.getStorage().getClass(), SpringComponentStorage.class);
    }

    @Test
    public void compileWithoutError() {
        PluggableAnnotationProcessor processor = new PluggableAnnotationProcessor();
        Compilation compilation =
            javac().withProcessors(processor).compile(Greeting, WhazzupGreeting);
        assertThat(compilation).succeededWithoutWarnings();
    }

    @Test
    public void getExtensions() {
        PluggableAnnotationProcessor processor = new PluggableAnnotationProcessor();
        Compilation compilation =
            javac().withProcessors(processor).compile(Greeting, WhazzupGreeting);
        assertThat(compilation).succeededWithoutWarnings();
        Map<String, Set<String>> extensions = new HashMap<>();
        extensions.put("test.Greeting",
            new HashSet<>(Collections.singletonList("test.WhazzupGreeting")));
        assertEquals(extensions, processor.getExtensions());
    }

    @Test
    public void compileNestedExtensionAnnotation() {
        PluggableAnnotationProcessor processor = new PluggableAnnotationProcessor();
        Compilation compilation = javac().withProcessors(processor)
            .compile(Greeting, SpinnakerExtension, WhazzupGreeting_SpinnakerExtension);
        assertThat(compilation).succeededWithoutWarnings();
        Map<String, Set<String>> extensions = new HashMap<>();
        extensions.put("test.Greeting",
            new HashSet<>(Collections.singletonList("test.WhazzupGreeting")));
        assertEquals(extensions, processor.getExtensions());
    }

}
