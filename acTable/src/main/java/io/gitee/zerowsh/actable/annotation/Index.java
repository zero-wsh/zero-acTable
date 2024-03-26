package io.gitee.zerowsh.actable.annotation;

import lombok.Getter;

import java.lang.annotation.*;


/**
 * 索引定义
 *
 * @author zero
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Index {
    /**
     * 索引、唯一索引、唯一约束
     * 同一个字段只需要（UK_IDX、UK）二选一
     */
    enum IndexEnums {
        IDX("idx_"), UK_IDX("uk_idx_"), UK("uk_");
        @Getter
        private final String prefix;

        IndexEnums(String prefix) {
            this.prefix = prefix;
        }
    }

    /**
     * 索引的名称，不管是否指定都会拼接雪花算法唯一ID，及value+雪花算法ID
     * 索引默认前缀idx_
     * 唯一索引默认前缀uk_idx
     * 唯一约束默认前缀uk_
     */
    String value() default "";

    /**
     * 类型
     */
    IndexEnums type() default IndexEnums.IDX;

    /**
     * 索引信息，索引字段和排序
     * 一个索引可以有多个字段，并且多个字段的排序值可以不同（DESC|ASC）
     */
    IndexColumn[] columnArr();

    /**
     * 冲突时提示信息，结和业务代码使用
     *
     * @return
     */
    String message() default "";

//    /**
//     * 索引备注
//     */
//    String comment() default AcTableConstants.DEFAULT_VALUE;
//
//    /**
//     * 索引方法
//     */
//    String method() default AcTableConstants.DEFAULT_VALUE;

}

