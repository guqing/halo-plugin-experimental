package xyz.guqing.plugin.apples.controller;

import com.alibaba.fastjson.JSONObject;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.halo.app.extensions.ExtRestController;
import run.halo.app.model.vo.ArchiveYearVO;
import run.halo.app.service.PostService;
import xyz.guqing.plugin.apples.service.AppleService;

/**
 * @author guqing
 * @date 2021-08-06
 */
@ExtRestController
@RequestMapping(value = "/plugins/apples")
public class ApplesController {

    @Autowired
    private PostService postService;
    @Autowired
    private AppleService appleService;

    @RequestMapping(value = "/name")
    public String name() {
        return "Malum: " + appleService.getName();
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