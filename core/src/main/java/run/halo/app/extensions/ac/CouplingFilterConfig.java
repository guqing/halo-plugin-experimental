package run.halo.app.extensions.ac;

import java.util.Optional;

/**
 * @author guqing
 * @since 2021-11-04
 */
public interface CouplingFilterConfig {

    /**
     * An optional {@link CouplingFilter} that will keep all the couplings that are matching. If
     * this is empty, every coupling will be considered a match, therefore selecting all the
     * couplings. Note: This filter rule will be applied in conjunction (AND operation) with {@link
     * #getExclude()} field.
     *
     * @return The {@link CouplingFilter} for inclusions.
     */
    Optional<CouplingFilter> getInclude();

    /**
     * An optional {@link CouplingFilter} that will discard all the couplings that are matching. If
     * this is empty, every coupling will be considered a non-match, therefore selecting all the
     * couplings. Note: This filter rule will be applied in conjunction (AND operation) with {@link
     * #getInclude()} field.
     *
     * @return The {@link CouplingFilter} for exclusions.
     */
    Optional<CouplingFilter> getExclude();

    class Builder {

        CouplingFilter include;
        CouplingFilter exclude;

        public Builder setInclude(CouplingFilter include) {
            this.include = include;
            return this;
        }

        public Builder setExclude(CouplingFilter exclude) {
            this.exclude = exclude;
            return this;
        }

        public CouplingFilterConfig build() {
            return new CouplingFilterConfig() {

                @Override
                public Optional<CouplingFilter> getInclude() {
                    return Optional.ofNullable(include);
                }

                @Override
                public Optional<CouplingFilter> getExclude() {
                    return Optional.ofNullable(exclude);
                }
            };
        }
    }
}