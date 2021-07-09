package io.gitee.zerowsh.actable.service;

import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.config.CreateTableConfig;
import io.gitee.zerowsh.actable.dao.SqlServerMapper;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.DatabaseTypeEnums;
import io.gitee.zerowsh.actable.util.AcTableUtils;
import io.gitee.zerowsh.actable.util.SqlServerAcTableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * sql_server实现
 *
 * @author zero
 */
@Component
@Slf4j
public class SqlServerImpl {

    @Resource
    private SqlServerMapper sqlServerMapper;
    @Resource
    private AcTableUtils acTableUtils;

    @Transactional(rollbackFor = Exception.class)
    public void acTable(CreateTableConfig createTableConfig, List<TableInfo> tableInfoList) {
        DatabaseTypeEnums databaseType = createTableConfig.getDatabaseType();
        log.info(StrUtil.format("执行 [{}] 自动建表。。。", databaseType));
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getName();
            if (sqlServerMapper.isExistTable(tableName) == 0) {
                //不存在--建表
                List<String> createTableSql = SqlServerAcTableUtils.getCreateTableSql(tableInfo);
                for (String sql : createTableSql) {
                    sqlServerMapper.executeSql(sql);
                    log.info(sql);
                }
            } else {
                /*
                 * 存在--改表
                 *
                 * 1.获取表结构
                 * 2.获取约束（唯一键、主键、索引）
                 * 3.获取默认值约束
                 * 4.修改表
                 * 5.顺序执行sql
                 */
                List<TableColumnInfo> tableColumnInfoList = sqlServerMapper.getTableStructure(tableName);
                List<ConstraintInfo> constraintInfoList = sqlServerMapper.getConstraintInfo(tableName);
                List<ConstraintInfo> defaultInfoList = sqlServerMapper.getDefaultInfo(tableName);
                List<String> updateTableSql = SqlServerAcTableUtils.getUpdateTableSql(tableInfo,
                        tableColumnInfoList,
                        constraintInfoList,
                        defaultInfoList,
                        createTableConfig.getModel());
                for (String sql : updateTableSql) {
                    sqlServerMapper.executeSql(sql);
                    log.info(sql);
                }
            }
        }
        log.info(StrUtil.format("执行 [{}] 自动建表完成！！！", databaseType));
        acTableUtils.executeScript();
    }
}
