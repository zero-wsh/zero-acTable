package io.gitee.zerowsh.actable.annotation;

import java.lang.annotation.*;

/**
 * 设置字段索引
 *
 * @author zero
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Index {

    /**
     * 索引的名字，不设置默认为{idx_当前标记字段名@Column的fieldName}<p>
     * 如果设置了名字例如union_name,系统会默认在名字前加idx_前缀，也就是idx_union_name
     */
    String value() default "";

    /**
     * 要建立索引的字段名，不设置默认为当前标记字段名@Column的fieldName
     * <p>可设置多个建立联合索引{"login_mobile","login_name"}
     */
    String[] columns() default {};

}

