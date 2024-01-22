package io.gitee.zerowsh.actable.service;

import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;

import java.util.List;

/**
 * 所有数据库基类
 *
 * @author zero
 */
public interface DatabaseService {

    /**
     * 处理字符串长度
     *
     * @param length
     * @return
     */
    default long handleStrLength(int length) {
        return length < 0 ? 255 : length;
    }

    /**
     * 处理时间长度
     *
     * @param length
     * @return
     */
    default int handleDateLength(int length) {
        return length > 7 || length < 0 ? 0 : length;
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
     * @param tableInfo           实体类获得
     * @param tableColumnInfoList 表结构获得
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
    default String addTableSql(String tableName, String columnInfo) {
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

}
