package io.gitee.zerowsh.actable.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.gitee.zerowsh.actable.annotation.*;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.TurnEnums;
import io.gitee.zerowsh.actable.properties.AcTableProperties;
import io.gitee.zerowsh.actable.util.sql.MysqlAcTableUtils;
import io.gitee.zerowsh.actable.util.sql.SqlServerAcTableUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.*;
import static io.gitee.zerowsh.actable.constant.StringConstants.COMMA;

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
     * @param acTableProperties
     * @return
     */
    public static List<TableInfo> getTableInfoByEntityPackage(AcTableProperties acTableProperties) {
        String entityPackage = acTableProperties.getEntityPackage();
        TurnEnums turn = acTableProperties.getTurn();
        //实体类表信息
        List<TableInfo> tableInfoList = new ArrayList<>();
        //用来判断是否有重复表名
        List<String> tableList = new ArrayList<>();
        for (String s : entityPackage.split(COMMA)) {
            Set<Class<?>> acTableClass = ClassUtil.scanPackageByAnnotation(s, AcTable.class);
            //mybatis plus兼容
            Set<Class<?>> tableNameClass = ClassUtil.scanPackageByAnnotation(s, TableName.class);
            //hibernate 兼容
            Set<Class<?>> tableClass = ClassUtil.scanPackageByAnnotation(s, Table.class);
            Set<Class<?>> tableSet = new HashSet<>();
            tableSet.addAll(acTableClass);
            tableSet.addAll(tableNameClass);
            tableSet.addAll(tableClass);
            for (Class<?> cls : tableSet) {
                if (Objects.nonNull(cls.getAnnotation(IgnoreTable.class))) {
                    continue;
                }
                TableInfo.TableInfoBuilder builder = TableInfo.builder();
                List<TableInfo.PropertyInfo> propertyInfoList = new ArrayList<>();
                List<TableInfo.IndexInfo> indexInfoList = new ArrayList<>();
                List<TableInfo.UniqueInfo> uniqueInfoList = new ArrayList<>();
                List<String> keyList = new ArrayList<>();
                List<String> propertyList = new ArrayList<>();
                AcTable acTable = cls.getAnnotation(AcTable.class);
                //mybatis plus兼容
                TableName tableNameAnn = cls.getAnnotation(TableName.class);
                //hibernate 兼容
                Table tableAnn = cls.getAnnotation(Table.class);
                String tableName = null;
                String comment = DEFAULT_VALUE;
                ApiModel apiModel = cls.getAnnotation(ApiModel.class);
                if (Objects.nonNull(acTable)) {
                    tableName = acTable.name();
                    comment = acTable.comment();
                }
                if (Objects.nonNull(apiModel)) {
                    comment = apiModel.value();
                }
                if (Objects.nonNull(tableNameAnn)) {
                    tableName = tableNameAnn.value();
                }
                if (Objects.nonNull(tableAnn)) {
                    tableName = tableAnn.name();
                }
                if (StrUtil.isBlank(tableName)) {
                    throw new RuntimeException(StrUtil.format("AcTable、com.baomidou.mybatisplus.annotation.TableName、javax.persistence.Table 都没设置表名！！！"));
                }
                if (tableList.contains(tableName)) {
                    throw new RuntimeException(StrUtil.format("[{}] 表名重复", tableName));
                }
                tableName = AcTableUtils.handleKeyword(tableName);
                tableList.add(tableName);
                builder.name(tableName);
                builder.comment(judgeIsNull(comment));
                getFieldInfo(cls, propertyInfoList, indexInfoList,
                        uniqueInfoList, keyList, propertyList, acTable,
                        null, turn, acTableProperties);
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
     * @param acTable
     * @param excludeSuperField
     * @param turn
     * @return
     */
    private static void getFieldInfo(Class<?> cls, List<TableInfo.PropertyInfo> propertyInfoList,
                                     List<TableInfo.IndexInfo> indexInfoList,
                                     List<TableInfo.UniqueInfo> uniqueInfoList,
                                     List<String> keyList,
                                     List<String> propertyList,
                                     AcTable acTable,
                                     ExcludeSuperField excludeSuperField,
                                     TurnEnums turn,
                                     AcTableProperties acTableProperties) {
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

            AcColumn acColumn = field.getAnnotation(AcColumn.class);
            //swagger兼容
            ApiModelProperty apiModelProperty = field.getAnnotation(ApiModelProperty.class);
            //mybatis plus 兼容
            TableField tableField = field.getAnnotation(TableField.class);
            TableId tableId = field.getAnnotation(TableId.class);
            //hibernate 兼容
            Column column = field.getAnnotation(Column.class);
            Id id = field.getAnnotation(Id.class);
            GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
            Transient transientAnn = field.getAnnotation(Transient.class);
            if (Objects.isNull(acColumn)) {
                if ((Objects.nonNull(tableField) && !tableField.exist())
                        || Objects.nonNull(transientAnn)) {
                    continue;
                }
                if (Objects.nonNull(tableField)) {
                    columnName = tableField.value();
                }
                if (Objects.nonNull(column)) {
                    columnName = column.name();
                }
                columnName = StrUtil.isBlank(columnName) ? fieldNameTurnDatabaseColumn(fieldName, turn, acTable) : columnName;
                if (propertyList.contains(columnName)) {
                    throw new RuntimeException(StrUtil.format(COLUMN_DUPLICATE_VALID_STR, fieldName));
                }
                propertyList.add(columnName);
                boolean isKey = Objects.nonNull(tableId) || Objects.nonNull(id);
                boolean isAutoIncrement = (Objects.nonNull(tableId) && Objects.equals(tableId.type(), IdType.AUTO))
                        || (Objects.nonNull(generatedValue) && Objects.equals(generatedValue.strategy(), GenerationType.IDENTITY));
                String columnComment = Objects.nonNull(apiModelProperty) && StrUtil.isNotBlank(apiModelProperty.value()) ? apiModelProperty.value() : null;
                propertyInfoBuilder.columnName(columnName)
                        .columnComment(columnComment)
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
                if ((Objects.nonNull(tableField) && !tableField.exist())
                        || Objects.nonNull(transientAnn)
                        || acColumn.exclude()) {
                    continue;
                }
                if (StrUtil.isNotBlank(acColumn.name())) {
                    columnName = acColumn.name();
                }
                if (Objects.nonNull(tableField)) {
                    columnName = tableField.value();
                }
                if (Objects.nonNull(column)) {
                    columnName = column.name();
                }
                columnName = AcTableUtils.handleKeyword(StrUtil.isBlank(columnName) ? fieldNameTurnDatabaseColumn(fieldName, turn, acTable) : columnName);
                if (propertyList.contains(columnName)) {
                    throw new RuntimeException(StrUtil.format(COLUMN_DUPLICATE_VALID_STR, fieldName));
                }
                propertyList.add(columnName);

                boolean isKey = Objects.nonNull(tableId) || Objects.nonNull(id) || acColumn.isKey();
                boolean isAutoIncrement = acColumn.isAutoIncrement()
                        || (Objects.nonNull(tableId) && Objects.equals(tableId.type(), IdType.AUTO))
                        || (Objects.nonNull(generatedValue) && Objects.equals(generatedValue.strategy(), GenerationType.IDENTITY));
                String columnComment = Objects.nonNull(apiModelProperty) && StrUtil.isNotBlank(apiModelProperty.value()) ? apiModelProperty.value() : judgeIsNull(acColumn.comment());
                propertyInfoBuilder.columnName(columnName)
                        .columnComment(columnComment)
                        .decimalLength(acColumn.decimalLength())
                        .defaultValue(judgeIsNull(acColumn.defaultValue()))
                        .isAutoIncrement(isAutoIncrement)
                        .isKey(isKey)
                        .isNull(acColumn.isNull())
                        .length(acColumn.length())
                        .type(getTypeStr(field.getType().getName(), acColumn.type()));
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
                        .columns(ArrayUtil.isEmpty(columns) ? new String[]{fieldNameTurnDatabaseColumn(fieldName, turn, acTable)} : columns).build();
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
                        .columns(ArrayUtil.isEmpty(columns) ? new String[]{fieldNameTurnDatabaseColumn(fieldName, turn, acTable)} : columns).build();
                uniqueInfoList.add(uniqueInfo);
            }

        }
        Class<?> superclass = cls.getSuperclass();
        if (Objects.isNull(superclass)) {
            return;
        }
        getFieldInfo(superclass, propertyInfoList, indexInfoList,
                uniqueInfoList, keyList, propertyList, acTable,
                cls.getAnnotation(ExcludeSuperField.class), turn, acTableProperties);
    }

    /**
     * java字段名转数据库列
     *
     * @param fieldName
     * @param turn
     * @return
     */
    private static String fieldNameTurnDatabaseColumn(String fieldName, TurnEnums turn, AcTable acTable) {
        TurnEnums columnTurn = Objects.nonNull(acTable) ? acTable.turn() : TurnEnums.DEFAULT;
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
