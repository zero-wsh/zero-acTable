package io.gitee.zerowsh.actable.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.gitee.zerowsh.actable.annotation.*;
import io.gitee.zerowsh.actable.constant.AcTableConstants;
import io.gitee.zerowsh.actable.dto.AcHistoryTable;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.HistoryEnums;
import io.gitee.zerowsh.actable.emnus.TurnEnums;
import io.gitee.zerowsh.actable.properties.AcTableProperties;
import io.gitee.zerowsh.actable.service.DatabaseService;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static io.gitee.zerowsh.actable.annotation.Index.IndexEnums.IDX;
import static io.gitee.zerowsh.actable.annotation.Index.IndexEnums.UK_IDX;
import static io.gitee.zerowsh.actable.constant.AcTableConstants.*;
import static io.gitee.zerowsh.actable.constant.StringConstants.CONVERT_STR;

/**
 * 处理实体工具类，通过实体类获取表、字段信息
 *
 * @author zero
 */
@SuppressWarnings("all")
@Slf4j
public class HandlerEntityUtils {


    public static Set<BeanDefinition> scanPackageByAnnotation(String basePackage, Class<? extends Annotation> annotationClass) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // 添加包含过滤条件，只扫描带有特定注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(annotationClass));
        // 执行扫描并获取结果
        return scanner.findCandidateComponents(basePackage);
    }

    /**
     * @param acTableProperties
     * @param databaseService
     * @return 通过实体类包名获取所有表信息 (字段 索引 唯一值等)
     */
    public static List<TableInfo> getTableInfoByEntityPackage(AcTableProperties acTableProperties,
                                                              DatabaseService databaseService) {
        String entityPackage = acTableProperties.getEntityPackage();
        //实体类表信息
        List<TableInfo> tableInfoList = new ArrayList<>();
        //用来判断是否有重复表名
        Set<String> tableJudge = new HashSet<>();

        for (String s : entityPackage.split(StrUtil.COMMA)) {
            Set<BeanDefinition> tableSet = new HashSet<>();
            Set<BeanDefinition> acTableBeanDefinitions = scanPackageByAnnotation(s, AcTable.class);
            tableSet.addAll(acTableBeanDefinitions);
            if (AnnotationUtils.isAnnotationPresent(() -> TableName.class)) {
                //mybatis plus兼容
                Set<BeanDefinition> tableNameBeanDefinitions = scanPackageByAnnotation(s, TableName.class);
                tableSet.addAll(tableNameBeanDefinitions);
            }
            if (AnnotationUtils.isAnnotationPresent(() -> Table.class)) {
                //hibernate 兼容
                Set<BeanDefinition> tableBeanDefinitions = scanPackageByAnnotation(s, Table.class);
                tableSet.addAll(tableBeanDefinitions);
            }
            for (BeanDefinition beanDefinition : tableSet) {
                Class<?> cls;
                try {
                    // 使用反射加载类并输出类名
                    cls = Class.forName(beanDefinition.getBeanClassName());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("未找到实例对象！");
                }
                handleTableInfo(cls, tableJudge, acTableProperties, databaseService, tableInfoList);
            }
        }

        if (!Objects.equals(acTableProperties.getHistory(), HistoryEnums.NONE)) {
            handleTableInfo(AcHistoryTable.class, tableJudge, acTableProperties, databaseService, tableInfoList);
        }
        return tableInfoList;

    }

    public static void handleTableInfo(Class<?> cls,
                                       Set<String> tableJudge,
                                       AcTableProperties acTableProperties,
                                       DatabaseService databaseService,
                                       List<TableInfo> tableInfoList) {

        if (Objects.nonNull(cls.getAnnotation(IgnoreTable.class))) {
            return;
        }
        AcTable acTable = cls.getAnnotation(AcTable.class);
        if (Objects.nonNull(acTable) && acTable.exclude()) {
            return;
        }

        TableInfo.TableInfoBuilder builder = TableInfo.builder();
        List<TableInfo.PropertyInfo> propertyInfoList = new ArrayList<>();
        //key=实体属性名称 value=数据库字段名
        Map<String, String> propertyMap = new HashMap<>();
        List<TableInfo.IndexInfo> indexInfoList = new ArrayList<>();
        List<TableInfo.UniqueIndexInfo> uniqueIndexInfoList = new ArrayList<>();
        List<TableInfo.UniqueInfo> uniqueInfoList = new ArrayList<>();
        List<String> keyList = new ArrayList<>();
        List<String> propertyList = new ArrayList<>();
        //定义表名
        String tableName = null;
        //定义表注释
        String comment = DEFAULT_VALUE;
        if (Objects.nonNull(acTable)) {
            tableName = acTable.name();
            comment = acTable.comment();
        }
        if (Objects.equals(comment, DEFAULT_VALUE)) {
            //swagger 兼容获取表注释
            ApiModel apiModel = AnnotationUtils.getAnnotationClassSafe(cls, () -> ApiModel.class);
            if (Objects.nonNull(apiModel)) {
                comment = apiModel.value();
            }
        }
        if (StrUtil.isBlank(tableName)) {
            //mybatis plus兼容
            TableName mpTable = AnnotationUtils.getAnnotationClassSafe(cls, () -> TableName.class);
            if (Objects.nonNull(mpTable)) {
                tableName = mpTable.value();
            }
        }
        if (StrUtil.isBlank(tableName)) {
            //hibernate 兼容
            Table jpaTable = AnnotationUtils.getAnnotationClassSafe(cls, () -> Table.class);
            if (Objects.nonNull(jpaTable)) {
                tableName = jpaTable.name();
            }
        }
        if (StrUtil.isBlank(tableName)) {
            throw new RuntimeException(StrUtil.format("io.gitee.zerowsh.actable.annotation.AcTable、com.baomidou.mybatisplus.annotation.TableName、javax.persistence.Table 注解都没设置表名！"));
        }
        if (tableJudge.contains(tableName)) {
            throw new RuntimeException(StrUtil.format("【{}】 表名重复！", tableName));
        }
        tableName = acTableProperties.getTableToUpperCase() ? databaseService.delKeywordHandle(tableName).toUpperCase() : databaseService.delKeywordHandle(tableName);
        tableJudge.add(tableName);
        //设置表名
        builder.name(tableName);
        String tableComment = judgeIsNull(comment);
        builder.comment(StrUtil.isBlank(tableComment) ? "" : tableComment);
        getFieldInfo(cls, propertyInfoList, propertyMap, indexInfoList, uniqueIndexInfoList,
                uniqueInfoList, propertyList, acTable,
                null, acTableProperties, databaseService, tableName);


        //定义在类上的修改字段注解
        HashMap<String, String> updateColumnNameMap = new HashMap();
        UpdateColumnName updateColumnName = cls.getAnnotation(UpdateColumnName.class);
        //需要修改的字段
        if (Objects.nonNull(updateColumnName)) {
            String[] value = updateColumnName.value();
            if (ArrayUtil.isNotEmpty(value)) {
                for (String updateColumnNameStr : value) {
                    String[] split = updateColumnNameStr.split(CONVERT_STR);
                    String column = propertyMap.get(split[0]);
                    if (StrUtil.isBlank(column)) {
                        throw new RuntimeException(StrUtil.format("找不到表【{}】实体类中有【{}】属性，请检查@UpdateColumnName注解！",
                                tableName, split[0]));
                    }
                    if (acTableProperties.getColumnToUpperCase()) {
                        column = column.toUpperCase();
                    }
                    updateColumnNameMap.put(column, split[1]);
                }
            }
        }
        if (CollectionUtil.isEmpty(propertyInfoList)) {
            throw new RuntimeException(StrUtil.format("类【{}】不存在字段信息！", cls.getName()));
        }

        //通过order字段正序排序
        propertyInfoList.sort(Comparator.comparing(TableInfo.PropertyInfo::getOrder));
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            String columnName = propertyInfo.getColumnName();
            if (propertyInfo.isKey()) {
                keyList.add(columnName);
            } else if (databaseService.autoincrementIsPk() && propertyInfo.isAutoIncrement()) {
                //mysql数据库是自增就必须是主键
                keyList.add(columnName);
                propertyInfo.setKey(true);
            }

            if (Objects.equals(columnName, propertyInfo.getOldColumnName())) {
                String newColumnName = updateColumnNameMap.get(columnName);
                if (StrUtil.isNotBlank(newColumnName)) {
                    propertyInfo.setOldColumnName(columnName);
                    propertyInfo.setColumnName(newColumnName);
                }
            }
        }

        //处理索引、唯一索引、唯一约束
        handleUkAndIdx(acTableProperties, propertyMap,
                indexInfoList,
                uniqueIndexInfoList,
                uniqueInfoList,
                cls,
                tableName);

        TableInfo tableInfo = builder.keyList(keyList)
                .propertyInfoList(propertyInfoList)
                .indexInfoList(indexInfoList)
                .uniqueIndexInfoList(uniqueIndexInfoList)
                .uniqueInfoList(uniqueInfoList)
                .build();
        tableInfoList.add(tableInfo);
    }

    public static void handleUkAndIdx(AcTableProperties acTableProperties, Map<String, String> propertyMap,
                                      List<TableInfo.IndexInfo> indexInfoList,
                                      List<TableInfo.UniqueIndexInfo> uniqueIndexInfoList,
                                      List<TableInfo.UniqueInfo> uniqueInfoList, Class<?> cls, String tableName) {
        handleUkAndIdxColumn(acTableProperties.getColumnToUpperCase(), propertyMap,
                indexInfoList,
                uniqueIndexInfoList,
                uniqueInfoList,
                cls.getAnnotation(Index.class),
                tableName,
                acTableProperties.getTurn());

        IndexArr indexArr = cls.getAnnotation(IndexArr.class);
        if (Objects.nonNull(indexArr)) {
            Index[] valueArr = indexArr.value();
            if (ArrayUtil.isNotEmpty(valueArr)) {
                for (Index index : valueArr) {
                    handleUkAndIdxColumn(acTableProperties.getColumnToUpperCase(), propertyMap,
                            indexInfoList,
                            uniqueIndexInfoList,
                            uniqueInfoList,
                            index, tableName,
                            acTableProperties.getTurn());
                }
            }
        }
        //处理父类
        Class<?> superclass = cls.getSuperclass();
        if (Objects.nonNull(superclass)) {
            handleUkAndIdx(acTableProperties, propertyMap,
                    indexInfoList,
                    uniqueIndexInfoList,
                    uniqueInfoList,
                    superclass,
                    tableName);
        }
    }

    /**
     * @param comment
     * @return 判断是否为null
     */
    private static String judgeIsNull(String comment) {
        return Objects.equals(comment, DEFAULT_VALUE) ? null : comment;
    }

    /**
     * 递归获取字段信息
     *
     * @param cls
     * @param propertyInfoList
     * @param propertyMap
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
                                     Map<String, String> propertyMap,
                                     List<TableInfo.IndexInfo> indexInfoList,
                                     List<TableInfo.UniqueIndexInfo> uniqueIndexInfoList,
                                     List<TableInfo.UniqueInfo> uniqueInfoList,
                                     List<String> propertyList,
                                     AcTable acTable,
                                     ExcludeSuperField excludeSuperField,
                                     AcTableProperties acTableProperties,
                                     DatabaseService databaseService,
                                     String tableName) {
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
            ApiModelProperty apiModelProperty = AnnotationUtils.getAnnotationSafe(field, () -> ApiModelProperty.class);

            //mybatis plus 兼容
            TableField tableField = AnnotationUtils.getAnnotationSafe(field, () -> TableField.class);
            TableId tableId = AnnotationUtils.getAnnotationSafe(field, () -> TableId.class);
            //hibernate 兼容
            Column column = AnnotationUtils.getAnnotationSafe(field, () -> Column.class);
            Id id = AnnotationUtils.getAnnotationSafe(field, () -> Id.class);
            GeneratedValue generatedValue = AnnotationUtils.getAnnotationSafe(field, () -> GeneratedValue.class);
            Transient transientAnn = AnnotationUtils.getAnnotationSafe(field, () -> Transient.class);
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
                    throw new RuntimeException(StrUtil.format(COLUMN_DUPLICATE_VALID_STR, tableName, fieldName));
                }
                propertyList.add(columnName);
                boolean isKey = Objects.nonNull(tableId) || Objects.nonNull(id);
                boolean isAutoIncrement = (Objects.nonNull(tableId) && Objects.equals(tableId.type(), IdType.AUTO))
                        || (Objects.nonNull(generatedValue) && Objects.equals(generatedValue.strategy(), GenerationType.IDENTITY));
                //只能从swagger注解上取值
                String columnComment = Objects.nonNull(apiModelProperty) && StrUtil.isNotBlank(apiModelProperty.value()) ? apiModelProperty.value() : null;
                if (acTableProperties.getColumnToUpperCase()) {
                    columnName = columnName.toUpperCase();
                }
                boolean isNull = AcTableConstants.COLUMN_IS_NULL_DEF;
                //是否为空，优先使用自定义注解
                if (isKey || isAutoIncrement) {
                    isNull = false;
                } else {
                    if (Objects.nonNull(apiModelProperty)) {
                        isNull = apiModelProperty.required();
                    }
                }
                propertyInfoBuilder.columnName(columnName)
                        .oldColumnName(columnName)
                        .tableName(tableName)
                        .columnComment(StrUtil.isBlank(columnComment) ? "" : columnComment)
                        .decimalLength(COLUMN_DECIMAL_LENGTH_DEF)
                        .isNull(isNull)
                        .isKey(isKey)
                        .isAutoIncrement(isAutoIncrement)
                        .length(COLUMN_LENGTH_DEF)
                        .type(databaseService.javaTypeTurnColumnType(field.getType().getName()))
                        .typeLimit(true);
            } else {
                //从自定义注解获取
                if ((Objects.nonNull(tableField) && !tableField.exist())
                        || Objects.nonNull(transientAnn)
                        || acColumn.exclude()) {
                    continue;
                }
                //列名，优先使用自定义注解
                columnName = acColumn.value();
                if (Objects.nonNull(tableField) && StrUtil.isBlank(columnName)) {
                    columnName = tableField.value();
                }
                if (Objects.nonNull(column) && StrUtil.isBlank(columnName)) {
                    columnName = column.name();
                }
                columnName = databaseService.delKeywordHandle(StrUtil.isBlank(columnName) ? fieldNameTurnDatabaseColumn(fieldName, turn, acTable) : columnName);
                if (propertyList.contains(columnName)) {
                    throw new RuntimeException(StrUtil.format(COLUMN_DUPLICATE_VALID_STR, tableName, fieldName));
                }
                propertyList.add(columnName);

                boolean isKey = Objects.nonNull(tableId) || Objects.nonNull(id) || acColumn.isKey();
                boolean isAutoIncrement = acColumn.isAutoIncrement()
                        || (Objects.nonNull(tableId) && Objects.equals(tableId.type(), IdType.AUTO))
                        || (Objects.nonNull(generatedValue) && Objects.equals(generatedValue.strategy(), GenerationType.IDENTITY));
                //先判断自定义注解是否有注释
                String columnComment = judgeIsNull(acColumn.comment());
                if (StrUtil.isBlank(columnComment)) {
                    if (Objects.nonNull(apiModelProperty) && StrUtil.isNotBlank(apiModelProperty.value())) {
                        columnComment = apiModelProperty.value();
                    }
                }
                if (acTableProperties.getColumnToUpperCase()) {
                    columnName = columnName.toUpperCase();
                }
                String oldColumnName = StrUtil.isNotBlank(acColumn.oldName()) ? acColumn.oldName() : columnName;
                boolean isNull = AcTableConstants.COLUMN_IS_NULL_DEF;
                //是否为空，优先使用自定义注解
                if (isKey || isAutoIncrement) {
                    isNull = false;
                } else {
                    //不能为空
                    if (!acColumn.isNull()) {
                        isNull = false;
                    } else {
                        if (Objects.nonNull(apiModelProperty)) {
                            isNull = apiModelProperty.required();
                        }
                    }
                }
                propertyInfoBuilder.columnName(columnName)
                        .oldColumnName(oldColumnName)
                        .tableName(tableName)
                        .columnComment(StrUtil.isBlank(columnComment) ? "" : columnComment)
                        .decimalLength(acColumn.decimalLength())
                        .defaultValue(judgeIsNull(acColumn.defaultValue()))
                        .isAutoIncrement(isAutoIncrement)
                        .isKey(isKey)
                        .order(acColumn.order())
                        .isNull(isNull)
                        .length(acColumn.length())
                        .type(databaseService.javaTypeTurnColumnType(field.getType().getName(), acColumn.type()))
                        .typeLimit(acColumn.typeLimit());
            }
            propertyInfoList.add(propertyInfoBuilder.build());
            propertyMap.put(fieldName, columnName);
        }
        Class<?> superclass = cls.getSuperclass();
        if (Objects.nonNull(superclass)) {
            getFieldInfo(superclass, propertyInfoList, propertyMap, indexInfoList, uniqueIndexInfoList,
                    uniqueInfoList, propertyList, acTable,
                    cls.getAnnotation(ExcludeSuperField.class), acTableProperties, databaseService, tableName);
        }
    }

    public static void handleUkAndIdxColumn(Boolean columnToUpperCase, Map<String, String> propertyMap,
                                            List<TableInfo.IndexInfo> indexInfoList,
                                            List<TableInfo.UniqueIndexInfo> uniqueIndexInfoList,
                                            List<TableInfo.UniqueInfo> uniqueInfoList,
                                            Index index,
                                            String tableName,
                                            TurnEnums turn) {
        List<TableInfo.Index> tableInfoList = new ArrayList<>();
        if (Objects.nonNull(index)) {
            IndexColumn[] columnArr = index.columnArr();
            if (ArrayUtil.isNotEmpty(columnArr)) {
                for (IndexColumn indexColumn : columnArr) {
                    String column = propertyMap.get(indexColumn.value());
                    if (StrUtil.isBlank(column)) {
                        throw new RuntimeException(StrUtil.format("找不到表【{}】实体类中有【{}】属性，请检查@IndexColumn注解！",
                                tableName, indexColumn.value()));
                    }
                    if (columnToUpperCase) {
                        column = column.toUpperCase();
                    }
                    tableInfoList.add(TableInfo.Index.builder()
                            .column(column)
                            .asc(indexColumn.asc()).build());
                }
                String value = index.value();
                switch (index.type()) {
                    case IDX:
                        TableInfo.IndexInfo indexInfo = TableInfo.IndexInfo.builder()
                                .value(IDX.getPrefix() + value)
                                .columns(tableInfoList).build();
                        indexInfoList.add(indexInfo);
                        break;
                    case UK_IDX:
                        TableInfo.UniqueIndexInfo uniqueIndexInfo = TableInfo.UniqueIndexInfo.builder()
                                .value(UK_IDX.getPrefix() + value)
                                .columns(tableInfoList).build();
                        uniqueIndexInfoList.add(uniqueIndexInfo);
                        break;
                    case UK:
                        TableInfo.UniqueInfo uniqueInfo = TableInfo.UniqueInfo.builder()
                                .value(Index.IndexEnums.UK.getPrefix() + value)
                                .columns(tableInfoList).build();
                        uniqueInfoList.add(uniqueInfo);
                        break;
                    default:
                }
            }
        }
    }

    public static String[] toUpperCase(String[] columns) {
        for (int i = 0; i < columns.length; i++) {
            columns[i] = columns[i].toUpperCase();
        }
        return columns;
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
            //取全局的
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
