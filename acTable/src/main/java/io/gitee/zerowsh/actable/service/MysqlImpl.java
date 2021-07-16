package io.gitee.zerowsh.actable.service;

import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.mapper.MysqlMapper;
import io.gitee.zerowsh.actable.util.sql.MysqlAcTableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
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

    @Transactional(rollbackFor = Exception.class)
    public List<String> acTable(ModelEnums modelEnums, List<TableInfo> tableInfoList) {
        List<String> resultList = new ArrayList<>();
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getName();
            if (mysqlMapper.isExistTable(tableName) == 0) {
                //不存在--建表
                resultList.addAll(MysqlAcTableUtils.getCreateTableSql(tableInfo));
            } else {
                /*
                 * 存在--改表
                 */
                List<TableColumnInfo> tableColumnInfoList = mysqlMapper.getTableStructure(tableName);
                List<ConstraintInfo> constraintInfoList = mysqlMapper.getConstraintInfo(tableName);
                List<String> updateTableSql = MysqlAcTableUtils.getUpdateTableSql(tableInfo,
                        tableColumnInfoList,
                        constraintInfoList,
                        modelEnums);
                resultList.addAll(updateTableSql);
            }
        }
        return resultList;
    }
}
