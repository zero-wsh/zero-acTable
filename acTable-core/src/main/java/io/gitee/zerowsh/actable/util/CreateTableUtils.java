package io.gitee.zerowsh.actable.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import io.gitee.zerowsh.actable.annotation.*;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.emnus.TurnEnums;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

import static io.gitee.zerowsh.actable.constant.CreateTableConstants.*;

/**
 * 创建表工具类
 *
 * @author zero
 */
@Slf4j
public class CreateTableUtils {

    /**
     * 获取表信息 (字段 索引 唯一值)
     *
     * @param cls
     * @param tableList
     * @param tableInfoList
     */
    public static void getTableInfoByClass(Class<?> cls, List<String> tableList, List<TableInfo> tableInfoList, TurnEnums turn) {
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
        tableList.add(tableName);
        builder.name(tableName);
        builder.comment(judgeIsNull(comment));
        getFieldInfo(cls, propertyInfoList, indexInfoList, uniqueInfoList, keyList, propertyList, table, null, turn);
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

    /**
     * 判断是否为null
     *
     * @param comment
     * @return
     */
    public static String judgeIsNull(String comment) {
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
                                     TurnEnums turn) {
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

            Column column = field.getAnnotation(Column.class);
            TableField tableField = field.getAnnotation(TableField.class);
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
                propertyInfoBuilder.columnName(columnName)
                        .decimalLength(COLUMN_DECIMAL_LENGTH_DEF)
                        .isNull(COLUMN_IS_NULL_DEF)
                        .length(COLUMN_LENGTH_DEF)
                        .type(getJavaTurnDatabaseValue(field.getType().getName()));
            } else {
                if (Objects.nonNull(tableField) && !tableField.exist()) {
                    continue;
                }
                boolean exclude = column.exclude();
                if (exclude) {
                    continue;
                }
                if (StrUtil.isNotBlank(column.name())) {
                    columnName = column.name();
                }
                if (Objects.nonNull(tableField)) {
                    columnName = tableField.value();
                }
                columnName = StrUtil.isBlank(columnName) ? fieldNameTurnDatabaseColumn(fieldName, turn, table) : columnName;
                ColumnTypeEnums type = column.type();
                if (propertyList.contains(columnName)) {
                    throw new RuntimeException(StrUtil.format(COLUMN_DUPLICATE_VALID_STR, fieldName));
                }
                propertyList.add(columnName);
                propertyInfoBuilder.columnName(columnName)
                        .columnComment(CreateTableUtils.judgeIsNull(column.comment()))
                        .decimalLength(column.decimalLength())
                        .defaultValue(CreateTableUtils.judgeIsNull(column.defaultValue()))
                        .isAutoIncrement(column.isAutoIncrement())
                        .isKey(column.isKey())
                        .isNull(column.isNull())
                        .length(column.length())
                        .type(Objects.equals(type, ColumnTypeEnums.DEFAULT)
                                ? getJavaTurnDatabaseValue(field.getType().getName()) : type);
                //处理主键
                if (column.isKey()) {
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
                uniqueInfoList, keyList, propertyList, table, cls.getAnnotation(ExcludeSuperField.class), turn);
    }

    /**
     * java字段名转数据库列
     *
     * @param fieldName
     * @param turn
     * @return
     */
    public static String fieldNameTurnDatabaseColumn(String fieldName, TurnEnums turn, Table table) {
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
     * 获取创建表sql
     * <p>
     * <p>
     * CREATE TABLE zero1 (id int primary key identity(1,1),name varchar(255)  NULL,zero int DEFAULT 0 NOT NULL,time datetime2(7)  NULL)
     * GO
     * --添加备注
     * EXEC sp_addextendedproperty 'MS_Description', N'22','SCHEMA', N'dbo','TABLE', N't_zero'
     * GO
     * EXEC sp_addextendedproperty 'MS_Description', N'主键','SCHEMA', N'dbo','TABLE', N'zero1','COLUMN', N'id'
     * GO
     * EXEC sp_addextendedproperty 'MS_Description', N'名称','SCHEMA', N'dbo','TABLE', N'zero1','COLUMN', N'name'
     * GO
     * EXEC sp_addextendedproperty 'MS_Description', N'Zero注释','SCHEMA', N'dbo','TABLE', N'zero1','COLUMN', N'zero'
     * GO
     * EXEC sp_addextendedproperty 'MS_Description', N'时间','SCHEMA', N'dbo','TABLE', N'zero1','COLUMN', N'time'
     * GO
     * -- 添加索引
     * CREATE NONCLUSTERED INDEX [idx_name] ON zero1 (name)
     * GO
     * --添加唯一键
     * ALTER TABLE zero1 add constraint uk_name unique (name)
     *
     * @param tableInfo
     * @return
     */
    public static List<String> getCreateTableSql(TableInfo tableInfo) {
        List<String> list = new ArrayList<>();
        List<String> addColumnCommentSqlList = new ArrayList<>();
        String name = tableInfo.getName();
        String comment = tableInfo.getComment();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        StringBuilder propertySb = new StringBuilder();
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            propertySb.append(StringPool.LEFT_SQ_BRACKET)
                    .append(propertyInfo.getColumnName())
                    .append(StringPool.RIGHT_SQ_BRACKET);
            splicingAddOrCreateColumnInfo(propertyInfo.getType(), propertySb, propertyInfo, name, addColumnCommentSqlList);
        }

        //建表
        list.add(StrUtil.format(CREATE_TABLE, name, propertySb.deleteCharAt(propertySb.length() - 1)));
        if (StrUtil.isNotBlank(comment)) {
            //表备注
            list.add(StrUtil.format(ADD_TABLE_COMMENT, comment, name));
        }
        list.addAll(addColumnCommentSqlList);
        //创建主键
        List<String> keySqlList = new ArrayList<>();
        List<String> keyList = tableInfo.getKeyList();
        if (CollectionUtil.isNotEmpty(keyList)) {
            StringBuilder keySb = new StringBuilder();
            for (String key : keyList) {
                keySb.append(StringPool.LEFT_SQ_BRACKET)
                        .append(key)
                        .append(StringPool.RIGHT_SQ_BRACKET)
                        .append(StringPool.COMMA);
            }
            keySqlList.add(StrUtil.format(CREATE_PRIMARY_KEY, tableInfo.getName(), PK_ + tableInfo.getName(), keySb.deleteCharAt(keySb.length() - 1)));
        }
        list.addAll(keySqlList);

        //唯一键
        List<String> uniqueSqlList = new ArrayList<>();
        List<TableInfo.UniqueInfo> uniqueInfoList = tableInfo.getUniqueInfoList();
        if (CollectionUtil.isNotEmpty(uniqueInfoList)) {
            for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
                String[] columns = uniqueInfo.getColumns();
                StringBuilder uniqueSb = new StringBuilder();
                for (String column : columns) {
                    uniqueSb.append(StringPool.LEFT_SQ_BRACKET)
                            .append(column)
                            .append(StringPool.RIGHT_SQ_BRACKET)
                            .append(StringPool.COMMA);
                }
                uniqueSqlList.add(StrUtil.format(CREATE_UNIQUE, tableInfo.getName(), uniqueInfo.getValue(), uniqueSb.deleteCharAt(uniqueSb.length() - 1)));
            }
        }
        list.addAll(uniqueSqlList);
        //索引
        List<String> indexSqlList = new ArrayList<>();
        List<TableInfo.IndexInfo> indexInfoList = tableInfo.getIndexInfoList();
        if (CollectionUtil.isNotEmpty(indexInfoList)) {
            for (TableInfo.IndexInfo indexInfo : indexInfoList) {
                String[] columns = indexInfo.getColumns();
                StringBuilder indexSb = new StringBuilder();
                for (String column : columns) {
                    indexSb.append(StringPool.LEFT_SQ_BRACKET)
                            .append(column)
                            .append(StringPool.RIGHT_SQ_BRACKET)
                            .append(StringPool.COMMA);
                }
                indexSqlList.add(StrUtil.format(CREATE_INDEX, indexInfo.getValue(), tableInfo.getName(), indexSb.deleteCharAt(indexSb.length() - 1)));
            }
        }
        list.addAll(indexSqlList);
        return list;
    }

