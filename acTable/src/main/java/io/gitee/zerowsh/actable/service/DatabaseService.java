package io.gitee.zerowsh.actable.service;

import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;

import java.util.List;
import java.util.Map;

/**
 * 所有数据库基类
 *
 * @author zero
 */
public interface DatabaseService {

    /**
     * 自增就是主键
     *
     * @return
     */
    default boolean autoincrementIsPk() {
        return false;
    }

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
     * @param tableInfo          实体类获得
     * @param tableColumnInfoMap 表结构获得
     * @param constraintInfoList
     * @param defaultInfoList    默认值约束
     * @param modelEnums
     * @return
     */
    List<String> getUpdateTableSql(TableInfo tableInfo,
                                   Map<String, TableColumnInfo> tableColumnInfoMap,
                                   List<ConstraintInfo> constraintInfoList,
                                   List<ConstraintInfo> defaultInfoList,
                                   ModelEnums modelEnums);

    /**
     * 删除关键字处理
     *
     * @param var
     * @return
     */
    String delKeywordHandle(String var);

    /**
     * 增加关键字处理
     *
     * @param var
     * @return
     */
    String addKeywordHandle(String var);

    /**
     * 字段类型转数据库类型
     *
     * @param fieldType
     * @return
     */
    default String javaTypeTurnColumnType(String fieldType) {
        return this.javaTypeTurnColumnType(fieldType, ColumnTypeConstants.DEFAULT_VALUE);
    }

    ;

    /**
     * 字段类型转数据库类型
     *
     * @param fieldType
     * @param columnType
     * @return
     */
    String javaTypeTurnColumnType(String fieldType, String columnType);

    /**
     * 左边关键字
     *
     * @return
     */
    String leftKeyword();

    /**
     * 右边关键字
     *
     * @return
     */
    String rightKeyword();

    /**
     * 获取所有表SQL
     *
     * @return
     */
    String getAllTableSql();

    /**
     * 如果存在删除表SQL
     *
     * @param tableName
     * @return
     */
    default String dropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + this.addKeywordHandle(tableName);
    }

    /**
     * 是否存在表SQL
     *
     * @param tableName
     * @return
     */
    String existTableSql(String tableName);

    /**
     * 建表SQL
     *
     * @param tableName
     * @param columnInfo
     * @return
     */
    default String addTableSql(String tableName, String columnInfo, String tableComment) {
        return StrUtil.format("CREATE TABLE {} ({})", this.addKeywordHandle(tableName), columnInfo);
    }

    /**
     * 获取表结构SQL
     *
     * @param tableName
     * @return
     */
    String getTableStructureSql(String tableName);

    /**
     * 获取表约束SQL
     * 1 主键
     * 2 唯一键
     * 3 索引
     *
     * @param tableName
     * @return
     */
    String getConstraintInfoSql(String tableName);

    /**
     * 获取修改主键SQL
     *
     * @param tableName
     * @param constraintName
     * @param columnList
     * @return
     */
    String getUpdatePkSql(String tableName, String constraintName, List<String> columnList);

    /**
     * 获取删除主键SQL
     *
     * @param tableName
     * @return
     */
    String getDropPkSql(String tableName);

    /**
     * 获取表默认值约束SQL
     *
     * @param tableName
     * @return
     */
    String getDefaultInfoSql(String tableName);

    /**
     * 添加表注释SQL
     *
     * @param tableName
     * @param comment
     * @return
     */
    String addTableCommentSql(String tableName, String comment);

    /**
     * 添加字段注释SQL
     *
     * @param tableName
     * @param columnName
     * @param comment
     * @return
     */
    String addColumnCommentSql(String tableName, String columnName, String comment);

    /**
     * 添加主键SQL
     *
     * @param tableName
     * @param constraintName
     * @param columnList
     * @return
     */
    String addPrimaryKeySql(String tableName, String constraintName, List<String> columnList);

    /**
     * 添加索引SQL
     *
     * @param tableName
     * @param indexName
     * @param columns
     * @return
     */
    String addIndexSql(String tableName, String indexName, List<TableInfo.Index> columns);

    /**
     * 添加唯一索引SQL
     *
     * @param tableName
     * @param constraintName
     * @param columns
     * @return
     */
    String addUniqueIndexSql(String tableName, String constraintName, List<TableInfo.Index> columns);

    /**
     * 添加唯一约束SQL
     *
     * @param tableName
     * @param constraintName
     * @param columns
     * @return
     */
    String addUniqueSql(String tableName, String constraintName, List<TableInfo.Index> columns);


    /**
     * 获取修改表注释SQL
     *
     * @param tableName
     * @param tableComment
     * @return
     */
    String getUpdateTableCommentSql(String tableName, String tableComment);

    /**
     * 获取修改列注释SQL
     *
     * @param tableName
     * @param columnName
     * @param columnComment
     * @return
     */
    String getUpdateColumnCommentSql(String tableName, String columnName, String columnComment);

    /**
     * 新增列
     *
     * @param tableName
     * @param columnNameDetails
     * @return
     */
    String getAddColumnSql(String tableName, StringBuilder columnNameDetails);

    /**
     * 修改列
     *
     * @param tableName
     * @param columnNameDetails
     * @return
     */
    String getUpdateColumnSql(String tableName, StringBuilder columnNameDetails);

    /**
     * 删除列
     *
     * @param tableName
     * @param columnName
     * @return
     */
    String getDelColumnSql(String tableName, String columnName);

    /**
     * 修改列名称
     *
     * @param tableName
     * @param oldColumnName
     * @param newColumnName
     * @param columnNameDetails
     * @return
     */
    String getUpdateColumnNameSql(String tableName, String oldColumnName, String newColumnName, String columnNameDetails);

    /**
     * 删除索引（普通索引+唯一索引）
     *
     * @param indexName
     * @return
     */
    String getDropIndexSql(String indexName);

    /**
     * 删除约束（唯一约束）
     *
     * @param tableName
     * @param constraintName
     * @return
     */
    String getDropConstraintSql(String tableName, String constraintName);


}
