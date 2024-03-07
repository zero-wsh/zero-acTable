package io.gitee.zerowsh.actable.autoconfig;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.constant.AcTableConstants;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.properties.AcTableProperties;
import io.gitee.zerowsh.actable.service.DatabaseService;
import io.gitee.zerowsh.actable.service.impl.DmImpl;
import io.gitee.zerowsh.actable.service.impl.MysqlImpl;
import io.gitee.zerowsh.actable.service.impl.SqlServerImpl;
import io.gitee.zerowsh.actable.util.HandlerEntityUtils;
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
import java.util.Map;
import java.util.Objects;

import static cn.hutool.core.util.StrUtil.COMMA;

/**
 * mysql实现
 *
 * @author zero
 */
@Slf4j
public class AcTableService {

    public AcTableService(DataSource dataSource, AcTableProperties acTableProperties) {
        this.acTable(dataSource, acTableProperties);
    }

    public void acTable(DataSource dataSource, AcTableProperties acTableProperties) {
        if (Objects.isNull(acTableProperties.getModel())
                || Objects.equals(acTableProperties.getModel(), ModelEnums.NONE)) {
            log.info("自动建表不做任何操作！");
            return;
        }
        String entityPackage = acTableProperties.getEntityPackage();
        if (StrUtil.isBlank(entityPackage)) {
            log.warn("没有需要操作的类，请设置实体类包路径！");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            String databaseType = connection.getMetaData().getDatabaseProductName();
            if (StrUtil.isBlank(databaseType)) {
                log.error("获取数据库类型失败！");
                return;
            }
            DatabaseService databaseService = this.getDatabaseService(databaseType);
            if (Objects.isNull(databaseService)) {
                log.error("数据库类型不支持 databaseType={}", databaseType);
                return;
            }
            //执行自动建表前先执行sql脚本，用于表存在时先更改相关字段，如mysql自增字段。
            this.executeScript(connection, acTableProperties, acTableProperties.getBeforeScript(), databaseType);
            List<TableInfo> tableInfoList = HandlerEntityUtils.getTableInfoByEntityPackage(acTableProperties, databaseService);
            if (CollectionUtil.isEmpty(tableInfoList)) {
                log.error("没有找到io.gitee.zerowsh.actable.annotation.AcTable、" +
                        "com.baomidou.mybatisplus.annotation.TableName、" +
                        "javax.persistence.Table注解标记的类！ entityPackage={}", entityPackage);
                return;
            }

            ModelEnums modelEnums = acTableProperties.getModel();
            List<String> executeSqlList = new ArrayList<>();
            this.handleExecuteSql(connection, modelEnums, tableInfoList, executeSqlList, databaseService);
            log.info(StrUtil.format("开始【{}】自动建表！", databaseType));
            if (CollectionUtil.isNotEmpty(executeSqlList)) {
                for (String sql : executeSqlList) {
                    JdbcUtil.executeSql(connection, sql);
                }
            }
            log.info(StrUtil.format("完成【{}】自动建表！", databaseType));
            this.executeScript(connection, acTableProperties, acTableProperties.getAfterScript(), databaseType);
        } catch (Exception e) {
            throw new RuntimeException("自动建表异常", e);
        }
    }

    public DatabaseService getDatabaseService(String databaseType) {
        switch (databaseType) {
            case AcTableConstants.MYSQL:
                return new MysqlImpl();
            case AcTableConstants.SQL_SERVER:
                return new SqlServerImpl();
            case AcTableConstants.DM:
                return new DmImpl();
            default:
                return null;
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

    public void handleExecuteSql(Connection connection,
                                 ModelEnums modelEnums,
                                 List<TableInfo> tableInfoList,
                                 List<String> executeSqlList,
                                 DatabaseService databaseService) throws SQLException {
        if (Objects.equals(modelEnums, ModelEnums.DEL_AND_ADD)) {
            for (TableInfo tableInfo : tableInfoList) {
                JdbcUtil.executeSql(connection, databaseService.dropTableSql(tableInfo.getName()));
            }
        }
        if (Objects.equals(modelEnums, ModelEnums.DEL_ALL_AND_ADD)) {
            List<String> tableNameList = JdbcUtil.getTableNameList(connection, databaseService.getAllTableSql());
            for (String tableName : tableNameList) {
                JdbcUtil.executeSql(connection, databaseService.dropTableSql(tableName));
            }
        }
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getName();
            if (JdbcUtil.isExist(connection, databaseService.existTableSql(tableName))) {
                /*
                 * 存在--改表
                 */
                Map<String, TableColumnInfo> tableColumnInfoMap = JdbcUtil.getTableColumnInfoMap(connection, databaseService.getTableStructureSql(tableName));
                List<ConstraintInfo> constraintInfoList = JdbcUtil.getConstraintInfoList(connection, databaseService.getConstraintInfoSql(tableName));
                //sqlserver有默认值约束
                List<ConstraintInfo> defaultInfoList = JdbcUtil.getConstraintInfoList(connection, databaseService.getDefaultInfoSql(tableName));
                executeSqlList.addAll(databaseService.getUpdateTableSql(tableInfo,
                        tableColumnInfoMap,
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
     * @param acTableProperties
     * @param script
     * @param databaseType
     */
    public void executeScript(Connection connection, AcTableProperties acTableProperties, String script, String databaseType) {
        if (StrUtil.isBlank(script)) {
            return;
        }
        log.info("执行【{}】SQL脚本 [{}]！", databaseType, script);
        for (String s : script.split(COMMA)) {
            try {
                Resource[] resources = new PathMatchingResourcePatternResolver()
                        .getResources(ResourceUtils.CLASSPATH_URL_PREFIX + s);
                for (Resource resource : resources) {
                    List<String> strings = this.inputStreamToString(resource.getInputStream(), script, acTableProperties);
                    for (String sql : strings) {
                        JdbcUtil.executeSql(connection, sql);
                    }
                }
            } catch (IOException | SQLException e) {
                throw new RuntimeException(StrUtil.format("执行【{}】SQL脚本【{}】失败， message={}！", databaseType, script, e.getMessage()));
            }
        }
        log.info("执行【{}】SQL脚本【{}】完成！", databaseType, script);
    }

    /**
     * input流转字符串
     *
     * @param inputStream
     * @param script
     * @param acTableProperties
     * @return
     */
    public List<String> inputStreamToString(InputStream inputStream, String script, AcTableProperties acTableProperties) {
        List<String> resultList = new ArrayList<>();
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String str;
            StringBuilder oneSql = new StringBuilder();
            //一行一行读取
            while ((str = bufferedReader.readLine()) != null) {
                if (str.contains(acTableProperties.getEndFlag())) {
                    if (acTableProperties.getSqlPart()) {
                        resultList.add(oneSql + str);
                    } else {
                        resultList.add(oneSql + str.replaceAll(acTableProperties.getEndFlag(), ""));
                    }
                    oneSql = new StringBuilder();
                } else {
                    oneSql.append(str).append(StrPool.CRLF);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(StrUtil.format("读取初始化文件【{}】异常！", script), e);
        }
        return resultList;
    }
}
