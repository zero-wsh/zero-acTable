package io.gitee.zerowsh.actable.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.constant.SqlConstants;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.emnus.SqlTypeEnums;
import io.gitee.zerowsh.actable.properties.AcTableProperties;
import io.gitee.zerowsh.actable.util.AcTableThreadLocalUtils;
import io.gitee.zerowsh.actable.util.HandlerEntityUtils;
import io.gitee.zerowsh.actable.util.IoUtil;
import io.gitee.zerowsh.actable.util.JdbcUtil;
import io.gitee.zerowsh.actable.util.sql.MysqlAcTableUtils;
import io.gitee.zerowsh.actable.util.sql.SqlServerAcTableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ResourceUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static cn.hutool.core.util.StrUtil.COMMA;
import static io.gitee.zerowsh.actable.constant.AcTableConstants.MYSQL;
import static io.gitee.zerowsh.actable.constant.AcTableConstants.SQL_SERVER;

/**
 * mysql实现
 *
 * @author zero
 */
@Slf4j
public class AcTableService {

    private AcTableService() {
    }

    public AcTableService(DataSource dataSource, AcTableProperties acTableProperties) {
        this.acTable(dataSource, acTableProperties);
    }

    public void acTable(DataSource dataSource, AcTableProperties acTableProperties) {
        if (Objects.isNull(acTableProperties.getModel())
                || Objects.equals(acTableProperties.getModel(), ModelEnums.NONE)) {
            log.info("自动建表不做任何操作！！！");
            return;
        }
        String entityPackage = acTableProperties.getEntityPackage();
        if (StrUtil.isBlank(entityPackage)) {
            log.warn("请设置实体类包路径！！！");
            return;
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            String databaseType = connection.getMetaData().getDatabaseProductName();
            AcTableThreadLocalUtils.setDatabaseType(databaseType);
            if (StrUtil.isBlank(databaseType)) {
                throw new RuntimeException("获取数据库类型失败！！！");
            }
            List<TableInfo> tableInfoList = HandlerEntityUtils.getTableInfoByEntityPackage(acTableProperties);
            if (CollectionUtil.isEmpty(tableInfoList)) {
                log.warn("没有找到@Table标记的类！！！ entityPackage={}", entityPackage);
                return;
            }

            ModelEnums modelEnums = acTableProperties.getModel();
            List<String> executeSqlList = new ArrayList<>();
            handleExecuteSql(connection, modelEnums, tableInfoList, executeSqlList);
            if (CollectionUtil.isNotEmpty(executeSqlList)) {
                log.info(StrUtil.format("执行 [{}] 自动建表。。。", databaseType));
                for (String sql : executeSqlList) {
                    JdbcUtil.executeSql(connection, sql);
                }
                log.info(StrUtil.format("执行 [{}] 自动建表完成！！！", databaseType));
            }
            this.executeScript(connection, acTableProperties.getScript());
            AcTableThreadLocalUtils.remove();
        } catch (Exception e) {
            log.error("执行自动建表异常：", e);
            throw new RuntimeException("执行自动建表异常");
        } finally {
            IoUtil.close(connection);
        }
    }

    /**
     * 处理sql语句
     *
     * @param connection
     * @param modelEnums
     * @param tableInfoList
     * @param executeSqlList
     * @throws SQLException
     */

    public void handleExecuteSql(Connection connection, ModelEnums modelEnums, List<TableInfo> tableInfoList, List<String> executeSqlList) throws SQLException {
        String databaseType = AcTableThreadLocalUtils.getDatabaseType();
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getName();
            if (JdbcUtil.isExist(connection, SqlConstants.getExecuteSql(SqlTypeEnums.EXIST_TABLE), tableName)) {
                /*
                 * 存在--改表
                 */
                List<TableColumnInfo> tableColumnInfoList = JdbcUtil.getTableColumnInfoList(connection, SqlConstants.getExecuteSql(SqlTypeEnums.TABLE_STRUCTURE), tableName);
                List<ConstraintInfo> constraintInfoList = JdbcUtil.getConstraintInfoList(connection, SqlConstants.getExecuteSql(SqlTypeEnums.CONSTRAINT_INFO), tableName);
                switch (databaseType) {
                    case MYSQL:
                        executeSqlList.addAll(MysqlAcTableUtils.getUpdateTableSql(tableInfo,
                                tableColumnInfoList,
                                constraintInfoList,
                                modelEnums));
                        break;
                    case SQL_SERVER:
                        List<ConstraintInfo> defaultInfoList = JdbcUtil.getConstraintInfoList(connection, SqlConstants.getExecuteSql(SqlTypeEnums.DEFAULT_INFO), tableName);
                        executeSqlList.addAll(SqlServerAcTableUtils.getUpdateTableSql(tableInfo,
                                tableColumnInfoList,
                                constraintInfoList,
                                defaultInfoList,
                                modelEnums));
                        break;
                    default:
                        throw new RuntimeException(StrUtil.format("数据库类型不支持 databaseType={}", databaseType));
                }

            } else {
                switch (databaseType) {
                    case MYSQL:
                        executeSqlList.addAll(MysqlAcTableUtils.getCreateTableSql(tableInfo));
                        break;
                    case SQL_SERVER:
                        executeSqlList.addAll(SqlServerAcTableUtils.getCreateTableSql(tableInfo));
                        break;
                    default:
                        throw new RuntimeException(StrUtil.format("数据库类型不支持 databaseType={}", databaseType));
                }
            }
        }
    }

    /**
     * 执行脚本
     *
     * @param connection
     * @param script
     */
    public void executeScript(Connection connection, String script) {
        String databaseType = AcTableThreadLocalUtils.getDatabaseType();
        if (StrUtil.isBlank(script)) {
            return;
        }
        log.info("执行 [{}] 初始化数据。。。", databaseType);
        List<org.springframework.core.io.Resource> list = new ArrayList<>();
        for (String s : script.split(COMMA)) {
            try {
                list.addAll(Arrays.asList(new PathMatchingResourcePatternResolver().getResources(ResourceUtils.CLASSPATH_URL_PREFIX + s)));
            } catch (IOException e) {
                log.warn("[{}] 没找到初始化数据文件 [{}]！！！", databaseType, s);
                return;
            }
        }

        String fileUrl = null;
        try {
            for (org.springframework.core.io.Resource resource : list) {
                File file = resource.getFile();
                if (file.isFile()) {
                    fileUrl = file.getAbsolutePath();
                    String sql = FileUtil.readUtf8String(file);
                    if (StrUtil.isNotBlank(sql)) {
                        JdbcUtil.executeSql(connection, sql);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException(StrUtil.format("初始化数据失败， fileUrl={} message={}", fileUrl, e.getMessage()));
        }
        log.info("执行 [{}] 初始化数据完成！！！", databaseType);
    }
}
