package io.gitee.zerowsh.actable.emnus;

import io.gitee.zerowsh.actable.constant.AcTableConstants;

import java.util.Arrays;
import java.util.List;

/**
 * 数据库列类型
 *
 * @author zero
 */
@SuppressWarnings("all")
public enum ColumnTypeEnums {
    /**
     * mysql和sqlserver都有的类型
     */
    DEFAULT(AcTableConstants.DEFAULT_VALUE),
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

    TIME("time"),
    LONGBLOB("longblob"),
    /**
     * sqlserver才有的
     */
    NVARCHAR("nvarchar"),
    NVARCHAR_MAX("nvarchar(max)"),
    VARCHAR_MAX("varchar(max)"),
    XML("xml"),
    DATETIME2("datetime2"),
    MONEY("money"),
    VARBINARY("varbinary"),
    VARBINARY_MAX("varbinary(max)"),
    NCHAR("nchar");

    private String type;

    ColumnTypeEnums(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /**
     * mysql不存在的类型
     */
    public static final List<ColumnTypeEnums> MYSQL_NOT_EXIST_TYPE = Arrays.asList(NVARCHAR,
            NVARCHAR_MAX, VARCHAR_MAX, XML, DATETIME2, MONEY, NCHAR, VARBINARY, VARBINARY_MAX);

    /**
     * sqlServer不存在的类型
     */
    public static final List<ColumnTypeEnums> SQL_SERVER_NOT_EXIST_TYPE = Arrays.asList(TIME, LONGBLOB);
}
