package io.gitee.zerowsh.actable.service;

import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.emnus.SqlTypeEnums;

import java.util.List;

/**
 * 所有数据库基类
 *
 * @author zero
 */
public interface DatabaseService {
    /**
     * 获取创建表的sql语句
     *
     * @param tableInfo
     * @return
     */
    List<String> getCreateTableSql(TableInfo tableInfo);

    /**
     * 获取修改表的sql语句
     *
     * @param tableInfo
     * @param tableColumnInfoList
     * @param constraintInfoList
     * @param defaultInfoList     默认值约束
     * @param modelEnums
     * @return
     */
    List<String> getUpdateTableSql(TableInfo tableInfo,
                                   List<TableColumnInfo> tableColumnInfoList,
                                   List<ConstraintInfo> constraintInfoList,
                                   List<ConstraintInfo> defaultInfoList,
                                   ModelEnums modelEnums);

    /**
     * 处理关键字
     *
     * @param var
     * @return
     */
    String handleKeyword(String var);

    /**
     * 字段类型转数据库类型
     *
     * @param fieldType
     * @return
     */
    default String javaTypeTurnColumnType(String fieldType) {
        return this.javaTypeTurnColumnType(fieldType, ColumnTypeEnums.DEFAULT);
    }

    ;

    /**
     * 字段类型转数据库类型
     *
     * @param fieldType
     * @param type
     * @return
     */
    String javaTypeTurnColumnType(String fieldType, ColumnTypeEnums type);

    /**
     * 获取执行sql语句
     *
     * @param sqlTypeEnums
     * @return
     */
    String getExecuteSql(SqlTypeEnums sqlTypeEnums);
}
