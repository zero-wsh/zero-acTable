package io.gitee.zerowsh.actable.emnus;

import io.gitee.zerowsh.actable.constant.CreateTableConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 数据库类型
 *
 * @author zero
 */
@SuppressWarnings("all")
public enum SqlServerColumnTypeEnums {
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

    SqlServerColumnTypeEnums(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }


    /**
     * 数据库类型转java类型
     */
    private static final Map<String, SqlServerColumnTypeEnums> JAVA_TURN_SQL_SERVER_MAP = new HashMap<String, SqlServerColumnTypeEnums>() {{
        put("java.lang.String", SqlServerColumnTypeEnums.NVARCHAR);
        put("java.lang.Long", SqlServerColumnTypeEnums.BIGINT);
        put("long", SqlServerColumnTypeEnums.BIGINT);
        put("java.lang.Integer", SqlServerColumnTypeEnums.INT);
        put("int", SqlServerColumnTypeEnums.INT);
        put("java.lang.Boolean", SqlServerColumnTypeEnums.BIT);
        put("java.lang.boolean", SqlServerColumnTypeEnums.BIT);
        put("java.util.Date", SqlServerColumnTypeEnums.DATETIME2);
        put("java.sql.Timestamp", SqlServerColumnTypeEnums.DATETIME2);
        put("java.time.LocalDate", SqlServerColumnTypeEnums.DATETIME2);
        put("java.time.LocalDateTime", SqlServerColumnTypeEnums.DATETIME2);
        put("java.math.BigDecimal", SqlServerColumnTypeEnums.NUMERIC);
        put("java.lang.Double", SqlServerColumnTypeEnums.NUMERIC);
        put("double", SqlServerColumnTypeEnums.NUMERIC);
        put("java.lang.Float", SqlServerColumnTypeEnums.FLOAT);
        put("float", SqlServerColumnTypeEnums.FLOAT);
        put("char", SqlServerColumnTypeEnums.NCHAR);
    }};

    /**
     * java类型转数据库类型
     *
     * @param key
     * @return
     */
    public static String getJavaTurnSqlServerValue(String key) {
        SqlServerColumnTypeEnums sqlServerColumnTypeEnums = JAVA_TURN_SQL_SERVER_MAP.get(key);
        return Objects.isNull(sqlServerColumnTypeEnums) ? SqlServerColumnTypeEnums.NVARCHAR.type : sqlServerColumnTypeEnums.type;
    }

    public static SqlServerColumnTypeEnums getByValue(String type) {
        for (SqlServerColumnTypeEnums types : values()) {
            if (Objects.equals(types.getType(), type)) {
                return types;
            }
        }
        return null;
    }
}
