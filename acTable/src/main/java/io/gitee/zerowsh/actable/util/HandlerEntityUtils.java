package io.gitee.zerowsh.actable.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.gitee.zerowsh.actable.annotation.*;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.TurnEnums;
import io.gitee.zerowsh.actable.properties.AcTableProperties;
import io.gitee.zerowsh.actable.service.DatabaseService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.*;
import static io.gitee.zerowsh.actable.constant.StringConstants.COMMA;
import static io.gitee.zerowsh.actable.constant.StringConstants.CONVERT_STR;

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
    public static List<TableInfo> getTableInfoByEntityPackage(AcTableProperties acTableProperties, String databaseType) {
        DatabaseService databaseService = AcTableUtils.getDatabaseService(databaseType);
        String entityPackage = acTableProperties.getEntityPackage();
        Boolean tableToUpperCase = acTableProperties.getTableToUpperCase();
        Boolean columnToUpperCase = acTableProperties.getColumnToUpperCase();
        //实体类表信息
        List<TableInfo> tableInfoList = new ArrayList<>();
        //用来判断是否有重复表名
        Set<String> tableJudge = new HashSet<>();
        for (String s : entityPackage.split(COMMA)) {
            Set<BeanDefinition> acTableBeanDefinitions = AcTableUtils.scanPackageByAnnotation(s, AcTable.class);
            //mybatis plus兼容
            Set<BeanDefinition> tableNameBeanDefinitions = AcTableUtils.scanPackageByAnnotation(s, TableName.class);
            //hibernate 兼容
            Set<BeanDefinition> tableBeanDefinitions = AcTableUtils.scanPackageByAnnotation(s, Table.class);
            Set<BeanDefinition> tableSet = new HashSet<>();
            tableSet.addAll(acTableBeanDefinitions);
            tableSet.addAll(tableNameBeanDefinitions);
            tableSet.addAll(tableBeanDefinitions);
            for (BeanDefinition beanDefinition : tableSet) {
                Class<?> cls;
                try {
                    // 使用反射加载类并输出类名
                    cls = Class.forName(beanDefinition.getBeanClassName());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("未找到实例对象！");
                }
                if (Objects.nonNull(cls.getAnnotation(IgnoreTable.class))) {
                    continue;
                }
                TableInfo.TableInfoBuilder builder = TableInfo.builder();
                List<TableInfo.PropertyInfo> propertyInfoList = new ArrayList<>();
                List<TableInfo.IndexInfo> indexInfoList = new ArrayList<>();
                List<TableInfo.UniqueInfo> uniqueInfoList = new ArrayList<>();
                List<String> keyList = new ArrayList<>();
                List<String> propertyList = new ArrayList<>();
                //定义表名
                String tableName = null;
                //定义表注释
                String comment = DEFAULT_VALUE;
                AcTable acTable = cls.getAnnotation(AcTable.class);
                if (Objects.nonNull(acTable)) {
                    tableName = acTable.name();
                    comment = acTable.comment();
                }
                if (Objects.equals(comment, DEFAULT_VALUE)) {
                    //swagger 兼容获取表注释
                    ApiModel apiModel = cls.getAnnotation(ApiModel.class);
                    if (Objects.nonNull(apiModel)) {
                        comment = apiModel.value();
                    }
                }
                if (Objects.isNull(tableName)) {
                    //mybatis plus兼容
                    TableName mpTable = cls.getAnnotation(TableName.class);
                    if (Objects.nonNull(mpTable)) {
                        tableName = mpTable.value();
                    }
                }
                if (Objects.isNull(tableName)) {
                    //hibernate 兼容
                    Table jpaTable = cls.getAnnotation(Table.class);
                    if (Objects.nonNull(jpaTable)) {
                        tableName = jpaTable.name();
                    }
                }
                if (StrUtil.isBlank(tableName)) {
                    throw new RuntimeException(StrUtil.format("io.gitee.zerowsh.actable.annotation.AcTable、com.baomidou.mybatisplus.annotation.TableName、javax.persistence.Table 注解都没设置表名！"));
                }
                if (tableJudge.contains(tableName)) {
                    throw new RuntimeException(StrUtil.format("[{}] 表名重复", tableName));
                }
                tableName = tableToUpperCase ? databaseService.handleKeyword(tableName).toUpperCase() : databaseService.handleKeyword(tableName);
                tableJudge.add(tableName);
                //设置表名
                builder.name(tableName);
                builder.comment(judgeIsNull(comment));
                HashMap<String, String> updateColumnNameMap = new HashMap();

                UpdateColumnName updateColumnName = cls.getAnnotation(UpdateColumnName.class);
                //需要修改的字段
                if (Objects.nonNull(updateColumnName)) {
                    String[] value = updateColumnName.value();
                    if (ArrayUtil.isNotEmpty(value)) {
                        for (String updateColumnNameStr : value) {
                            String[] split = updateColumnNameStr.split(CONVERT_STR);
                            updateColumnNameMap.put(split[1], split[0]);
                        }
                    }
                }
                getFieldInfo(cls, propertyInfoList, indexInfoList,
                        uniqueInfoList, propertyList, acTable, updateColumnNameMap,
                        null, acTableProperties, databaseService);
                if (CollectionUtil.isEmpty(propertyInfoList)) {
                    throw new RuntimeException(StrUtil.format("类 [{}] 不存在字段信息", cls.getName()));
                }
                //通过order字段正序排序
                propertyInfoList.sort(Comparator.comparing(TableInfo.PropertyInfo::getOrder));
                for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
                    if (propertyInfo.isKey()) {
                        keyList.add(propertyInfo.getColumnName());
                    }
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
     * @param propertyList      判断类中是否有重复字段
     * @param acTable
     * @param updateColumnName
     * @param excludeSuperField
     * @param turn
     * @return
     */
    private static void getFieldInfo(Class<?> cls, List<TableInfo.PropertyInfo> propertyInfoList,
                                     List<TableInfo.IndexInfo> indexInfoList,
                                     List<TableInfo.UniqueInfo> uniqueInfoList,
                                     List<String> propertyList,
                                     AcTable acTable,
                                     HashMap<String, String> updateColumnNameMap,
                                     ExcludeSuperField excludeSuperField,
                                     AcTableProperties acTableProperties,
                                     DatabaseService databaseService) {
        TurnEnums turn = acTableProperties.getTurn();
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
            //swagger 兼容
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
                //从其他注解获取
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
                    throw new RuntimeException(StrUtil.format(COLUMN_DUPLICATE_VALID_STR, cls.getName(), fieldName));
                }
                propertyList.add(columnName);
                boolean isKey = Objects.nonNull(tableId) || Objects.nonNull(id);
                boolean isAutoIncrement = (Objects.nonNull(tableId) && Objects.equals(tableId.type(), IdType.AUTO))
                        || (Objects.nonNull(generatedValue) && Objects.equals(generatedValue.strategy(), GenerationType.IDENTITY));
                String columnComment = Objects.nonNull(apiModelProperty) && StrUtil.isNotBlank(apiModelProperty.value()) ? apiModelProperty.value() : null;
                propertyInfoBuilder.columnComment(columnComment)
                        .decimalLength(COLUMN_DECIMAL_LENGTH_DEF)
                        .isNull(COLUMN_IS_NULL_DEF)
                        .isKey(isKey)
                        .isAutoIncrement(isAutoIncrement)
                        .length(COLUMN_LENGTH_DEF)
                        .type(databaseService.javaTypeTurnColumnType(field.getType().getName()));
            } else {
                //从自定义注解获取
                if ((Objects.nonNull(tableField) && !tableField.exist())
                        || Objects.nonNull(transientAnn)
                        || acColumn.exclude()) {
                    continue;
                }
                columnName = acColumn.name();
                if (Objects.nonNull(tableField) && StrUtil.isBlank(columnName)) {
                    columnName = tableField.value();
                }
                if (Objects.nonNull(column) && StrUtil.isBlank(columnName)) {
                    columnName = column.name();
                }
                columnName = databaseService.handleKeyword(StrUtil.isBlank(columnName) ? fieldNameTurnDatabaseColumn(fieldName, turn, acTable) : columnName);
                if (propertyList.contains(columnName)) {
                    throw new RuntimeException(StrUtil.format(COLUMN_DUPLICATE_VALID_STR, cls.getName(), fieldName));
                }
                propertyList.add(columnName);

                boolean isKey = Objects.nonNull(tableId) || Objects.nonNull(id) || acColumn.isKey();
                boolean isAutoIncrement = acColumn.isAutoIncrement()
                        || (Objects.nonNull(tableId) && Objects.equals(tableId.type(), IdType.AUTO))
                        || (Objects.nonNull(generatedValue) && Objects.equals(generatedValue.strategy(), GenerationType.IDENTITY));
                String columnComment = Objects.nonNull(apiModelProperty) && StrUtil.isNotBlank(apiModelProperty.value()) ? apiModelProperty.value() : judgeIsNull(acColumn.comment());
                propertyInfoBuilder.oldColumnName(StrUtil.isNotBlank(acColumn.oldName()) ? acColumn.oldName() : updateColumnNameMap.get(columnName))
                        .columnComment(columnComment)
                        .decimalLength(acColumn.decimalLength())
                        .defaultValue(judgeIsNull(acColumn.defaultValue()))
                        .isAutoIncrement(isAutoIncrement)
                        .isKey(isKey)
                        .order(acColumn.order())
                        .isNull(acColumn.isNull())
                        .length(acColumn.length())
                        .type(databaseService.javaTypeTurnColumnType(field.getType().getName(), acColumn.type()));
            }
            if (acTableProperties.getColumnToUpperCase()) {
                columnName = columnName.toUpperCase();
            }
            propertyInfoList.add(propertyInfoBuilder.columnName(columnName).build());

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
                uniqueInfoList, propertyList, acTable, updateColumnNameMap,
                cls.getAnnotation(ExcludeSuperField.class), acTableProperties, databaseService);
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
}
