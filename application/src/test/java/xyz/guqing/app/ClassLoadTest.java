package xyz.guqing.app;

import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.pf4j.JarPluginManager;
import org.pf4j.Plugin;
import org.pf4j.PluginManager;
import xyz.guqing.plugin.core.internal.SpringBootPluginClassLoader;

/**
 * @author guqing
 * @since 2021-10-28
 */
public class ClassLoadTest {
    @Test
    public void test() {
        PluginManager pluginManager = new JarPluginManager();
        pluginManager.loadPlugin(Paths.get("/home/guqing/Developer/plugins/plugin1-0.0.1.jar"));
        pluginManager.startPlugins();
    }

    @Test
    public void test2() {
        int modifiers = 1;
        if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
            System.out.println("helo");
        }
    }
}
