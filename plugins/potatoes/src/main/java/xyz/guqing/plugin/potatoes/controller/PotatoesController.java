package xyz.guqing.plugin.potatoes.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import run.halo.app.extensions.annotation.ExtRestController;

/**
 * @author guqing
 * @since 2021-11-04
 */
@ExtRestController
@RequestMapping(value = "/plugins/potatoes")
public class PotatoesController {

    @GetMapping("/name")
    public String name() {
        return "Lycopersicon esculentum";
    }

    @GetMapping("/boom")
    public String boom() {
        return String.valueOf(1 / 0);
    }
}
