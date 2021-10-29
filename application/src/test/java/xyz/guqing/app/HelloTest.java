package xyz.guqing.app;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.pf4j.JarPluginManager;
import org.pf4j.PluginManager;

/**
 * @author guqing
 * @since 2021-10-29
 */
public class HelloTest {
    @Test
    public void test() {
        PluginManager pluginManager = new JarPluginManager(); // or "new ZipPluginManager() / new DefaultPluginManager()"

        // start and load all plugins of application
        pluginManager.loadPlugin(Paths.get("/home/guqing/Developer/workspce/halo-plugin/plugins/apples"));
    }
}
