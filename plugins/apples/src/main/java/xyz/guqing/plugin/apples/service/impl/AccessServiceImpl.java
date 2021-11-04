package xyz.guqing.plugin.apples.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.halo.app.mail.MailService;
import run.halo.app.utils.HaloUtils;
import xyz.guqing.plugin.apples.service.AccessService;

/**
 * @author guqing
 * @since 2021-11-04
 */
@Service
public class AccessServiceImpl implements AccessService {

    @Autowired
    private MailService mailService;

    @Override
    public String sayHello() {
        mailService.testConnection();
        normalizeUrl();
        return "你无法使用我";
    }

    private String normalizeUrl() {
        return HaloUtils.normalizeUrl("https://guqing.xyz/");
    }
}
