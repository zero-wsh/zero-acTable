package io.gitee.zerowsh.actable.util;

import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.util.sql.MysqlAcTableUtils;
import io.gitee.zerowsh.actable.util.sql.SqlServerAcTableUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.MYSQL;
import static io.gitee.zerowsh.actable.constant.AcTableConstants.SQL_SERVER;
import static io.gitee.zerowsh.actable.constant.StringConstants.*;

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
                if (var.startsWith(BACKTICK) && var.endsWith(BACKTICK)) {
                    var = var.replace(BACKTICK, "");
                }
                break;
            case SQL_SERVER:
                if (var.startsWith(LEFT_SQ_BRACKET) && var.endsWith(RIGHT_SQ_BRACKET)) {
                    var = var.replace(LEFT_SQ_BRACKET, "")
                            .replace(RIGHT_SQ_BRACKET, "");
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

    /**
     * 拆分sql
     *
     * @param sqls
     * @return
     */
    public static List<String> splitSql(String sqls) {
        List<String> resultList = new ArrayList<>();
        //按分号拆分
        String[] split = sqls.split(SEMICOLON);
        String oneSqL = "";
        for (String s : split) {
            oneSqL = oneSqL + s;
            //统计当前sqZ里面单引号的个数(所有个数减去注释个数即正常个数)
            int allCount = StrUtil.count(oneSqL, QUOTATION);
            int noUseCount = StrUtil.count(oneSqL, BACK_SLASH + QUOTATION);
            allCount -= noUseCount;
            int allCount1 = StrUtil.count(oneSqL, QUOTE);
            int noUseCount1 = StrUtil.count(oneSqL, BACK_SLASH + QUOTE);
            allCount -= noUseCount;
            allCount1 -= noUseCount1;
            //如果对称代表分号有效
            if (allCount % 2 == 0 || allCount1 % 2 == 0) {
                if (StrUtil.isNotBlank(oneSqL)) {
                    resultList.add(oneSqL.trim());
                }
                //重新初始化
                oneSqL = "";
            }
        }
        return resultList;
    }
}
