package io.gitee.zerowsh.actable.emnus;

/**
 * 脚本执行历史记录情况
 *
 * @author zero
 */
public enum HistoryEnums {
    /**
     * 不会创建历史表，需要保证可重复执行sql脚本
     */
    NONE,
    /**
     * 会创建历史表，并且允许重复执行sql脚本
     */
    REPEAT,
    /**
     * 会创建历史表，并且不允许重复执行sql脚本
     */
    NOT_REPEAT
}
