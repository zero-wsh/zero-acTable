package io.gitee.zerowsh.actable.emnus;

/**
 * 数据库列类型
 * 当等于DEFAULT_VALUE是代表根据字段类型转
 *
 * @author zero
 */
public enum JavaTypeTurnColumnTypeEnums {
    /**
     *
     */
    STRING("java.lang.String", ColumnTypeEnums.VARCHAR, ColumnTypeEnums.NVARCHAR),
    LONG("java.lang.Long,long", ColumnTypeEnums.BIGINT),
    INTEGER("java.lang.Integer,int", ColumnTypeEnums.INT),
    BOOLEAN("java.lang.Boolean,boolean", ColumnTypeEnums.BIT),
    DATE("java.util.Date,java.sql.Timestamp,java.time.LocalDate,java.time.LocalDateTime", ColumnTypeEnums.DATETIME, ColumnTypeEnums.DATETIME2),
    BIG_DECIMAL("java.math.BigDecimal,java.lang.Double,double", ColumnTypeEnums.NUMERIC),
    FLOAT("java.lang.Float,float", ColumnTypeEnums.FLOAT),
    CHAR("char", ColumnTypeEnums.CHAR, ColumnTypeEnums.NCHAR);

    private String javaType;
    private ColumnTypeEnums mysql;
    private ColumnTypeEnums sqlServer;

    JavaTypeTurnColumnTypeEnums(String javaType, ColumnTypeEnums mysql, ColumnTypeEnums sqlServer) {
        this.javaType = javaType;
        this.mysql = mysql;
        this.sqlServer = sqlServer;
    }

    JavaTypeTurnColumnTypeEnums(String javaType, ColumnTypeEnums identical) {
        this.javaType = javaType;
        this.mysql = identical;
        this.sqlServer = identical;
    }

    public String getJavaType() {
        return javaType;
    }

    public ColumnTypeEnums getMysql() {
        return mysql;
    }

    public ColumnTypeEnums getSqlServer() {
        return sqlServer;
    }

    public static ColumnTypeEnums getMysqlByValue(String filedType) {
        for (JavaTypeTurnColumnTypeEnums types : JavaTypeTurnColumnTypeEnums.values()) {
            if (types.getJavaType().contains(filedType)) {
                return types.getMysql();
            }
        }
        return ColumnTypeEnums.VARCHAR;
    }

    public static ColumnTypeEnums getSqlServerByValue(String filedType) {
        for (JavaTypeTurnColumnTypeEnums types : JavaTypeTurnColumnTypeEnums.values()) {
            if (types.getJavaType().contains(filedType)) {
                return types.getSqlServer();
            }
        }
        return ColumnTypeEnums.NVARCHAR;
    }
}
