package io.gitee.zerowsh.actable.emnus;

import io.gitee.zerowsh.actable.constant.CreateTableConstants;

/**
 * 数据库类型
 *
 * @author zero
 */
public enum ColumnTypeEnums {

    DEFAULT(CreateTableConstants.DEFAULT_VALUE),
    NVARCHAR("nvarchar"),
    NVARCHAR_MAX("nvarchar(max)"),
    VARCHAR("varchar"),
    VARCHAR_MAX("varchar(max)"),
    TEXT("text"),
    XML("xml"),
    BIGINT("bigint"),
    INT("int"),
    BIT("bit"),
    DATETIME2("datetime2"),
    DATETIME("datetime"),
    DATE("date"),
    MONEY("money"),
    NCHAR("nchar"),
    CHAR("char"),
    FLOAT("float"),
    DECIMAL("decimal"),
    TINYINT("tinyint"),
    NUMERIC("numeric");

    private String type;

    ColumnTypeEnums(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
