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
    private String constraintType;
    private String constraintColumnName;
    /**
     * 1 主键
     * 2 唯一键
     * 3 索引
     * 4 默认值 （基本没用）
     */
    private Integer constraintFlag;
}
