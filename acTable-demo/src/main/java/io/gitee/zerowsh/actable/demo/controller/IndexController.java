package io.gitee.zerowsh.actable.demo.controller;

import io.gitee.zerowsh.actable.demo.entity.ZeroEntity;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "首页模块")
@RestController
public class IndexController {

    @ApiOperation(value = "向客人问好")
    @PostMapping("/sayHi")
    public ResponseEntity<String> sayHi(@RequestBody ZeroEntity zeroEntity) {
        return ResponseEntity.ok("Hi:");
    }
}
