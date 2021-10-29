package xyz.guqing.plugin.apples;

import org.pf4j.PluginWrapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import xyz.guqing.plugin.core.SpringBootPlugin;
import xyz.guqing.plugin.core.boot.SpringBootstrap;

/**
 * @author guqing
 * @date 2021-08-06
 */
public class ApplesPlugin extends SpringBootPlugin {

    public ApplesPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected SpringBootstrap createSpringBootstrap() {
        return new SpringBootstrap(this, ApplesPluginStarter.class);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @SpringBootApplication
    public static class ApplesPluginStarter {

        public static void main(String[] args) {
            SpringApplication.run(ApplesPluginStarter.class, args);
        }
    }
}