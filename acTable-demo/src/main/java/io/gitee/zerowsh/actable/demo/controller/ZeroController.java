package io.gitee.zerowsh.actable.demo.controller;

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

    @GetMapping("getList")
    public Object getList() {
        return zeroService.list();
    }
}