    /**
     * 创建约束（主键 唯一值 索引）
     *
     * @param tableInfo
     * @param list
     */
    private static void createConstraint(TableInfo tableInfo,
                                         List<String> list,
                                         boolean pkFlag,
                                         boolean ukFlag,
                                         boolean idxFlag,
                                         List<String> zeroUkList,
                                         List<String> zeroIdxList) {

        if (pkFlag) {
            //创建主键
            List<String> keySqlList = new ArrayList<>();
            List<String> keyList = tableInfo.getKeyList();
            if (CollectionUtil.isNotEmpty(keyList)) {
                StringBuilder keySb = new StringBuilder();
                for (String key : keyList) {
                    keySb.append(StringPool.LEFT_SQ_BRACKET)
                            .append(key)
                            .append(StringPool.RIGHT_SQ_BRACKET)
                            .append(StringPool.COMMA);
                }
                keySqlList.add(StrUtil.format(CREATE_PRIMARY_KEY, tableInfo.getName(), PK_ + tableInfo.getName(), keySb.deleteCharAt(keySb.length() - 1)));
            }
            list.addAll(keySqlList);
        }


        if (ukFlag) {
            //唯一键
            List<String> uniqueSqlList = new ArrayList<>();
            List<TableInfo.UniqueInfo> uniqueInfoList = tableInfo.getUniqueInfoList();
            if (CollectionUtil.isNotEmpty(uniqueInfoList)) {
                for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
                    String[] columns = uniqueInfo.getColumns();
                    StringBuilder uniqueSb = new StringBuilder();
                    for (String column : columns) {
                        uniqueSb.append(StringPool.LEFT_SQ_BRACKET)
                                .append(column)
                                .append(StringPool.RIGHT_SQ_BRACKET)
                                .append(StringPool.COMMA);
                    }

                    if (!zeroUkList.contains(uniqueInfo.getValue())) {
                        uniqueSqlList.add(StrUtil.format(CREATE_UNIQUE, tableInfo.getName(), uniqueInfo.getValue(), uniqueSb.deleteCharAt(uniqueSb.length() - 1)));
                    }
                }
            }
            list.addAll(uniqueSqlList);
        }
        if (idxFlag) {
            //索引
            List<String> indexSqlList = new ArrayList<>();
            List<TableInfo.IndexInfo> indexInfoList = tableInfo.getIndexInfoList();
            if (CollectionUtil.isNotEmpty(indexInfoList)) {
                for (TableInfo.IndexInfo indexInfo : indexInfoList) {
                    String[] columns = indexInfo.getColumns();
                    StringBuilder indexSb = new StringBuilder();
                    for (String column : columns) {
                        indexSb.append(StringPool.LEFT_SQ_BRACKET)
                                .append(column)
                                .append(StringPool.RIGHT_SQ_BRACKET)
                                .append(StringPool.COMMA);
                    }

                    if (!zeroIdxList.contains(indexInfo.getValue())) {
                        indexSqlList.add(StrUtil.format(CREATE_INDEX, indexInfo.getValue(), tableInfo.getName(), indexSb.deleteCharAt(indexSb.length() - 1)));
                    }

                }
            }
            list.addAll(indexSqlList);
        }
    }

    /**
     * 处理表备注
     *
     * @param list
     * @param comment
     * @param tableComment
     * @param tableName
     */
    private static void handleTableComment(List<String> list, String comment, String tableComment, String tableName) {
        if (!Objects.equals(comment, tableComment)) {
            if (Objects.isNull(tableComment)) {
                //数据库为null新增备注
                list.add(StrUtil.format(ADD_TABLE_COMMENT, comment, tableName));
            } else {
                if (Objects.isNull(comment)) {
                    //字段null删除备注
                    list.add(StrUtil.format(DROP_TABLE_COMMENT, tableName));
                } else {
                    //修改备注
                    list.add(StrUtil.format(UPDATE_TABLE_COMMENT, comment, tableName));
                }
            }
        }
    }

    /**
     * 处理约束（主键 唯一键 索引 默认值）
     *
     * @param constraintInfoList
     * @param defaultInfoList
     */
    private static Map<String, Collection<String>> handleConstraint(String tableName,
                                                                    List<ConstraintInfo> constraintInfoList,
                                                                    List<ConstraintInfo> defaultInfoList,
                                                                    List<String> delUkList,
                                                                    List<String> delIdxList) {
        Map<String, Collection<String>> map = new HashMap<>();
        //删除所有约束（唯一键、主键、索引、默认值）
        List<String> delPkConstraintSqlList = new ArrayList<>();
        Set<String> delUkConstraintSqlSet = new HashSet<>();
        Set<String> delIdxConstraintSqlSet = new HashSet<>();
        List<String> delDefConstraintSqlList = new ArrayList<>();
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            Integer constraintFlag = constraintInfo.getConstraintFlag();
            String constraintName = constraintInfo.getConstraintName();
            switch (constraintFlag) {
                case PK:
                    //主键
                    delPkConstraintSqlList.add(StrUtil.format(DROP_CONSTRAINT, tableName, constraintName));
                    break;
                case UK:
                    //唯一键
                    delUkList.add(constraintName);
                    delUkConstraintSqlSet.add(StrUtil.format(DROP_CONSTRAINT, tableName, constraintName));
                    break;
                case INDEX:
                    //索引
                    delIdxList.add(constraintName);
                    delIdxConstraintSqlSet.add(StrUtil.format(DROP_INDEX, constraintName, tableName));
                    break;
            }
        }
        //删除默认值约束
        for (ConstraintInfo defaultInfo : defaultInfoList) {
            delDefConstraintSqlList.add(StrUtil.format(DROP_CONSTRAINT, tableName, defaultInfo.getConstraintName()));
        }
        map.put(DEL_PK_C_SQL, delPkConstraintSqlList);
        map.put(DEL_UK_C_SQL, delUkConstraintSqlSet);
        map.put(DEL_INDEX_C_SQL, delIdxConstraintSqlSet);
        map.put(DEL_DF_C_SQL, delDefConstraintSqlList);
        return map;
    }

    /**
     * 获取修改表sql
     *
     * @param tableInfo
     * @param tableColumnInfoList
     * @param constraintInfoList
     * @param defaultInfoList
     * @param modelEnums
     * @return
     */
    public static List<String> getUpdateTableSql(TableInfo tableInfo,
                                                 List<TableColumnInfo> tableColumnInfoList,
                                                 List<ConstraintInfo> constraintInfoList,
                                                 List<ConstraintInfo> defaultInfoList,
                                                 ModelEnums modelEnums) {
        List<String> list = new ArrayList<>();
        TableColumnInfo firstTableColumnInfo = tableColumnInfoList.get(0);
        String tableName = firstTableColumnInfo.getTableName();
        List<ConstraintInfo> constraintInfoNewList = new ArrayList<>();
        //处理表备注
        handleTableComment(list, tableInfo.getComment(), firstTableColumnInfo.getTableComment(), tableName);
        /*
         * 执行顺序
         * 1.删除所有约束（唯一键、主键、索引,默认值）
         * 2.修改列信息
         * 3.新增列信息
         * 4.创建约束（唯一键、主键、索引）
         */
        //删除列
        List<String> delColumnSqlList = new ArrayList<>();
        //添加列
        List<String> addColumnSqlList = new ArrayList<>();
        //添加列备注
        List<String> addColumnCommentSqlList = new ArrayList<>();
        //修改列
        List<String> updateColumnSqlList = new ArrayList<>();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();

        /*
         * 标记是否需要删除相关约束
         */
        boolean defFlag = false;
        boolean pkFlag = false;
        boolean idxFlag = false;
        boolean ukFlag = false;

        for (TableColumnInfo tableColumnInfo : tableColumnInfoList) {
            boolean flag = false;
            Iterator<TableInfo.PropertyInfo> it = propertyInfoList.iterator();
            while (it.hasNext()) {
                TableInfo.PropertyInfo propertyInfo = it.next();
                if (Objects.equals(tableColumnInfo.getColumnName(), propertyInfo.getColumnName())) {
                    //默认值
                    boolean defExistUpdate = !(Objects.equals(propertyInfo.getDefaultValue(), tableColumnInfo.getDefaultValue())
                            || Objects.equals(StringPool.LEFT_BRACKET + propertyInfo.getDefaultValue() + StringPool.RIGHT_BRACKET, tableColumnInfo.getDefaultValue())
                            || Objects.equals(StringPool.LEFT_BRACKET + StringPool.LEFT_BRACKET + propertyInfo.getDefaultValue() + StringPool.RIGHT_BRACKET + StringPool.RIGHT_BRACKET, tableColumnInfo.getDefaultValue()));
                    if (defExistUpdate) {
                        excludeDefConstraint(defaultInfoList, propertyInfo.getColumnName());
                        defFlag = true;
                    }
                    //类型、是否为空、是否自增
                    boolean existUpdate = !(Objects.equals(tableColumnInfo.getTypeStr(), propertyInfo.getType().getType()))
                            || defExistUpdate
                            || !(propertyInfo.isKey() == tableColumnInfo.isKey())
                            || !(tableColumnInfo.isNull() == (!propertyInfo.isKey() && !propertyInfo.isAutoIncrement() && propertyInfo.isNull()))
                            || !(tableColumnInfo.isAutoIncrement() == propertyInfo.isAutoIncrement());

                    ColumnTypeEnums type = propertyInfo.getType();
                    int length = propertyInfo.getLength();
                    int decimalLength = propertyInfo.getDecimalLength();
                    //长度、精度
                    switch (type) {
                        case NVARCHAR:
                        case VARCHAR:
                        case NCHAR:
                        case CHAR:
                            existUpdate = existUpdate || !(Objects.equals(tableColumnInfo.getLength(), handleStrLength(length)));
                            break;
                        case DATETIME2:
                            existUpdate = existUpdate || !(Objects.equals(tableColumnInfo.getDecimalLength(), handleDateTime2Length(length)));
                            break;
                        case DECIMAL:
                        case NUMERIC:
                            if (decimalLength > length) {
                                decimalLength = length;
                            }
                            length = length > 38 || length < 0 ? 18 : length;
                            decimalLength = decimalLength > 38 || decimalLength < 0 ? 2 : decimalLength;
                            existUpdate = existUpdate || !(Objects.equals(tableColumnInfo.getLength(), length))
                                    || !(Objects.equals(tableColumnInfo.getDecimalLength(), decimalLength));
                            break;
                    }
                    if (existUpdate) {
                        if (tableColumnInfo.isAutoIncrement() && !propertyInfo.isAutoIncrement()) {
                            throw new RuntimeException(StrUtil.format("修改时，不能将自增字段改成非自增，请手动删除该字段或者调整实体类！！！table={} column={}", tableName, propertyInfo.getColumnName()));
                        }
                        if (propertyInfo.isKey() || tableColumnInfo.isKey()) {
                            pkFlag = true;
                        }
                        StringBuilder propertySb = new StringBuilder();
                        splicingAddOrCreateColumn(type, propertySb, propertyInfo, tableName, true, updateColumnSqlList);
                        updateColumnSqlList.add(StrUtil.format(UPDATE_COLUMN,
                                tableName, propertyInfo.getColumnName(), propertySb));
                    }

                    //判断是否调整了备注
                    if (!Objects.equals(tableColumnInfo.getColumnComment(), propertyInfo.getColumnComment())) {
                        if (Objects.isNull(tableColumnInfo.getColumnComment())) {
                            //数据库为null新增备注
                            list.add(StrUtil.format(ADD_COLUMN_COMMENT, propertyInfo.getColumnComment(), tableName, propertyInfo.getColumnName()));
                        } else {
                            if (Objects.isNull(propertyInfo.getColumnComment())) {
                                //字段null删除备注
                                list.add(StrUtil.format(DROP_COLUMN_COMMENT, tableName, propertyInfo.getColumnName()));
                            } else {
                                //修改备注
                                list.add(StrUtil.format(UPDATE_COLUMN_COMMENT, propertyInfo.getColumnComment(), tableName, propertyInfo.getColumnName()));
                            }
                        }
                    }
                    flag = true;
                    it.remove();
                    break;
                }
            }
            if (Objects.equals(modelEnums, ModelEnums.ADD_OR_UPDATE_DEL)) {
                //如果数据库有但是实体类没有，进行删除
                if (!flag) {
                    if (tableColumnInfo.isKey()) {
                        pkFlag = true;
                    }

                    if (handleUkConstraintDatabase(tableColumnInfo.getColumnName(), constraintInfoList)) {
                        ukFlag = true;
                    }
                    if (handleIdxConstraintDatabase(tableColumnInfo.getColumnName(), constraintInfoList)) {
                        idxFlag = true;
                    }
                    if (StrUtil.isNotBlank(tableColumnInfo.getDefaultValue())) {
                        excludeDefConstraint(defaultInfoList, tableColumnInfo.getColumnName());
                        defFlag = true;
                    }
                    delColumnSqlList.add(StrUtil.format(DROP_COLUMN, tableName, tableColumnInfo.getColumnName()));
                }
            }
        }

        //如果实体类有但是数据库没有，进行新增
        if (CollectionUtil.isNotEmpty(propertyInfoList)) {
            for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
                if (propertyInfo.isKey()) {
                    pkFlag = true;
                }
                if (handleUkConstraint(tableInfo, propertyInfo)) {
                    ukFlag = true;
                }
                if (handleIdxConstraint(tableInfo, propertyInfo)) {
                    idxFlag = true;
                }
                StringBuilder propertySb = new StringBuilder();
                splicingAddOrCreateColumnInfo(propertyInfo.getType(), propertySb, propertyInfo, tableName, addColumnCommentSqlList);
                addColumnSqlList.add(StrUtil.format(ADD_COLUMN, tableName, propertyInfo.getColumnName(), propertySb.deleteCharAt(propertySb.length() - 1)));
            }
        }


        //判断字段数据库和实体类约束是否改变
        /*
         * 1.判断数据库
         * 2.判断实体类
         */
        Set<String> uniqueInfoSet = getPropertyUniqueSet(tableInfo.getUniqueInfoList());
        Set<String> indexInfoSet = getPropertyIndexSet(tableInfo.getIndexInfoList());

        Set<String> uniqueInfoSet1 = getDatabaseUniqueSet(constraintInfoList);
        Set<String> indexInfoSet1 = getDatabaseIndexSet(constraintInfoList);
        int i = 0;
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            i++;
            switch (constraintInfo.getConstraintFlag()) {
                case UK:
                    if (!uniqueInfoSet.contains(constraintInfo.getConstraintColumnName())) {
                        constraintInfoNewList.add(constraintInfo);
                        ukFlag = true;
                    }
                    break;
                case INDEX:
                    if (!indexInfoSet.contains(constraintInfo.getConstraintColumnName())) {
                        constraintInfoNewList.add(constraintInfo);
                        idxFlag = true;
                    }
                    break;
                default:
                    constraintInfoNewList.add(constraintInfo);
            }
        }
        for (TableInfo.UniqueInfo uniqueInfo : tableInfo.getUniqueInfoList()) {
            String[] columns = uniqueInfo.getColumns();
            if (ArrayUtil.isNotEmpty(columns)) {
                Arrays.sort(columns);
                if (!uniqueInfoSet1.contains(StrUtil.join(StringPool.COMMA, columns))) {
                    ukFlag = true;
                    break;
                }
            }
        }
        for (TableInfo.IndexInfo indexInfo : tableInfo.getIndexInfoList()) {
            String[] columns = indexInfo.getColumns();
            if (ArrayUtil.isNotEmpty(columns)) {
                Arrays.sort(columns);
                if (!indexInfoSet1.contains(StrUtil.join(StringPool.COMMA, columns))) {
                    idxFlag = true;
                    break;
                }
            }
        }
        //是否排除主键约束的删除
        excludePkConstraint(constraintInfoNewList, pkFlag);
        List<String> delUkList = new ArrayList<>();
        List<String> delIdxList = new ArrayList<>();
        List<String> zeroUkList = new ArrayList<>();
        List<String> zeroIdxList = new ArrayList<>();
        //获取需要删除的约束（主键 唯一键 索引 默认值）
        Map<String, Collection<String>> delConstraintSqlMap = handleConstraint(tableName, constraintInfoNewList, defaultInfoList, delUkList, delIdxList);
        //删除列
        if (pkFlag) {
            list.addAll(delConstraintSqlMap.get(DEL_PK_C_SQL));
        }
        if (defFlag) {
            list.addAll(delConstraintSqlMap.get(DEL_DF_C_SQL));
        }
        if (ukFlag) {
            list.addAll(delConstraintSqlMap.get(DEL_UK_C_SQL));
        }
        if (idxFlag) {
            list.addAll(delConstraintSqlMap.get(DEL_INDEX_C_SQL));
        }
        list.addAll(delColumnSqlList);
        //添加列
        list.addAll(addColumnSqlList);
        list.addAll(addColumnCommentSqlList);
        //修改列
        list.addAll(updateColumnSqlList);

        for (ConstraintInfo constraintInfo : constraintInfoList) {
            switch (constraintInfo.getConstraintFlag()) {
                case UK:
                    if (!delUkList.contains(constraintInfo.getConstraintName())) {
                        zeroUkList.add(constraintInfo.getConstraintName());
                    }
                    break;
                case INDEX:
                    if (!delIdxList.contains(constraintInfo.getConstraintName())) {
                        zeroIdxList.add(constraintInfo.getConstraintName());
                    }
                    break;
                default:
            }
        }
        createConstraint(tableInfo, list, pkFlag, ukFlag, idxFlag, zeroUkList, zeroIdxList);
        return list;
    }

    /**
     * 排除主键约束删除
     *
     * @param defaultInfoList
     * @param pkFlag
     */
    public static void excludePkConstraint(List<ConstraintInfo> defaultInfoList, boolean pkFlag) {
        if (!pkFlag) {
            defaultInfoList.removeIf(constraintInfo -> Objects.equals(PK, constraintInfo.getConstraintFlag()));
        }
    }

    /**
     * 排除默认值约束删除
     *
     * @param defaultInfoList
     * @param columnName
     */
    public static void excludeDefConstraint(List<ConstraintInfo> defaultInfoList, String columnName) {
        defaultInfoList.removeIf(constraintInfo -> Objects.equals(constraintInfo.getConstraintColumnName(), columnName));
    }

    /**
     * 获取字段唯一键集合
     *
     * @param uniqueInfoList
     * @return
     */
    public static Set<String> getPropertyUniqueSet(List<TableInfo.UniqueInfo> uniqueInfoList) {
        Set<String> set = new HashSet<>();
        for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
            String[] columns = uniqueInfo.getColumns();
            if (ArrayUtil.isNotEmpty(columns)) {
                Arrays.sort(columns);
                set.add(StrUtil.join(StringPool.COMMA, columns));
            }
        }
        return set;
    }

    /**
     * 获取数据库唯一键集合
     *
     * @param constraintInfoList
     * @return
     */
    public static Set<String> getDatabaseUniqueSet(List<ConstraintInfo> constraintInfoList) {
        Set<String> set = new HashSet<>();
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            if (Objects.equals(constraintInfo.getConstraintFlag(), UK)) {
                set.add(constraintInfo.getConstraintColumnName());
            }
        }
        return set;
    }

    /**
     * 获取字段索引集合
     *
     * @param indexInfoList
     * @return
     */
    public static Set<String> getPropertyIndexSet(List<TableInfo.IndexInfo> indexInfoList) {
        Set<String> set = new HashSet<>();
        for (TableInfo.IndexInfo indexInfo : indexInfoList) {
            String[] columns = indexInfo.getColumns();
            if (ArrayUtil.isNotEmpty(columns)) {
                Arrays.sort(columns);
                set.add(StrUtil.join(StringPool.COMMA, columns));
            }
        }
        return set;
    }

    /**
     * 获取数据库索引集合
     *
     * @param constraintInfoList
     * @return
     */
    public static Set<String> getDatabaseIndexSet(List<ConstraintInfo> constraintInfoList) {
        Set<String> set = new HashSet<>();
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            if (Objects.equals(constraintInfo.getConstraintFlag(), INDEX)) {
                set.add(constraintInfo.getConstraintColumnName());
            }
        }
        return set;
    }

    private static boolean handleUkConstraint(TableInfo tableInfo, TableInfo.PropertyInfo propertyInfo) {
        List<TableInfo.UniqueInfo> uniqueInfoList = tableInfo.getUniqueInfoList();
        for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
            String[] columns = uniqueInfo.getColumns();
            for (String column : columns) {
                if (Objects.equals(column, propertyInfo.getColumnName())) {
                    return true;
                }
            }
        }
        return false;
    }


    private static boolean handleIdxConstraint(TableInfo tableInfo, TableInfo.PropertyInfo propertyInfo) {
        List<TableInfo.IndexInfo> indexInfoList = tableInfo.getIndexInfoList();
        for (TableInfo.IndexInfo indexInfo : indexInfoList) {
            String[] columns = indexInfo.getColumns();
            for (String column : columns) {
                if (Objects.equals(column, propertyInfo.getColumnName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean handleUkConstraintDatabase(String columnName, List<ConstraintInfo> constraintInfoList) {
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            if (Objects.equals(constraintInfo.getConstraintColumnName(), columnName)
                    && Objects.equals(constraintInfo.getConstraintFlag(), 2)) {
                return true;
            }
        }
        return false;
    }

    private static boolean handleIdxConstraintDatabase(String columnName, List<ConstraintInfo> constraintInfoList) {
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            if (Objects.equals(constraintInfo.getConstraintColumnName(), columnName)
                    && Objects.equals(constraintInfo.getConstraintFlag(), 3)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理字符串长度
     *
     * @param length
     * @return
     */
    public static int handleStrLength(int length) {
        return length < 0 ? 255 : length;
    }

    /**
     * 处理时间长度
     *
     * @param length
     * @return
     */
    public static int handleDateTime2Length(int length) {
        return length > 7 || length < 0 ? 7 : length;
    }

    /**
     * 拼接添加或创建（表、字段）信息
     *
     * @param type
     * @param propertySb
     * @param propertyInfo
     * @param tableName
     * @param isUpdate
     * @param updateColumnSqlList
     */
    private static void splicingAddOrCreateColumn(ColumnTypeEnums type,
                                                  StringBuilder propertySb,
                                                  TableInfo.PropertyInfo propertyInfo,
                                                  String tableName,
                                                  boolean isUpdate,
                                                  List<String> updateColumnSqlList) {
        switch (type) {
            case VARCHAR:
            case NVARCHAR:
            case DATETIME2:
            case NCHAR:
            case CHAR:
                propertySb.append(StringPool.SPACE)
                        .append(type.getType())
                        .append(StringPool.LEFT_BRACKET);
                if (Objects.equals(type, ColumnTypeEnums.DATETIME2)) {
                    //对类型特殊处理
                    if (propertyInfo.getLength() > 7 || propertyInfo.getLength() < 0) {
                        log.warn(COLUMN_LENGTH_VALID_STR, tableName, propertyInfo.getColumnName(), type, propertyInfo.getLength(), 7);
                        propertySb.append(7);
                    } else {
                        propertySb.append(propertyInfo.getLength());
                    }
                } else {
                    if (propertyInfo.getLength() < 0) {
                        log.warn(COLUMN_LENGTH_VALID_STR, tableName, propertyInfo.getColumnName(), type, propertyInfo.getLength(), 255);
                        propertySb.append(255);
                    } else {
                        propertySb.append(propertyInfo.getLength());
                    }
                }
                propertySb.append(StringPool.RIGHT_BRACKET);
                break;

            case DECIMAL:
            case NUMERIC:
                propertySb.append(StringPool.SPACE)
                        .append(type.getType())
                        .append(StringPool.LEFT_BRACKET);

                int decimalLength = propertyInfo.getDecimalLength();
                int length = propertyInfo.getLength();
                if (decimalLength > length) {
                    log.warn("表 [{}] 字段 [{}] {}精度长度 [{}] 大于类型长度 [{}] 存在问题，使用类型长度 [{}]", tableName, propertyInfo.getColumnName(), type, decimalLength, length, length);
                    decimalLength = length;
                }
                if (length > 38 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, propertyInfo.getColumnName(), type, length, 38);
                    propertySb.append(18);
                } else {
                    propertySb.append(length);
                }
                if (decimalLength > 38 || decimalLength < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, propertyInfo.getColumnName(), type, decimalLength, 2);
                    propertySb.append(StringPool.COMMA).append(2);
                } else {
                    propertySb.append(StringPool.COMMA).append(decimalLength);
                }
                propertySb.append(StringPool.RIGHT_BRACKET);
                break;
            default:
                propertySb.append(StringPool.SPACE)
                        .append(type.getType());
        }
        //是否自增
        if (propertyInfo.isAutoIncrement()) {
            if (isUpdate) {
                throw new RuntimeException(StrUtil.format("修改时，不能将字段改成自增，请手动删除该字段或者调整实体类！！！table={} column={}", tableName, propertyInfo.getColumnName()));
            } else {
                propertySb.append(StringPool.SPACE)
                        .append(IDENTITY);
            }
        }
        //默认值
        if (StrUtil.isNotBlank(propertyInfo.getDefaultValue())) {
            if (isUpdate) {
                updateColumnSqlList.add(StrUtil.format(ADD_DEFAULT, tableName, propertyInfo.getDefaultValue(), propertyInfo.getColumnName()));
            } else {
                propertySb.append(StringPool.SPACE)
                        .append(DEFAULT)
                        .append(StringPool.SPACE)
                        .append(propertyInfo.getDefaultValue());
            }
        }

        //是否为空
        if (propertyInfo.isNull() && !propertyInfo.isKey() && !propertyInfo.isAutoIncrement()) {
            propertySb.append(StringPool.SPACE)
                    .append(NULL);
        } else {
            propertySb.append(StringPool.SPACE)
                    .append(NOT_NULL);
        }
    }

    /**
     * 拼接添加或创建（表、字段）信息，拼接列备注sql
     *
     * @param type
     * @param propertySb
     * @param propertyInfo
     * @param tableName
     * @param addColumnCommentSqlList
     */
    private static void splicingAddOrCreateColumnInfo(ColumnTypeEnums type,
                                                      StringBuilder propertySb,
                                                      TableInfo.PropertyInfo propertyInfo,
                                                      String tableName,
                                                      List<String> addColumnCommentSqlList) {
        splicingAddOrCreateColumn(type, propertySb, propertyInfo, tableName, false, null);
        propertySb.append(StringPool.COMMA);
        if (StrUtil.isNotBlank(propertyInfo.getColumnComment())) {
            //字段备注
            addColumnCommentSqlList.add(StrUtil.format(ADD_COLUMN_COMMENT, propertyInfo.getColumnComment(), tableName, propertyInfo.getColumnName()));
        }
    }
}
