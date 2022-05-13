package xyz.guqing.plugin.apples.controller;

import com.alibaba.fastjson.JSONObject;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.halo.app.extensions.annotation.ExtRestController;
import run.halo.app.model.vo.ArchiveYearVO;
import run.halo.app.service.PostService;
import xyz.guqing.plugin.apples.service.AppleService;

/**
 * @author guqing
 * @date 2021-08-06
 */
@RestController
@RequestMapping(value = "/plugins/apples")
public class ApplesController {

    @Autowired
    private AppleService appleService;
    @Autowired
    private PostService postService;

    @RequestMapping(value = "/name")
    public String name() {
        return "Malum: " + appleService.getName();
    }

    @GetMapping("version")
    public String version() {
        return "0.0.3";
    }

    @GetMapping("/posts")
    public List<ArchiveYearVO> archives() {
        return postService.listYearArchives();
    }

    @GetMapping("json")
    public String json() {
        return JSONObject.toJSONString(postService.listMonthArchives());
    }
}
