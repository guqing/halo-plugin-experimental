package xyz.guqing.plugin.potatoes;

import org.pf4j.Extension;
import run.halo.app.extensions.extpoint.IHaloPlugin;

/**
 * @author guqing
 * @since 2021-11-04
 */
@Extension
public class HaloPlugin implements IHaloPlugin {

    @Override
    public String getName() {
        return "Potatoes";
    }

    @Override
    public String logo() {
        return null;
    }

    @Override
    public String description() {
        return null;
    }
}
