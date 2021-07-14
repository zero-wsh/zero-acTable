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
public enum MysqlColumnTypeEnums {
    DEFAULT(CreateTableConstants.DEFAULT_VALUE),
    VARCHAR("varchar"),
    TEXT("text"),
    BIGINT("bigint"),
    INT("int"),
    BIT("bit"),
    TIME("time"),
    DATETIME("datetime"),
    DATE("date"),
    CHAR("char"),
    FLOAT("float"),
    DECIMAL("decimal"),
    TINYINT("tinyint"),
    NUMERIC("numeric");

    private String type;

    MysqlColumnTypeEnums(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }


    private static final Map<String, MysqlColumnTypeEnums> JAVA_TURN_MYSQL_MAP = new HashMap<String, MysqlColumnTypeEnums>() {{
        put("java.lang.String", MysqlColumnTypeEnums.VARCHAR);
        put("java.lang.Long", MysqlColumnTypeEnums.BIGINT);
        put("long", MysqlColumnTypeEnums.BIGINT);
        put("java.lang.Integer", MysqlColumnTypeEnums.INT);
        put("int", MysqlColumnTypeEnums.INT);
        put("java.lang.Boolean", MysqlColumnTypeEnums.BIT);
        put("java.lang.boolean", MysqlColumnTypeEnums.BIT);
        put("java.util.Date", MysqlColumnTypeEnums.DATETIME);
        put("java.sql.Timestamp", MysqlColumnTypeEnums.DATETIME);
        put("java.time.LocalDate", MysqlColumnTypeEnums.DATETIME);
        put("java.time.LocalDateTime", MysqlColumnTypeEnums.DATETIME);
        put("java.math.BigDecimal", MysqlColumnTypeEnums.NUMERIC);
        put("java.lang.Double", MysqlColumnTypeEnums.NUMERIC);
        put("double", MysqlColumnTypeEnums.NUMERIC);
        put("java.lang.Float", MysqlColumnTypeEnums.FLOAT);
        put("float", MysqlColumnTypeEnums.FLOAT);
        put("char", MysqlColumnTypeEnums.CHAR);
    }};

    /**
     * java类型转数据库类型
     *
     * @param key
     * @return
     */
    public static String getJavaTurnMysqlValue(String key) {
        MysqlColumnTypeEnums mysqlColumnTypeEnums = JAVA_TURN_MYSQL_MAP.get(key);
        return Objects.isNull(mysqlColumnTypeEnums) ? MysqlColumnTypeEnums.VARCHAR.type : mysqlColumnTypeEnums.type;
    }

    public static MysqlColumnTypeEnums getByValue(String type) {
        for (MysqlColumnTypeEnums types : values()) {
            if (Objects.equals(types.getType(), type)) {
                return types;
            }
        }
        return null;
    }
}
