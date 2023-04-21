package io.gitee.zerowsh.actable.properties;

import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.emnus.TurnEnums;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置类
 *
 * @author zero
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zero.ac-table")
public class AcTableProperties {
    /**
     * 实体类的包名,多个用逗号隔开
     */
    private String entityPackage;
    /**
     * 建表支持的模式；默认什么也不做
     */
    private ModelEnums model = ModelEnums.NONE;
    /**
     * java转数据库的方式，全局配置，可以通过@AcTable turn属性配置异类；默认驼峰
     */
    private TurnEnums turn = TurnEnums.DEFAULT;
    /**
     * 建表之前脚本
     */
    private String beforeScript;
    /**
     * 建表之后脚本
     */
    private String afterScript;

}
