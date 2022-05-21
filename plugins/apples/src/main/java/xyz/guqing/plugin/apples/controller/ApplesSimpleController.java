package xyz.guqing.plugin.apples.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author guqing
 * @since 2.0.0
 */
@RestController
@RequestMapping(value = "/plugins/simple/apples")
public class ApplesSimpleController {

    @GetMapping
    public String name() {
        return "hello some-apples";
    }
}
