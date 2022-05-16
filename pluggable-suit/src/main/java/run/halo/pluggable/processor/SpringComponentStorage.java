package run.halo.pluggable.processor;

import java.util.Map;
import java.util.Set;

public class SpringComponentStorage extends ComponentStorage {

    public SpringComponentStorage(PluggableAnnotationProcessor processor) {
        super(processor);
    }

    @Override
    public Map<String, Set<String>> read() {
        return null;
    }

    @Override
    public void write(Map<String, Set<String>> extensions) {

    }
}
