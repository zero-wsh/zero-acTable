package io.gitee.zerowsh.actable.config;

import lombok.Getter;
import lombok.Setter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.emnus.TurnEnums;

@Component
@ConfigurationProperties(prefix = "zero.mybatis.ac-table")
@Getter
@Setter
@MapperScan("io.gitee.zerowsh.actable.dao")
public class CreateTableConfig {
    /**
     * 实体类的包名,多个用逗号隔开
     */
    private String entityPackage;
    /**
     * 建表支持的模式
     */
    private ModelEnums model;
    /**
     * java转数据库的方式，全局配置，可以通过@Table turn属性配置异类
     */
    private TurnEnums turn = TurnEnums.DEFAULT;

}
