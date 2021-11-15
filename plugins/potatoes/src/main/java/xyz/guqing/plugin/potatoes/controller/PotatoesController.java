package xyz.guqing.plugin.potatoes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import run.halo.app.extensions.annotation.ExtRestController;
import xyz.guqing.plugin.potatoes.service.PotatoService;

/**
 * @author guqing
 * @since 2021-11-04
 */
@ExtRestController
@RequestMapping(value = "/plugins/potatoes")
public class PotatoesController {

    @Autowired
    private PotatoService potatoService;

    @GetMapping("/name")
    public String name() {
        potatoService.create();
        return "Lycopersicon esculentum";
    }

    @GetMapping("/boom")
    public String boom() {
        return String.valueOf(1 / 0);
    }
}
