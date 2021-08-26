package io.gitee.zerowsh.actable.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.emnus.SqlTypeEnums;
import io.gitee.zerowsh.actable.properties.AcTableProperties;
import io.gitee.zerowsh.actable.util.AcTableUtils;
import io.gitee.zerowsh.actable.util.HandlerEntityUtils;
import io.gitee.zerowsh.actable.util.IoUtil;
import io.gitee.zerowsh.actable.util.JdbcUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ResourceUtils;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.hutool.core.util.StrUtil.COMMA;
import static io.gitee.zerowsh.actable.constant.AcTableConstants.SQL_SERVER;
import static io.gitee.zerowsh.actable.constant.StringConstants.CRLF;
import static io.gitee.zerowsh.actable.constant.StringConstants.SQL_SPLIT_STR;

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
            if (StrUtil.isBlank(databaseType)) {
                throw new RuntimeException("获取数据库类型失败！！！");
            }
            List<TableInfo> tableInfoList = HandlerEntityUtils.getTableInfoByEntityPackage(acTableProperties, databaseType);
            if (CollectionUtil.isEmpty(tableInfoList)) {
                log.warn("没有找到@Table标记的类！！！ entityPackage={}", entityPackage);
                return;
            }

            ModelEnums modelEnums = acTableProperties.getModel();
            List<String> executeSqlList = new ArrayList<>();
            handleExecuteSql(connection, modelEnums, tableInfoList, executeSqlList, databaseType);
            if (CollectionUtil.isNotEmpty(executeSqlList)) {
                log.info(StrUtil.format("执行 [{}] 自动建表。。。", databaseType));
                for (String sql : executeSqlList) {
                    JdbcUtil.executeSql(connection, sql);
                }
                log.info(StrUtil.format("执行 [{}] 自动建表完成！！！", databaseType));
            }
            this.executeScript(connection, acTableProperties.getScript(), databaseType);
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

    public void handleExecuteSql(Connection connection, ModelEnums modelEnums, List<TableInfo> tableInfoList, List<String> executeSqlList, String databaseType) throws SQLException {
        DatabaseService databaseService = AcTableUtils.getDatabaseService(databaseType);
        if (Objects.equals(modelEnums, ModelEnums.DEL_AND_ADD)) {
            List<String> tableNameList = JdbcUtil.getTableNameList(connection, databaseService.getExecuteSql(SqlTypeEnums.GET_ALL_TABLE));
            for (String tableName : tableNameList) {
                JdbcUtil.executeSql(connection, databaseService.getExecuteSql(SqlTypeEnums.DROP_TABLE), tableName);
            }
        }
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getName();
            if (!Objects.equals(modelEnums, ModelEnums.DEL_AND_ADD)
                    && JdbcUtil.isExist(connection, databaseService.getExecuteSql(SqlTypeEnums.EXIST_TABLE), tableName)) {
                /*
                 * 存在--改表
                 */
                List<TableColumnInfo> tableColumnInfoList = JdbcUtil.getTableColumnInfoList(connection, databaseService.getExecuteSql(SqlTypeEnums.TABLE_STRUCTURE), tableName);
                List<ConstraintInfo> constraintInfoList = JdbcUtil.getConstraintInfoList(connection, databaseService.getExecuteSql(SqlTypeEnums.CONSTRAINT_INFO), tableName);
                List<ConstraintInfo> defaultInfoList = null;
                //sqlserver默认值处理
                if (Objects.equals(databaseType, SQL_SERVER)) {
                    defaultInfoList = JdbcUtil.getConstraintInfoList(connection, databaseService.getExecuteSql(SqlTypeEnums.DEFAULT_INFO), tableName);
                }
                executeSqlList.addAll(databaseService.getUpdateTableSql(tableInfo,
                        tableColumnInfoList,
                        constraintInfoList,
                        defaultInfoList,
                        modelEnums));
            } else {
                executeSqlList.addAll(databaseService.getCreateTableSql(tableInfo));
            }
        }
    }

    /**
     * 执行脚本
     *
     * @param connection
     * @param script
     */
    public void executeScript(Connection connection, String script, String databaseType) {
        if (StrUtil.isBlank(script)) {
            return;
        }
        log.info("执行 [{}] 初始化数据。。。", databaseType);
        for (String s : script.split(COMMA)) {
            try {
                Resource[] resources = new PathMatchingResourcePatternResolver()
                        .getResources(ResourceUtils.CLASSPATH_URL_PREFIX + s);
                for (Resource resource : resources) {
                    List<String> strings = inputStreamToString(resource.getInputStream());
                    for (String sql : strings) {
                        JdbcUtil.executeSql(connection, sql);
                    }
                }
            } catch (IOException | SQLException e) {
                throw new RuntimeException(StrUtil.format("初始化数据失败， message={}", e.getMessage()));
            }
        }

        log.info("执行 [{}] 初始化数据完成！！！", databaseType);
    }

    /**
     * input流转字符串
     *
     * @param inputStream
     * @return
     */
    public List<String> inputStreamToString(InputStream inputStream) {
        List<String> resultList = new ArrayList<>();
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String str;
            StringBuilder oneSql = new StringBuilder();
            while ((str = bufferedReader.readLine()) != null) {
                if (str.contains(SQL_SPLIT_STR)) {
                    resultList.add(oneSql + str);
                    oneSql = new StringBuilder();
                } else {
                    oneSql.append(str).append(CRLF);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("读取初始化文件文件异常", e);
        }
        return resultList;
    }
}
