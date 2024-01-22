package io.gitee.zerowsh.actable.emnus;

/**
 * java转数据库的方式
 *
 * @author zero
 */
public enum TurnEnums {
    /**
     * 默认，通过默认值来保证就近原则（字段注解属性高于类注解属性）
     */
    DEFAULT,

    /**
     * 驼峰
     */
    DUMP,
    /**
     * 保持不变
     * 注意：如果是这个枚举值，并且io.gitee.zerowsh.actable.properties.AcTableProperties.columnToUpperCase=true，
     * 如果没指定@AcColumn的字段名，会导致转成大写时字段不好阅读，所以请指定@AcColumn的字段名
     */
    SOURCE
}
