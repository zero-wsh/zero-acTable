package io.gitee.zerowsh.actable.annotation;

import java.lang.annotation.*;

/**
 * 索引排序
 * 一个索引可以有多个字段，并且多个字段的排序值可以不同（DESC|ASC）
 *
 * @author zero
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IndexColumn {
    /**
     * 要建立索引的字段名
     * 该值和实体类属性保持一致
     */
    String value();

    /**
     * true正序 false倒序，默认正序
     */
    boolean asc() default true;

}

