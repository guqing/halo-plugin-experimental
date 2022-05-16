package run.halo.app.extensions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plugins/resources")
public class PluginResourceController {
    @Autowired
    private SpringPluginManager pluginManager;

    @GetMapping("/{pluginId}")
    public Resource getResource(@PathVariable String pluginId) {
        PluginApplicationContext pluginApplicationContext =
            pluginManager.getPluginApplicationContext(pluginId);
        return pluginApplicationContext.getResource("index.html");
    }
}
