package io.gitee.zerowsh.actable.emnus;

/**
 * 执行sql的类型
 *
 * @author zero
 */
public enum SqlTypeEnums {
    /**
     * 判断是否存在表
     */
    EXIST_TABLE,

    /**
     * 获取表结构
     */
    TABLE_STRUCTURE,
    /**
     * 获取表约束信息
     */
    CONSTRAINT_INFO,
    /**
     * 获取表默认值约束
     */
    DEFAULT_INFO
}
