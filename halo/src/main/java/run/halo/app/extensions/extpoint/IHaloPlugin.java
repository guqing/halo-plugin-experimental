package run.halo.app.extensions.extpoint;

/**
 * @author guqing
 * @since 2021-11-04
 */
public interface IHaloPlugin {

    /**
     * plugin name.
     *
     * @return a plugin name
     */
    String getName();

    /**
     * plugin logo.
     *
     * @return plugin logo url.
     */
    String logo();

    /**
     * plugin description.
     *
     * @return plugin description.
     */
    String description();
}
