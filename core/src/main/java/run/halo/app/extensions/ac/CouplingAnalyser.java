package run.halo.app.extensions.ac;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import run.halo.app.extensions.ac.CouplingFilter.Builder;

/**
 * @author guqing
 * @since 2021-11-04
 */
@Slf4j
public class CouplingAnalyser {
    private static final boolean disable = true;
    private static final CouplingFilter filter = new Builder().setTargetPackage(
        "^(run\\.halo\\.app\\.utils|run\\.halo\\.app\\.mail).*$").build();

    private static final CouplingFilterConfig config = new CouplingFilterConfig.Builder()
        .setInclude(filter).build();

    public static Set<String> analyseClass(String className, byte[] classData) {
        if (disable) {
            return Collections.emptySet();
        }
        UsageCollector collector = new UsageCollector(config);
        new FilteredClassVisitor(className, collector, classData).visit();
        return collector.getMethodCouplings()
            .stream()
            .map(MethodCoupling::getTarget)
            .map(Method::getClassName)
            .collect(Collectors.toSet());
    }

    public static Set<String> analyseClass(String className) {
        UsageCollector collector = new UsageCollector(config);
        try {
            new FilteredClassVisitor(className, collector).visit();
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
        return collector.getMethodCouplings()
            .stream()
            .map(MethodCoupling::getTarget)
            .map(Method::getClassName)
            .collect(Collectors.toSet());
    }
}
