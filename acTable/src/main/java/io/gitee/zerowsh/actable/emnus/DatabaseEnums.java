package io.gitee.zerowsh.actable.emnus;

import java.util.Objects;

/**
 * 数据库类型
 *
 * @author zero
 */
public enum DatabaseEnums {
    /*
     *
     */
    MYSQL("MySQL", "mysql"),
    SQL_SERVER("Microsoft SQL Server", "sqlserver");

    private String type;
    private String druidType;

    DatabaseEnums(String type, String druidType) {
        this.type = type;
        this.druidType = druidType;
    }

    public String getType() {
        return type;
    }

    public String getDruidType() {
        return druidType;
    }

    public static String getDruidTypeByType(String type) {
        for (DatabaseEnums databaseEnums : DatabaseEnums.values()) {
            if (Objects.equals(databaseEnums.getType(), type)) {
                return databaseEnums.druidType;
            }
        }
        return null;
    }
}
