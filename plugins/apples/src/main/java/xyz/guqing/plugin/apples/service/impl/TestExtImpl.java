package xyz.guqing.plugin.apples.service.impl;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import run.halo.app.extensions.TestExtPoint;
import xyz.guqing.plugin.apples.AppleServiceFactory;
import xyz.guqing.plugin.apples.service.AppleService;

/**
 * @author guqing
 * @since 2.0.0
 */
@Component
public class TestExtImpl implements TestExtPoint, DisposableBean {
    @Override
    public String getName() {
        return "hello world---->" + AppleServiceFactory.getInstance().getName();
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("TestExtImpl被销毁....");
    }
}
