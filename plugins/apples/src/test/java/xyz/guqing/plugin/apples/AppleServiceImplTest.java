package xyz.guqing.plugin.apples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import run.halo.app.service.PostService;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("plugintest")
public class AppleServiceImplTest {

    @Autowired
    private PostService postService;

    @Test
    public void test() {
        assertThat(postService).isNotNull();
    }
}

@SpringBootApplication
@ComponentScan(basePackages = {"run.halo.app", "xyz.guqing.plugin.apples"})
class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
