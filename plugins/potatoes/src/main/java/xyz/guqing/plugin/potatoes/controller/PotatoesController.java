package xyz.guqing.plugin.potatoes.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import run.halo.app.extensions.annotation.ExtRestController;
import xyz.guqing.plugin.potatoes.entity.Potato;
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
    @Autowired
    ApplicationContext applicationContext;

    @GetMapping("/name")
    public Potato name() {
        return potatoService.getById(1);
    }

    @GetMapping("/boom")
    public String boom() {
        return String.valueOf(1 / 0);
    }

    @GetMapping("/beans")
    public String[] beans() {
        return applicationContext.getBeanDefinitionNames();
    }
}
