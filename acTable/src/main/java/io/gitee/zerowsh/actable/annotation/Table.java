package io.gitee.zerowsh.actable.annotation;

import io.gitee.zerowsh.actable.constant.AcTableConstants;
import io.gitee.zerowsh.actable.emnus.TurnEnums;

import java.lang.annotation.*;


/**
 * 标记表的注解
 *
 * @author zero
 */
//表示注解加在接口、类、枚举等
@Target(ElementType.TYPE)
//VM将在运行期也保留注释，因此可以通过反射机制读取注解的信息
@Retention(RetentionPolicy.RUNTIME)
//将此注解包含在javadoc中
@Documented
public @interface Table {

    /**
     * 表名
     * 1.当@Table name和mybatis plus @TableName同时存在时，优先使用@TableName的value
     * 2.两个注解都不存在或者设置的值都无效时，使用字段配合turn进行转换
     */
    String name() default "";


    /**
     * 表注释
     *
     * @return 表注释
     */
    String comment() default AcTableConstants.DEFAULT_VALUE;


    /**
     * 当字段没有标记@Column注解时，java转数据库的方式
     *
     * @return boolean
     */
    TurnEnums turn() default TurnEnums.DEFAULT;
}
