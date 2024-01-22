package io.gitee.zerowsh.actable.emnus;

import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import lombok.Getter;

/**
 * java类型转数据库类型
 *
 * @author zero
 */
public enum JavaTypeTurnColumnTypeEnums {
    /**
     *
     */
    INTEGER("java.lang.Integer,int", ColumnTypeConstants.INT),
    LONG("java.lang.Long,long", ColumnTypeConstants.BIGINT),
    SHORT("java.lang.Short,short", ColumnTypeConstants.SMALLINT),
    BYTE("java.lang.Byte,byte", ColumnTypeConstants.TINYINT),
    CHAR("java.lang.Char,char", ColumnTypeConstants.CHAR, ColumnTypeConstants.NCHAR, ColumnTypeConstants.CHAR),
    BOOLEAN("java.lang.Boolean,boolean", ColumnTypeConstants.BIT),
    STRING("java.lang.String", ColumnTypeConstants.VARCHAR, ColumnTypeConstants.NVARCHAR, ColumnTypeConstants.VARCHAR),
    DATETIME("java.util.Date,java.sql.Timestamp,java.time.LocalDateTime", ColumnTypeConstants.DATETIME, ColumnTypeConstants.DATETIME2, ColumnTypeConstants.DATETIME),
    DATE("java.time.LocalDate", ColumnTypeConstants.DATE),
    BIG_DECIMAL("java.math.BigDecimal", ColumnTypeConstants.DECIMAL, ColumnTypeConstants.NUMERIC, ColumnTypeConstants.DECIMAL),
    DOUBLE("java.lang.Double,double", ColumnTypeConstants.DOUBLE),
    FLOAT("java.lang.Float,float", ColumnTypeConstants.FLOAT);

    @Getter
    private final String javaType;
    @Getter
    private final String mysql;
    @Getter
    private final String sqlServer;
    @Getter
    private final String dm;

    JavaTypeTurnColumnTypeEnums(String javaType,
                                String mysql,
                                String sqlServer,
                                String dm) {
        this.javaType = javaType;
        this.mysql = mysql;
        this.sqlServer = sqlServer;
        this.dm = dm;
    }

    JavaTypeTurnColumnTypeEnums(String javaType, String identical) {
        this.javaType = javaType;
        this.mysql = identical;
        this.sqlServer = identical;
        this.dm = identical;
    }

    public static String getMysqlByValue(String filedType) {
        for (JavaTypeTurnColumnTypeEnums types : JavaTypeTurnColumnTypeEnums.values()) {
            if (types.getJavaType().contains(filedType)) {
                return types.mysql;
            }
        }
        return ColumnTypeConstants.VARCHAR;
    }

    public static String getSqlServerByValue(String filedType) {
        for (JavaTypeTurnColumnTypeEnums types : JavaTypeTurnColumnTypeEnums.values()) {
            if (types.getJavaType().contains(filedType)) {
                return types.sqlServer;
            }
        }
        return ColumnTypeConstants.NVARCHAR;
    }

    public static String getDmByValue(String filedType) {
        for (JavaTypeTurnColumnTypeEnums types : JavaTypeTurnColumnTypeEnums.values()) {
            if (types.getJavaType().contains(filedType)) {
                return types.dm;
            }
        }
        return ColumnTypeConstants.VARCHAR;
    }
}
