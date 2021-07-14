package io.gitee.zerowsh.actable.service;

import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.config.CreateTableConfig;
import io.gitee.zerowsh.actable.dao.MysqlMapper;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.DatabaseTypeEnums;
import io.gitee.zerowsh.actable.util.AcTableUtils;
import io.gitee.zerowsh.actable.util.MysqlAcTableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * mysql实现
 *
 * @author zero
 */
@Component
@Slf4j
public class MysqlImpl {

    @Resource
    private MysqlMapper mysqlMapper;
    @Resource
    private AcTableUtils acTableUtils;

    @Transactional(rollbackFor = Exception.class)
    public void acTable(CreateTableConfig createTableConfig, List<TableInfo> tableInfoList) {
        DatabaseTypeEnums databaseType = createTableConfig.getDatabaseType();
        log.info(StrUtil.format("执行 [{}] 自动建表。。。", databaseType));
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getName();
            if (mysqlMapper.isExistTable(tableName) == 0) {
                //不存在--建表
                List<String> createTableSql = MysqlAcTableUtils.getCreateTableSql(tableInfo);
                for (String sql : createTableSql) {
                    mysqlMapper.executeSql(sql);
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
                List<TableColumnInfo> tableColumnInfoList = mysqlMapper.getTableStructure(tableName);
                List<ConstraintInfo> constraintInfoList = mysqlMapper.getConstraintInfo(tableName);
                List<String> updateTableSql = MysqlAcTableUtils.getUpdateTableSql(tableInfo,
                        tableColumnInfoList,
                        constraintInfoList,
                        createTableConfig.getModel());
                for (String sql : updateTableSql) {
                    mysqlMapper.executeSql(sql);
                    log.info(sql);
                }
            }
        }
        log.info(StrUtil.format("执行 [{}] 自动建表完成！！！", databaseType));
//        acTableUtils.executeScript();
    }
}
