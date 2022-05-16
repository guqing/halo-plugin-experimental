package run.halo.pluggable.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;

/**
 * It's a storage (database) that persists components of spring framework.
 * The standard operations supported by storage are {@link #read} and {@link #write}.
 * The storage is populated by {@link PluggableAnnotationProcessor}.
 *
 * @author guqing
 */
public abstract class ComponentStorage {

    private static final Pattern COMMENT = Pattern.compile("#.*");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    protected final PluggableAnnotationProcessor processor;

    public ComponentStorage(PluggableAnnotationProcessor processor) {
        this.processor = processor;
    }

    public abstract Set<String> read();

    public abstract void write(Set<String> extensions);

    /**
     * Helper method.
     */
    protected Filer getFiler() {
        return processor.getProcessingEnvironment().getFiler();
    }

    /**
     * Helper method.
     */
    protected void error(String message, Object... args) {
        processor.error(message, args);
    }

    /**
     * Helper method.
     */
    protected void error(Element element, String message, Object... args) {
        processor.error(element, message, args);
    }

    /**
     * Helper method.
     */
    protected void info(String message, Object... args) {
        processor.info(message, args);
    }

    /**
     * Helper method.
     */
    protected void info(Element element, String message, Object... args) {
        processor.info(element, message, args);
    }

    public static void read(Reader reader, Set<String> entries) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = COMMENT.matcher(line).replaceFirst("");
                line = WHITESPACE.matcher(line).replaceAll("");
                if (line.length() > 0) {
                    entries.add(line);
                }
            }
        }
    }
}
