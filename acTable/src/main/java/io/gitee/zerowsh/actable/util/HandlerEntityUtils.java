package io.gitee.zerowsh.actable.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import io.gitee.zerowsh.actable.annotation.*;
import io.gitee.zerowsh.actable.config.AcTableConfig;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.TurnEnums;
import io.gitee.zerowsh.actable.util.sql.MysqlAcTableUtils;
import io.gitee.zerowsh.actable.util.sql.SqlServerAcTableUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.*;

/**
 * 处理实体工具类
 *
 * @author zero
 */
@SuppressWarnings("all")
@Slf4j
public class HandlerEntityUtils {
    /**
     * 通过实体类包名获取所有表信息 (字段 索引 唯一值等)
     *
     * @param acTableConfig
     * @return
     */
    public static List<TableInfo> getTableInfoByEntityPackage(AcTableConfig acTableConfig) {
        String entityPackage = acTableConfig.getEntityPackage();
        TurnEnums turn = acTableConfig.getTurn();
        //实体类表信息
        List<TableInfo> tableInfoList = new ArrayList<>();
        //用来判断是否有重复表名
        List<String> tableList = new ArrayList<>();
        for (String s : entityPackage.split(StringPool.COMMA)) {
            Set<Class<?>> tableClass = ClassUtil.scanPackageByAnnotation(s, Table.class);
            Set<Class<?>> tableNameClass = ClassUtil.scanPackageByAnnotation(s, TableName.class);
            Set<Class<?>> tableSet = new HashSet<>();
            tableSet.addAll(tableClass);
            tableSet.addAll(tableNameClass);
            for (Class<?> cls : tableSet) {
                TableInfo.TableInfoBuilder builder = TableInfo.builder();
                List<TableInfo.PropertyInfo> propertyInfoList = new ArrayList<>();
                List<TableInfo.IndexInfo> indexInfoList = new ArrayList<>();
                List<TableInfo.UniqueInfo> uniqueInfoList = new ArrayList<>();
                List<String> keyList = new ArrayList<>();
                List<String> propertyList = new ArrayList<>();
                Table table = cls.getAnnotation(Table.class);
                TableName tableNameAnn = cls.getAnnotation(TableName.class);
                String tableName = null;
                String comment = DEFAULT_VALUE;
                if (Objects.nonNull(table)) {
                    tableName = table.name();
                    comment = table.comment();
                }
                if (Objects.nonNull(tableNameAnn)) {
                    tableName = tableNameAnn.value();
                }
                if (StrUtil.isBlank(tableName)) {
                    throw new RuntimeException(StrUtil.format("@Table和@TableName 都没设置表名！！！"));
                }
                if (tableList.contains(tableName)) {
                    throw new RuntimeException(StrUtil.format("[{}] 表名重复", tableName));
                }
                tableName = AcTableUtils.handleKeyword(tableName);
                tableList.add(tableName);
                builder.name(tableName);
                builder.comment(judgeIsNull(comment));
                getFieldInfo(cls, propertyInfoList, indexInfoList,
                        uniqueInfoList, keyList, propertyList, table,
                        null, turn, acTableConfig);
                if (CollectionUtil.isEmpty(propertyInfoList)) {
                    throw new RuntimeException(StrUtil.format("类 [{}] 不存在字段信息", cls.getName()));
                }
                TableInfo tableInfo = builder.keyList(keyList)
                        .propertyInfoList(propertyInfoList)
                        .indexInfoList(indexInfoList)
                        .uniqueInfoList(uniqueInfoList)
                        .build();
                tableInfoList.add(tableInfo);
            }
        }
        return tableInfoList;

    }

    /**
     * 判断是否为null
     *
     * @param comment
     * @return
     */
    private static String judgeIsNull(String comment) {
        return Objects.equals(comment, DEFAULT_VALUE) ? null : comment;
    }

