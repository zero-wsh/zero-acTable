package io.gitee.zerowsh.actable.demo.controller;

import cn.hutool.core.util.RuntimeUtil;
import io.gitee.zerowsh.actable.demo.service.IZeroService;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Api(tags = "测试")
@RequestMapping("/zero")
public class ZeroController {
    @Resource
    private IZeroService zeroService;

    @GetMapping("execForLines")
    public Object execForLines(String command) {
        return RuntimeUtil.execForLines("sh", "-c",command);
    }
    @GetMapping("execForStr")
    public Object execForStr(String command) {
        return RuntimeUtil.execForStr("sh", "-c",command);
    }
}
