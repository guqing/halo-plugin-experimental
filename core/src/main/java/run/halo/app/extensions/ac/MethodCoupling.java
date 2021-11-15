package run.halo.app.extensions.ac;

import java.util.Comparator;

/**
 * @author guqing
 * @since 2021-11-04
 */
public interface MethodCoupling {

    Comparator<MethodCoupling> COMPARATOR = (a, b) -> {
        final int comparison = Method.COMPARATOR.compare(a.getSource(), b.getSource());
        if (comparison == 0) {
            return Method.COMPARATOR.compare(a.getTarget(), b.getTarget());
        }
        return comparison;
    };

    /**
     * Source method
     *
     * @return The source method.
     */
    Method getSource();

    /**
     * Target method
     *
     * @return The target method.
     */
    Method getTarget();

    class Builder {

        Method source;
        Method target;

        public Builder source(Method source) {
            this.source = source;
            return this;
        }

        public Builder target(Method target) {
            this.target = target;
            return this;
        }

        public MethodCoupling build() {
            return new MethodCoupling() {
                @Override
                public Method getSource() {
                    return source;
                }

                @Override
                public Method getTarget() {
                    return target;
                }
            };
        }
    }
}
