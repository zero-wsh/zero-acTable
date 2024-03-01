package io.gitee.zerowsh.actable.dto;

import lombok.Data;

/**
 * 表约束信息
 *
 * @author zero
 */
@Data
public class ConstraintInfo {
    private String constraintName;
    /**
     * 约束字段拼接，多个按顺序拼接
     */
    private String constraintColumnName;
    /**
     * 1 主键
     * 2 唯一键
     * 3 索引
     * 4 唯一索引
     */
    private Integer constraintFlag;
    /**
     * 索引排序，和字段constraintColumnName对应
     */
    private String indexSortStr;
}
