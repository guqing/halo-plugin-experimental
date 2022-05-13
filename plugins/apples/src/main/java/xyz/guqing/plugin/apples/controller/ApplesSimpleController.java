package xyz.guqing.plugin.apples.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import run.halo.app.extensions.annotation.ExtRestController;

/**
 * @author guqing
 * @since 2.0.0
 */
@ExtRestController
@RequestMapping(value = "/plugins/simple/apples")
public class ApplesSimpleController {

    @GetMapping
    public String name() {
        return "hello some-apples";
    }
}
