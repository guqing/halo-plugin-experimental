package run.halo.pluggable.processor;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.FilerException;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.pf4j.processor.ExtensionAnnotationProcessor;
import org.pf4j.processor.ExtensionStorage;

public class SpringComponentStorage extends ComponentStorage {

    public static final String EXTENSIONS_RESOURCE = "META-INF/plugin-components.idx";

    public SpringComponentStorage(PluggableAnnotationProcessor processor) {
        super(processor);
    }

    @Override
    public Set<String> read() {
        Set<String> extensions = new LinkedHashSet<>();

        try {
            FileObject file = getFiler()
                .getResource(StandardLocation.CLASS_OUTPUT, "", EXTENSIONS_RESOURCE);
            // TODO try to calculate the extension point
            ExtensionStorage.read(file.openReader(true), extensions);
        } catch (FileNotFoundException | NoSuchFileException e) {
            // doesn't exist, ignore
        } catch (FilerException e) {
            // re-opening the file for reading or after writing is ignorable
        } catch (IOException e) {
            error(e.getMessage());
        }

        return extensions;
    }

    @Override
    public void write(Set<String> extensions) {
        try {
            FileObject file =
                getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", EXTENSIONS_RESOURCE);
            try (BufferedWriter writer = new BufferedWriter(file.openWriter())) {
                writer.write("# Generated by Halo"); // write header
                writer.newLine();
                for (String extension : extensions) {
                    writer.write(extension);
                    writer.newLine();
                }
            }
        } catch (FileNotFoundException e) {
            // it's the first time, create the file
        } catch (FilerException e) {
            // re-opening the file for reading or after writing is ignorable
        } catch (IOException e) {
            error(e.toString());
        }
    }
}
