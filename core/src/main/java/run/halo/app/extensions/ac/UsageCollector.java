package run.halo.app.extensions.ac;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author guqing
 * @since 2021-11-04
 */
public class UsageCollector implements Collector {

    // This default filter will allow all the method couplings without filtering out anything.
    private static final CouplingFilterConfig DEFAULT_COUPLING_FILTER =
        new CouplingFilterConfig.Builder().build();

    private final Multimap<Method, Method> methodRefMap;

    private final CouplingFilterConfig couplingFilterConfig;

    public UsageCollector() {
        this(DEFAULT_COUPLING_FILTER);
    }

    /**
     * @param couplingFilterConfig A filter to conditionally select the source and target methods
     *                             couplings.
     */
    public UsageCollector(final CouplingFilterConfig couplingFilterConfig) {
        Objects.requireNonNull(couplingFilterConfig, "couplingFilterConfig should not be null");

        this.couplingFilterConfig = couplingFilterConfig;
        this.methodRefMap = LinkedHashMultimap.create();
    }

    private static MethodCoupling mapToMethodCoupling(final Entry<Method, Method> entry) {
        return new MethodCoupling.Builder()
            .source(entry.getKey())
            .target(entry.getValue())
            .build();
    }

    @Override
    public void collectMethodCoupling(final MethodCoupling coupling) {
        if (CouplingFilterUtils.filterMethodCoupling(couplingFilterConfig, coupling)) {
            methodRefMap.put(coupling.getSource(), coupling.getTarget());
        }
    }

    /**
     * Generates the efferent coupling graph for each method in the classes loaded by the class
     * loader.
     *
     * @return The list of method couplings.
     */
    public List<MethodCoupling> getMethodCouplings() {
        return ImmutableList.copyOf(
            methodRefMap.entries().stream()
                .map(UsageCollector::mapToMethodCoupling)
                .sorted(MethodCoupling.COMPARATOR)
                .collect(Collectors.toList())
        );
    }
}
