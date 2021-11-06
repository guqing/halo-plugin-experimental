package xyz.guqing.plugin.potatoes;

import java.util.stream.Collectors;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import run.halo.app.extensions.extpoint.IHaloPlugin;
import run.halo.app.model.entity.Post;
import run.halo.app.service.PostService;

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