    /**
     * 递归获取字段信息
     *
     * @param cls
     * @param propertyInfoList
     * @param indexInfoList
     * @param uniqueInfoList
     * @param keyList
     * @param propertyList      判断类中是否有重复字段
     * @param table
     * @param excludeSuperField
     * @param turn
     * @return
     */
    private static void getFieldInfo(Class<?> cls, List<TableInfo.PropertyInfo> propertyInfoList,
                                     List<TableInfo.IndexInfo> indexInfoList,
                                     List<TableInfo.UniqueInfo> uniqueInfoList,
                                     List<String> keyList,
                                     List<String> propertyList,
                                     Table table,
                                     ExcludeSuperField excludeSuperField,
                                     TurnEnums turn,
                                     AcTableConfig acTableConfig) {
        for (Field field : cls.getDeclaredFields()) {
            TableInfo.PropertyInfo.PropertyInfoBuilder propertyInfoBuilder = TableInfo.PropertyInfo.builder();
            String fieldName = field.getName();
            String columnName = null;
            //需要排除父类的字段
            if (Objects.nonNull(excludeSuperField)) {
                String[] value = excludeSuperField.value();
                if (ArrayUtil.isNotEmpty(value)) {
                    if (Arrays.asList(value).contains(fieldName)) {
                        continue;
                    }
                }
            }
            //需要排除修饰符的方法
            String modifier = Modifier.toString(field.getModifiers());
            if (modifier.contains(STATIC) || modifier.contains(TRANSIENT)) {
                continue;
            }

            Column column = field.getAnnotation(Column.class);
            TableField tableField = field.getAnnotation(TableField.class);
            TableId tableId = field.getAnnotation(TableId.class);
            if (Objects.isNull(column)) {
                if (Objects.nonNull(tableField) && !tableField.exist()) {
                    continue;
                }
                if (Objects.nonNull(tableField)) {
                    columnName = tableField.value();
                }
                columnName = StrUtil.isBlank(columnName) ? fieldNameTurnDatabaseColumn(fieldName, turn, table) : columnName;
                if (propertyList.contains(columnName)) {
                    throw new RuntimeException(StrUtil.format(COLUMN_DUPLICATE_VALID_STR, fieldName));
                }
                propertyList.add(columnName);
                boolean isKey = Objects.nonNull(tableId);
                boolean isAutoIncrement = Objects.nonNull(tableId) && Objects.equals(tableId.type(), IdType.AUTO);
                propertyInfoBuilder.columnName(columnName)
                        .decimalLength(COLUMN_DECIMAL_LENGTH_DEF)
                        .isNull(COLUMN_IS_NULL_DEF)
                        .isKey(isKey)
                        .isAutoIncrement(isAutoIncrement)
                        .length(COLUMN_LENGTH_DEF)
                        .type(AcTableUtils.handleType(field.getType().getName()));
                //处理主键
                if (isKey) {
                    keyList.add(columnName);
                }
            } else {
                if ((Objects.nonNull(tableField) && !tableField.exist()) || column.exclude()) {
                    continue;
                }
                if (StrUtil.isNotBlank(column.name())) {
                    columnName = column.name();
                }
                if (Objects.nonNull(tableField)) {
                    columnName = tableField.value();
                }
                columnName = AcTableUtils.handleKeyword(StrUtil.isBlank(columnName) ? fieldNameTurnDatabaseColumn(fieldName, turn, table) : columnName);
                if (propertyList.contains(columnName)) {
                    throw new RuntimeException(StrUtil.format(COLUMN_DUPLICATE_VALID_STR, fieldName));
                }
                propertyList.add(columnName);

                boolean isKey = Objects.nonNull(tableId) || column.isKey();
                boolean isAutoIncrement = column.isAutoIncrement() || (Objects.nonNull(tableId) && Objects.equals(tableId.type(), IdType.AUTO));
                propertyInfoBuilder.columnName(columnName)
                        .columnComment(judgeIsNull(column.comment()))
                        .decimalLength(column.decimalLength())
                        .defaultValue(judgeIsNull(column.defaultValue()))
                        .isAutoIncrement(isAutoIncrement)
                        .isKey(isKey)
                        .isNull(column.isNull())
                        .length(column.length())
                        .type(getTypeStr(field.getType().getName(), column.type()));
                //处理主键
                if (isKey) {
                    keyList.add(columnName);
                }
            }
            propertyInfoList.add(propertyInfoBuilder.build());

            /*
             *处理索引
             */
            Index index = field.getAnnotation(Index.class);
            if (Objects.nonNull(index)) {
                String[] columns = index.columns();
                String value = index.value();
                TableInfo.IndexInfo indexInfo = TableInfo.IndexInfo.builder()
                        .value(StrUtil.isBlank(value) ? IDX_ + fieldName : IDX_ + value)
                        .columns(ArrayUtil.isEmpty(columns) ? new String[]{fieldNameTurnDatabaseColumn(fieldName, turn, table)} : columns).build();
                indexInfoList.add(indexInfo);
            }
            /*
             *处理唯一键
             */
            Unique unique = field.getAnnotation(Unique.class);
            if (Objects.nonNull(unique)) {
                String[] columns = unique.columns();
                String value = unique.value();
                TableInfo.UniqueInfo uniqueInfo = TableInfo.UniqueInfo.builder()
                        .value(StrUtil.isBlank(value) ? UK_ + fieldName : UK_ + value)
                        .columns(ArrayUtil.isEmpty(columns) ? new String[]{fieldNameTurnDatabaseColumn(fieldName, turn, table)} : columns).build();
                uniqueInfoList.add(uniqueInfo);
            }

        }
        Class<?> superclass = cls.getSuperclass();
        if (Objects.isNull(superclass)) {
            return;
        }
        getFieldInfo(superclass, propertyInfoList, indexInfoList,
                uniqueInfoList, keyList, propertyList, table,
                cls.getAnnotation(ExcludeSuperField.class), turn, acTableConfig);
    }

