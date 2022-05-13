package xyz.guqing.plugin.apples;

import xyz.guqing.plugin.apples.service.AppleService;
import xyz.guqing.plugin.apples.service.impl.AppleServiceImpl;

/**
 * @author guqing
 * @since 2.0.0
 */
public class AppleServiceFactory {
    private static final AppleService appleService = new AppleServiceImpl();

    public static AppleService getInstance() {
        return appleService;
    }
}
