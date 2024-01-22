package io.gitee.zerowsh.actable.emnus;

/**
 * 建表支持的模式
 *
 * @author zero
 */
public enum ModelEnums {
    /**
     * 啥也不做
     */
    NONE,
    /**
     * 只会新增、修改表结构
     * 注意：不会删除多余的字段
     */
    ADD_OR_UPDATE,
    /**
     * 表结构和实体类保持一致
     * 注意：可能会删除表中字段
     */
    ADD_OR_UPDATE_OR_DEL,
    /**
     * 先删除表（只删除实体类对应表），再新增
     * 注意：数据备份
     */
    DEL_AND_ADD,
    /**
     * 先删除库中所有表，再新增
     * 注意：数据备份
     */
    DEL_ALL_AND_ADD
}