    /**
     * java字段名转数据库列
     *
     * @param fieldName
     * @param turn
     * @return
     */
    private static String fieldNameTurnDatabaseColumn(String fieldName, TurnEnums turn, Table table) {
        TurnEnums columnTurn = Objects.nonNull(table) ? table.turn() : TurnEnums.DEFAULT;
        if (Objects.equals(columnTurn, TurnEnums.DEFAULT)) {
            if (turn == TurnEnums.SOURCE) {
                return fieldName;
            }
        } else {
            if (columnTurn == TurnEnums.SOURCE) {
                return fieldName;
            }
        }
        return StrUtil.toUnderlineCase(fieldName);
    }

    /**
     * 处理类型
     *
     * @param var
     * @return
     */
    public static String getTypeStr(String fieldType, ColumnTypeEnums type) {
        String databaseType = AcTableThreadLocalUtils.getDatabaseType();
        switch (databaseType) {
            case SQL_SERVER:
                if (Objects.equals(type, ColumnTypeEnums.DEFAULT)) {
                    return SqlServerAcTableUtils.getJavaTurnSqlServerValue(fieldType);
                } else {
                    boolean contains = ColumnTypeEnums.SQL_SERVER_NOT_EXIST_TYPE.contains(type);
                    if (contains) {
                        log.warn(COLUMN_TYPE_FAIL, databaseType, type, ColumnTypeEnums.NVARCHAR);
                        return ColumnTypeEnums.NVARCHAR.getType();
                    } else {
                        return type.getType();
                    }
                }
            case MYSQL:
                if (Objects.equals(type, ColumnTypeEnums.DEFAULT)) {
                    return MysqlAcTableUtils.getJavaTurnMysqlValue(fieldType);
                } else {
                    boolean contains = ColumnTypeEnums.MYSQL_NOT_EXIST_TYPE.contains(type);
                    if (contains) {
                        log.warn(COLUMN_TYPE_FAIL, databaseType, type, ColumnTypeEnums.VARCHAR);
                        return ColumnTypeEnums.VARCHAR.getType();
                    } else {
                        return type.getType();
                    }
                }
            default:
        }
        return null;
    }
}
