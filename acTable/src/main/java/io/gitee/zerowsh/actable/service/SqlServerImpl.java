package io.gitee.zerowsh.actable.service;

import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.mapper.SqlServerMapper;
import io.gitee.zerowsh.actable.util.sql.SqlServerAcTableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
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

    @Transactional(rollbackFor = Exception.class)
    public List<String> acTable(ModelEnums modelEnums, List<TableInfo> tableInfoList) {
        List<String> resultList = new ArrayList<>();
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getName();
            if (sqlServerMapper.isExistTable(tableName) == 0) {
                //不存在--建表
                resultList.addAll(SqlServerAcTableUtils.getCreateTableSql(tableInfo));
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
                        modelEnums);
                resultList.addAll(updateTableSql);
            }
        }
        return resultList;
    }
}
