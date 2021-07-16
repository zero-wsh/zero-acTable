package io.gitee.zerowsh.actable.util;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import io.gitee.zerowsh.actable.util.sql.MysqlAcTableUtils;
import io.gitee.zerowsh.actable.util.sql.SqlServerAcTableUtils;
import lombok.extern.slf4j.Slf4j;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.MYSQL;
import static io.gitee.zerowsh.actable.constant.AcTableConstants.SQL_SERVER;

/**
 * 所有数据库共用工具类
 *
 * @author zero
 */
@Slf4j
public class AcTableUtils {
    /**
     * 处理关键字
     *
     * @param var
     * @return
     */
    public static String handleKeyword(String var) {
        String databaseType = AcTableThreadLocalUtils.getDatabaseType();
        switch (databaseType) {
            case MYSQL:
                if (var.startsWith(StringPool.BACKTICK) && var.endsWith(StringPool.BACKTICK)) {
                    var = var.replace(StringPool.BACKTICK, "");
                }
                break;
            case SQL_SERVER:
                if (var.startsWith(StringPool.LEFT_SQ_BRACKET) && var.endsWith(StringPool.RIGHT_SQ_BRACKET)) {
                    var = var.replace(StringPool.LEFT_SQ_BRACKET, "")
                            .replace(StringPool.RIGHT_SQ_BRACKET, "");
                }
                break;
            default:
        }
        return var;
    }

    /**
     * 处理类型
     *
     * @param var
     * @return
     */
    public static String handleType(String var) {
        String databaseType = AcTableThreadLocalUtils.getDatabaseType();
        switch (databaseType) {
            case SQL_SERVER:
                var = SqlServerAcTableUtils.getJavaTurnSqlServerValue(var);
                break;
            case MYSQL:
                var = MysqlAcTableUtils.getJavaTurnMysqlValue(var);
                break;
            default:
        }
        return var;
    }


    /**
     * 处理字符串长度
     *
     * @param length
     * @return
     */
    public static int handleStrLength(int length) {
        return length < 0 ? 255 : length;
    }

    /**
     * 处理时间长度
     *
     * @param length
     * @return
     */
    public static int handleDateLength(int length) {
        return length > 7 || length < 0 ? 0 : length;
    }

}
