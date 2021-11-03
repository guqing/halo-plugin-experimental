package xyz.guqing.plugin.apples.service.impl;

import org.pf4j.Extension;
import org.springframework.stereotype.Service;
import xyz.guqing.plugin.apples.service.AppleService;

/**
 * @author guqing
 * @since 2021-11-02
 */
@Extension
public class AppleServiceImpl implements AppleService {

    @Override
    public String getName() {
        return "Red Apple.";
    }
}
