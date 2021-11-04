package xyz.guqing.plugin.potatoes;

import java.util.stream.Collectors;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import run.halo.app.extensions.SpringPlugin;
import run.halo.app.extensions.extpoint.IHaloPlugin;
import run.halo.app.model.entity.Post;
import run.halo.app.service.PostService;

/**
 * @author guqing
 * @since 2021-11-04
 */
@Extension
public class PotatoesApp extends SpringPlugin implements IHaloPlugin {

    @Autowired
    private PostService postService;

    public PotatoesApp(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getName() {
        return "Potatoes";
    }

    @Override
    public String saySomething() {
        return "I wrote these articles: " + postService.listAll()
            .stream()
            .map(Post::getTitle)
            .collect(Collectors.joining(","));
    }
}
