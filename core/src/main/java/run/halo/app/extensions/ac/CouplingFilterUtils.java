package run.halo.app.extensions.ac;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Class file coupling filter utils.
 *
 * @author guqing
 * @date 2021-11-04
 * @see <a href="https://github.com/ExpediaGroup/jarviz">jarviz</a>
 */
public final class CouplingFilterUtils {

    private CouplingFilterUtils() {
    }

    /**
     * Applying a {@link CouplingFilterConfig} against a given coupling. Include and exclude filter
     * rules will be applied in conjunction (AND operation for Includes, OR for Excludes). If the
     * return value is true, coupling should be retained or discarded otherwise.
     *
     * @param couplingFilterConfig The filters.
     * @param coupling             The method coupling.
     * @return Indicates whether the given coupling should be kept (true) or discarded (false).
     */
    public static boolean filterMethodCoupling(final CouplingFilterConfig couplingFilterConfig,
        final MethodCoupling coupling) {
        return
            couplingFilterConfig.getInclude()
                .map(f -> matchIncludeCoupling(f, coupling))
                .orElse(true)
                &&
                !couplingFilterConfig.getExclude()
                    .map(f -> matchExcludeCoupling(f, coupling))
                    .orElse(false);
    }

    /**
     * Matches a given coupling to an included RegEx backed {@link CouplingFilter}.
     *
     * @param filter   The filter.
     * @param coupling The coupling.
     * @return Indicates whether the given coupling should be kept (true) or discarded (false).
     */
    static boolean matchIncludeCoupling(final CouplingFilter filter,
        final MethodCoupling coupling) {
        return matchString(filter.getSourcePackagePattern(), coupling.getSource().getPackageName(),
            true) &&
            matchString(filter.getSourceClassPattern(), coupling.getSource().getSimpleClassName(),
                true) &&
            matchString(filter.getSourceMethodPattern(), coupling.getSource().getMethodName(), true)
            &&
            matchString(filter.getTargetPackagePattern(), coupling.getTarget().getPackageName(),
                true) &&
            matchString(filter.getTargetClassPattern(), coupling.getTarget().getSimpleClassName(),
                true) &&
            matchString(filter.getTargetMethodPattern(), coupling.getTarget().getMethodName(),
                true);
    }

    /**
     * Matches a given coupling to an excluded RegEx backed {@link CouplingFilter}.
     *
     * @param filter   The filter.
     * @param coupling The coupling.
     * @return Indicates whether the given coupling should be excluded (true) or kept (false).
     */
    static boolean matchExcludeCoupling(final CouplingFilter filter,
        final MethodCoupling coupling) {
        return matchString(filter.getSourcePackagePattern(), coupling.getSource().getPackageName(),
            false) ||
            matchString(filter.getSourceClassPattern(), coupling.getSource().getSimpleClassName(),
                false) ||
            matchString(filter.getSourceMethodPattern(), coupling.getSource().getMethodName(),
                false) ||
            matchString(filter.getTargetPackagePattern(), coupling.getTarget().getPackageName(),
                false) ||
            matchString(filter.getTargetClassPattern(), coupling.getTarget().getSimpleClassName(),
                false) ||
            matchString(filter.getTargetMethodPattern(), coupling.getTarget().getMethodName(),
                false);
    }

    /**
     * Matches a given string to an optional RegEx pattern. If pattern is missing, provided default
     * value is returned.
     *
     * @param pattern      The RegEx pattern
     * @param string       The string to match to the given pattern.
     * @param defaultValue The default value to return when there are no matches.
     * @return Indicates whether the given coupling matches the pattern, else return provided
     * default value.
     */
    static boolean matchString(Pattern pattern, final String string,
        final boolean defaultValue) {
        return Optional.ofNullable(pattern)
            .map(p -> p.matcher(string).matches())
            .orElse(defaultValue);
    }
}