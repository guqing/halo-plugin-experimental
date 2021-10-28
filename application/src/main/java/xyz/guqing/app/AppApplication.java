package xyz.guqing.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import xyz.guqing.plugin.core.utils.ApplicationContextProvider;

/**
 * @author guqing
 * @since 2021-10-27
 */
@SpringBootApplication
public class AppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

    @Bean
    public ApplicationContextAware multiApplicationContextProviderRegister() {
        return ApplicationContextProvider::registerApplicationContext;
    }
}
