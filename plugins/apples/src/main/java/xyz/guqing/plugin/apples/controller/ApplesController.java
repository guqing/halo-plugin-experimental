package xyz.guqing.plugin.apples.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author guqing
 * @date 2021-08-06
 */
@RestController
@RequestMapping(value = "/plugins/apples")
public class ApplesController {

    @RequestMapping(value = "/name")
    public String name() {
        return "Malum";
    }
}