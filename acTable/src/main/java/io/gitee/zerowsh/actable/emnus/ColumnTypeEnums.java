package io.gitee.zerowsh.actable.emnus;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.DEFAULT_VALUE;

/**
 * 数据库列类型
 * 当等于DEFAULT_VALUE是代表根据字段类型转
 *
 * @author zero
 */
@SuppressWarnings("all")
@Slf4j
public enum ColumnTypeEnums {
    /**
     * mysql和sqlserver都有的类型
     */
    DEFAULT(DEFAULT_VALUE),
    VARCHAR("varchar"),
    TEXT("text"),
    BIGINT("bigint"),
    INT("int"),
    BIT("bit"),
    DATETIME("datetime"),
    DATE("date"),
    CHAR("char"),
    FLOAT("float"),
    DECIMAL("decimal"),
    TINYINT("tinyint"),
    NUMERIC("numeric"),
    /**
     * mysql才有的
     */
    TIME("time", null),
    LONGBLOB("longblob", null),
    /**
     * sqlserver才有的
     */
    NVARCHAR(null, "nvarchar"),
    NVARCHAR_MAX(null, "nvarchar(max)"),
    VARCHAR_MAX(null, "varchar(max)"),
    XML(null, "xml"),
    DATETIME2(null, "datetime2"),
    MONEY(null, "money"),
    VARBINARY(null, "varbinary"),
    VARBINARY_MAX(null, "varbinary(max)"),
    NCHAR(null, "nchar");

    private String mysql;
    private String sqlServer;

    ColumnTypeEnums(String mysql, String sqlServer) {
        this.mysql = mysql;
        this.sqlServer = sqlServer;
    }

    /**
     * 类型相同
     *
     * @param identical
     */
    ColumnTypeEnums(String identical) {
        this.mysql = identical;
        this.sqlServer = identical;
    }

    public String getMysql() {
        return StrUtil.isBlank(mysql) ? VARCHAR.getMysql() : mysql;
    }

    public String getSqlServer() {
        return StrUtil.isBlank(sqlServer) ? NVARCHAR.getSqlServer() : sqlServer;
    }
}
