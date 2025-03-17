package io.gitee.zerowsh.actable.emnus;

import io.gitee.zerowsh.actable.service.DatabaseService;
import io.gitee.zerowsh.actable.service.impl.DmImpl;
import io.gitee.zerowsh.actable.service.impl.MysqlImpl;
import io.gitee.zerowsh.actable.service.impl.SqlServerImpl;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * 数据库类型
 *
 * @author zero
 */
@Slf4j
@Getter
public enum DatabaseTypeEnums {
    /**
     * 数据库类型
     */
    MYSQL("MySQL", new MysqlImpl()),
    SQL_SERVER("Microsoft SQL Server", new SqlServerImpl()),
    DM("DM DBMS", new DmImpl());

    private final String databaseType;
    private final DatabaseService databaseService;

    DatabaseTypeEnums(String databaseType, DatabaseService databaseService) {
        this.databaseType = databaseType;
        this.databaseService = databaseService;
    }

    public static DatabaseService getDatabaseService(String databaseType) {
        for (DatabaseTypeEnums types : DatabaseTypeEnums.values()) {
            if (Objects.equals(types.getDatabaseType(), databaseType)) {
                return types.getDatabaseService();
            }
        }
        return DatabaseTypeEnums.MYSQL.getDatabaseService();
    }

}
