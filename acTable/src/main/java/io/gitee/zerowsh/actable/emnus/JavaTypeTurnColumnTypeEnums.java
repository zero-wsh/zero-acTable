package io.gitee.zerowsh.actable.emnus;

/**
 * java类型转数据库类型
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
    BIG_DECIMAL("java.math.BigDecimal", ColumnTypeEnums.DECIMAL, ColumnTypeEnums.NUMERIC),
    DOUBLE("java.lang.Double,double", ColumnTypeEnums.DOUBLE),
    FLOAT("java.lang.Float,float", ColumnTypeEnums.FLOAT),
    CHAR("char", ColumnTypeEnums.CHAR, ColumnTypeEnums.NCHAR);

    private final String javaType;
    private final ColumnTypeEnums mysql;
    private final ColumnTypeEnums sqlServer;

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

    public static String getMysqlByValue(String filedType) {
        for (JavaTypeTurnColumnTypeEnums types : JavaTypeTurnColumnTypeEnums.values()) {
            if (types.getJavaType().contains(filedType)) {
                return types.getMysql().getMysql();
            }
        }
        return ColumnTypeEnums.VARCHAR.getMysql();
    }

    public static String getSqlServerByValue(String filedType) {
        for (JavaTypeTurnColumnTypeEnums types : JavaTypeTurnColumnTypeEnums.values()) {
            if (types.getJavaType().contains(filedType)) {
                return types.getSqlServer().getSqlServer();
            }
        }
        return ColumnTypeEnums.NVARCHAR.getSqlServer();
    }
}
