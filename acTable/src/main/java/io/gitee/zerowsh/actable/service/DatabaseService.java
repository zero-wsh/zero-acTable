package io.gitee.zerowsh.actable.service;

import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.DOUBLE_QUOTES;
import static io.gitee.zerowsh.actable.constant.AcTableConstants.SINGLE_QUOTE;

/**
 * 所有数据库基类
 *
 * @author zero
 */
@Slf4j
public abstract class DatabaseService {
    public String leftSymbol;// 左包裹符号
    public String rightSymbol;// 右包裹符号

    // 强制子类通过构造函数传递符号
    public DatabaseService(String leftSymbol, String rightSymbol) {
        this.leftSymbol = leftSymbol;
        this.rightSymbol = rightSymbol;
    }

    /**
     * 自增就是主键
     *
     * @return
     */
    public boolean autoincrementIsPk() {
        return false;
    }

    /**
     * 拼接sql时忽略length和decimalLength
     *
     * @return 需要忽略的字段类型集合
     */
    abstract public Set<String> ignoreLengthAndDecimalLength();

    /**
     * 默认值处理
     *
     * @return
     */
    public String defaultValue(String var) {
        if (StrUtil.isNotBlank(var)) {
            if (var.startsWith(SINGLE_QUOTE) && var.endsWith(SINGLE_QUOTE)) {
                return StrUtil.removeSuffix(StrUtil.removePrefix(var, SINGLE_QUOTE), SINGLE_QUOTE);
            } else if (var.startsWith(DOUBLE_QUOTES) && var.endsWith(DOUBLE_QUOTES)) {
                return StrUtil.removeSuffix(StrUtil.removePrefix(var, DOUBLE_QUOTES), DOUBLE_QUOTES);
            }
        }
        return var;
    }


    /**
     * 删除关键字处理
     *
     * @param var
     * @return
     */
    public String delKeywordHandle(String var) {
        if (var.startsWith(leftSymbol) && var.endsWith(rightSymbol)) {
            return StrUtil.removeSuffix(StrUtil.removePrefix(var, leftSymbol), rightSymbol);
        }
        return var;
    }

    /**
     * 增加关键字处理
     *
     * @param var
     * @return
     */
    public String addKeywordHandle(String var) {
        return leftSymbol + var + rightSymbol;
    }

    /**
     * 获取创建表的sql语句
     *
     * @param tableInfo
     * @return
     */
    abstract public List<String> getCreateTableSql(TableInfo tableInfo);

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
    abstract public List<String> getUpdateTableSql(TableInfo tableInfo,
                                                   Map<String, TableInfo.PropertyInfo> tableColumnInfoMap,
                                                   List<ConstraintInfo> constraintInfoList,
                                                   List<ConstraintInfo> defaultInfoList,
                                                   ModelEnums modelEnums);


    /**
     * 字段类型转数据库类型
     *
     * @param fieldType
     * @return
     */
    public String javaTypeTurnColumnType(String fieldType) {
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
    abstract public String javaTypeTurnColumnType(String fieldType, String columnType);

    /**
     * 获取所有表SQL
     *
     * @return
     */
    abstract public String getAllTableSql();

    /**
     * 如果存在删除表SQL
     *
     * @param tableName
     * @return
     */
    public String dropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + this.addKeywordHandle(tableName);
    }

    /**
     * 是否存在表SQL
     *
     * @param tableName
     * @return
     */
    abstract public String existTableSql(String tableName);

    /**
     * 建表SQL
     *
     * @param tableName
     * @param columnInfo
     * @param suffixInfo 后缀信息比如：COMMENT='表注释'
     * @return
     */
    public String addTableSql(String tableName, String columnInfo, String suffixInfo) {
        return StrUtil.format("CREATE TABLE {} ({}){}", this.addKeywordHandle(tableName), columnInfo, suffixInfo);
    }

    /**
     * 获取表结构SQL
     *
     * @param tableName
     * @return
     */
    abstract public String getTableStructureSql(String tableName);

    /**
     * 获取表约束SQL
     * 1 主键
     * 2 唯一键
     * 3 索引
     *
     * @param tableName
     * @return
     */
    abstract public String getConstraintInfoSql(String tableName);

    /**
     * 获取修改主键SQL
     *
     * @param tableName
     * @param constraintName
     * @param columnList
     * @param tableExistPk
     * @return
     */
    abstract public String getUpdatePkSql(String tableName, String constraintName, List<String> columnList, boolean tableExistPk);

    /**
     * 获取删除主键SQL
     *
     * @param tableName
     * @return
     */
    abstract public String getDropPkSql(String tableName);

    /**
     * 获取表默认值约束SQL
     *
     * @param tableName
     * @return
     */
    abstract public String getDefaultInfoSql(String tableName);

    /**
     * 添加表注释SQL
     *
     * @param tableName
     * @param comment
     * @return
     */
    abstract public String addTableCommentSql(String tableName, String comment);

    /**
     * 添加字段注释SQL
     *
     * @param tableName
     * @param columnName
     * @param comment
     * @return
     */
    abstract public String addColumnCommentSql(String tableName, String columnName, String comment);

    /**
     * 添加主键SQL
     *
     * @param tableName
     * @param constraintName
     * @param columnList
     * @return
     */
    abstract public String addPrimaryKeySql(String tableName, String constraintName, List<String> columnList);

    /**
     * 添加索引SQL
     *
     * @param tableName
     * @param indexName
     * @param columns
     * @return
     */
    abstract public String addIndexSql(String tableName, String indexName, List<TableInfo.Index> columns);

    /**
     * 添加唯一索引SQL
     *
     * @param tableName
     * @param constraintName
     * @param columns
     * @return
     */
    abstract public String addUniqueIndexSql(String tableName, String constraintName, List<TableInfo.Index> columns);

    /**
     * 添加唯一约束SQL
     *
     * @param tableName
     * @param constraintName
     * @param columns
     * @return
     */
    abstract public String addUniqueSql(String tableName, String constraintName, List<TableInfo.Index> columns);


    /**
     * 获取修改表注释SQL
     *
     * @param tableName
     * @param tableComment
     * @return
     */
    abstract public String getUpdateTableCommentSql(String tableName, String tableComment);

    /**
     * 获取修改列注释SQL
     *
     * @param tableName
     * @param columnName
     * @param columnComment
     * @return
     */
    abstract public String getUpdateColumnCommentSql(String tableName, String columnName, String columnComment);

    /**
     * 新增列
     *
     * @param tableName
     * @param columnNameDetails
     * @return
     */
    abstract public String getAddColumnSql(String tableName, StringBuilder columnNameDetails);

    /**
     * 修改列
     *
     * @param tableName
     * @param columnNameDetails
     * @return
     */
    abstract public String getUpdateColumnSql(String tableName, String columnNameDetails);

    /**
     * 删除列
     *
     * @param tableName
     * @param columnName
     * @return
     */
    abstract public String getDelColumnSql(String tableName, String columnName);

    /**
     * 修改列名称
     *
     * @param tableName
     * @param oldColumnName
     * @param newColumnName
     * @param columnNameDetails
     * @return
     */
    abstract public String getUpdateColumnNameSql(String tableName, String oldColumnName, String newColumnName, String columnNameDetails);

    /**
     * 删除索引（普通索引+唯一索引）
     *
     * @param indexName
     * @return
     */
    abstract public String getDropIndexSql(String indexName);

    /**
     * 删除约束（唯一约束）
     *
     * @param tableName
     * @param constraintName
     * @return
     */
    abstract public String getDropConstraintSql(String tableName, String constraintName);


}
