package io.gitee.zerowsh.actable.util.sql;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.util.AcTableUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.*;

/**
 * sql_server自动建表工具类
 *
 * @author zero
 */
@Slf4j
@SuppressWarnings("all")
public class SqlServerAcTableUtils {
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
        List<String> resultList = new ArrayList<>();
        List<String> addColumnCommentSqlList = new ArrayList<>();
        String tableName = tableInfo.getName();
        String comment = tableInfo.getComment();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        StringBuilder propertySb = new StringBuilder();
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            String columnName = propertyInfo.getColumnName();
            String columnComment = propertyInfo.getColumnComment();
            propertySb.append(StrUtil.format(SQL_SERVER_KEYWORD_HANDLE, columnName));
            splicingColumnInfo(propertySb, propertyInfo, tableName);
            if (StrUtil.isNotBlank(columnComment)) {
                addColumnCommentSqlList.add(StrUtil.format(ADD_COLUMN_COMMENT, columnComment, tableName, columnName));
            }
        }

        //建表
        resultList.add(StrUtil.format(CREATE_TABLE, StrUtil.format(SQL_SERVER_KEYWORD_HANDLE, tableName), propertySb.deleteCharAt(propertySb.length() - 1)));
        if (StrUtil.isNotBlank(comment)) {
            //添加表备注
            resultList.add(StrUtil.format(ADD_TABLE_COMMENT, comment, tableName));
        }
        //添加字段备注
        resultList.addAll(addColumnCommentSqlList);
        //创建主键
        createPk(true, tableInfo.getKeyList(), tableName, resultList);

        //创建唯一键
        createUk(true, tableInfo.getUniqueInfoList(), tableName, resultList, null);

        //创建索引
        createIdx(true, tableInfo.getIndexInfoList(), tableName, resultList, null);
        return resultList;
    }

    /**
     * 创建主键
     *
     * @param flag
     * @param keyList
     * @param tableName
     * @param resultList
     */
    private static void createPk(boolean flag, List<String> keyList, String tableName, List<String> resultList) {
        if (flag && CollectionUtil.isNotEmpty(keyList)) {
            StringBuilder keySb = new StringBuilder();
            for (String key : keyList) {
                keySb.append(StrUtil.format(SQL_SERVER_KEYWORD_HANDLE, key)).append(StringPool.COMMA);
            }
            resultList.add(StrUtil.format(CREATE_PRIMARY_KEY, tableName, PK_ + tableName, keySb.deleteCharAt(keySb.length() - 1)));
        }
    }

    /**
     * 创建唯一键
     *
     * @param flag
     * @param uniqueInfoList
     * @param tableName
     * @param resultList
     */
    private static void createUk(boolean flag, List<TableInfo.UniqueInfo> uniqueInfoList, String tableName, List<String> resultList, List<String> existUkNameList) {
        if (flag && CollectionUtil.isNotEmpty(uniqueInfoList)) {
            for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
                String[] columns = uniqueInfo.getColumns();
                StringBuilder uniqueSb = new StringBuilder();
                for (String column : columns) {
                    uniqueSb.append(StrUtil.format(SQL_SERVER_KEYWORD_HANDLE, column)).append(StringPool.COMMA);
                }

                if (CollectionUtil.isEmpty(existUkNameList) || !existUkNameList.contains(uniqueInfo.getValue())) {
                    resultList.add(StrUtil.format(CREATE_UNIQUE, tableName, uniqueInfo.getValue(), uniqueSb.deleteCharAt(uniqueSb.length() - 1)));
                }
            }
        }
    }

    /**
     * 创建索引
     *
     * @param indexInfoList
     * @param tableName
     * @param resultList
     */
    private static void createIdx(boolean flag, List<TableInfo.IndexInfo> indexInfoList, String tableName, List<String> resultList, List<String> existIdxNameList) {
        if (flag && CollectionUtil.isNotEmpty(indexInfoList)) {
            for (TableInfo.IndexInfo indexInfo : indexInfoList) {
                String[] columns = indexInfo.getColumns();
                StringBuilder indexSb = new StringBuilder();
                for (String column : columns) {
                    indexSb.append(StrUtil.format(SQL_SERVER_KEYWORD_HANDLE, column)).append(StringPool.COMMA);
                }
                if (CollectionUtil.isEmpty(existIdxNameList) || !existIdxNameList.contains(indexInfo.getValue())) {
                    resultList.add(StrUtil.format(CREATE_INDEX, indexInfo.getValue(), tableName, indexSb.deleteCharAt(indexSb.length() - 1)));
                }
            }
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
     * 处理列备注
     *
     * @param tableColumnInfo
     * @param propertyInfo
     * @param resultList
     * @param tableName
     */
    private static void handleColumnComment(TableColumnInfo tableColumnInfo, TableInfo.PropertyInfo propertyInfo, List<String> resultList, String tableName) {
        //判断是否调整了备注
        if (!Objects.equals(tableColumnInfo.getColumnComment(), propertyInfo.getColumnComment())) {
            if (Objects.isNull(tableColumnInfo.getColumnComment())) {
                //数据库为null新增备注
                resultList.add(StrUtil.format(ADD_COLUMN_COMMENT, propertyInfo.getColumnComment(), tableName, propertyInfo.getColumnName()));
            } else {
                if (Objects.isNull(propertyInfo.getColumnComment())) {
                    //字段null删除备注
                    resultList.add(StrUtil.format(DROP_COLUMN_COMMENT, tableName, propertyInfo.getColumnName()));
                } else {
                    //修改备注
                    resultList.add(StrUtil.format(UPDATE_COLUMN_COMMENT, propertyInfo.getColumnComment(), tableName, propertyInfo.getColumnName()));
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
        Map<String, Collection<String>> map = new HashMap<>(4);
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
                default:
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
        List<String> resultList = new ArrayList<>();
        TableColumnInfo firstTableColumnInfo = tableColumnInfoList.get(0);
        String tableName = firstTableColumnInfo.getTableName();
        List<ConstraintInfo> constraintInfoNewList = new ArrayList<>();
        List<ConstraintInfo> defaultInfoNewList = new ArrayList<>();
        //处理表备注
        handleTableComment(resultList, tableInfo.getComment(), firstTableColumnInfo.getTableComment(), tableName);
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
        //添加列默认值
        List<String> addColumnDefSqlList = new ArrayList<>();
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
                String type = propertyInfo.getType();
                if (Objects.equals(tableColumnInfo.getColumnName(), propertyInfo.getColumnName())) {
                    //默认值
                    boolean defExistUpdate = !(Objects.equals(propertyInfo.getDefaultValue(), tableColumnInfo.getDefaultValue())
                            || Objects.equals(StringPool.LEFT_BRACKET + propertyInfo.getDefaultValue() + StringPool.RIGHT_BRACKET, tableColumnInfo.getDefaultValue())
                            || Objects.equals(StringPool.LEFT_BRACKET + StringPool.LEFT_BRACKET + propertyInfo.getDefaultValue() + StringPool.RIGHT_BRACKET + StringPool.RIGHT_BRACKET, tableColumnInfo.getDefaultValue()));
                    boolean judgeDef = defExistUpdate || (!(Objects.equals(tableColumnInfo.getTypeStr(), type)) && Objects.nonNull(tableColumnInfo.getDefaultValue()));
                    if (judgeDef) {
                        addDelDefConstraintInfo(defaultInfoList, defaultInfoNewList, propertyInfo.getColumnName());
                        defFlag = true;
                    }
                    //类型、是否为空、是否自增
                    boolean existUpdate = !(Objects.equals(tableColumnInfo.getTypeStr(), type))
                            || defExistUpdate
                            || propertyInfo.isKey() != tableColumnInfo.isKey()
                            || !(tableColumnInfo.isNull() == (!propertyInfo.isKey() && !propertyInfo.isAutoIncrement() && propertyInfo.isNull()))
                            || tableColumnInfo.isAutoIncrement() != propertyInfo.isAutoIncrement();
                    int length = propertyInfo.getLength();
                    int decimalLength = propertyInfo.getDecimalLength();
                    ColumnTypeEnums typeEnum = getSqlServerByValue(type);
                    //长度、精度
                    switch (typeEnum) {
                        case NVARCHAR:
                        case VARCHAR:
                        case NCHAR:
                        case CHAR:
                            existUpdate = existUpdate || !(Objects.equals(tableColumnInfo.getLength(), AcTableUtils.handleStrLength(length)));
                            break;
                        case DATETIME2:
                            existUpdate = existUpdate || !(Objects.equals(tableColumnInfo.getDecimalLength(), AcTableUtils.handleDateLength(length)));
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
                        default:
                    }
                    if (existUpdate) {
                        if (tableColumnInfo.isAutoIncrement() && !propertyInfo.isAutoIncrement()) {
                            throw new RuntimeException(StrUtil.format("修改时，不能将自增字段改成非自增，请手动删除该字段或者调整实体类！！！table={} column={}", tableName, propertyInfo.getColumnName()));
                        }
                        if (propertyInfo.isKey() || tableColumnInfo.isKey()) {
                            pkFlag = true;
                        }
                        StringBuilder propertySb = new StringBuilder();
                        splicingColumnInfo(propertySb, propertyInfo, tableName, true, addColumnDefSqlList);
                        updateColumnSqlList.add(StrUtil.format(UPDATE_COLUMN,
                                tableName, propertyInfo.getColumnName(), propertySb.deleteCharAt(propertySb.length() - 1)));
                    }
                    //处理列备注
                    handleColumnComment(tableColumnInfo, propertyInfo, resultList, tableName);
                    flag = true;
                    it.remove();
                    break;
                }
            }
            if (Objects.equals(modelEnums, ModelEnums.ADD_OR_UPDATE_OR_DEL)) {
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
                        addDelDefConstraintInfo(defaultInfoList, defaultInfoNewList, tableColumnInfo.getColumnName());
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
                splicingColumnInfo(propertySb, propertyInfo, tableName);
                if (StrUtil.isNotBlank(propertyInfo.getColumnComment())) {
                    //字段备注
                    addColumnCommentSqlList.add(StrUtil.format(ADD_COLUMN_COMMENT, propertyInfo.getColumnComment(), tableName, propertyInfo.getColumnName()));
                }
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
        for (ConstraintInfo constraintInfo : constraintInfoList) {
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
        List<String> exitUkList = new ArrayList<>();
        List<String> exitIdxList = new ArrayList<>();
        //获取需要删除的约束（主键 唯一键 索引 默认值）
        Map<String, Collection<String>> delConstraintSqlMap = handleConstraint(tableName, constraintInfoNewList, defaultInfoNewList, delUkList, delIdxList);

        for (ConstraintInfo constraintInfo : constraintInfoList) {
            switch (constraintInfo.getConstraintFlag()) {
                case UK:
                    if (!delUkList.contains(constraintInfo.getConstraintName())) {
                        exitUkList.add(constraintInfo.getConstraintName());
                    }
                    break;
                case INDEX:
                    if (!delIdxList.contains(constraintInfo.getConstraintName())) {
                        exitIdxList.add(constraintInfo.getConstraintName());
                    }
                    break;
                default:
            }
        }
        //删除列
        if (pkFlag) {
            resultList.addAll(delConstraintSqlMap.get(DEL_PK_C_SQL));
        }
        if (ukFlag) {
            resultList.addAll(delConstraintSqlMap.get(DEL_UK_C_SQL));
        }
        if (idxFlag) {
            resultList.addAll(delConstraintSqlMap.get(DEL_INDEX_C_SQL));
        }
        if (defFlag) {
            resultList.addAll(delConstraintSqlMap.get(DEL_DF_C_SQL));
        }
        resultList.addAll(delColumnSqlList);
        //添加列
        resultList.addAll(addColumnSqlList);
        //添加列备注
        resultList.addAll(addColumnCommentSqlList);
        //修改列
        resultList.addAll(updateColumnSqlList);
        //创建主键
        createPk(pkFlag, tableInfo.getKeyList(), tableName, resultList);
        //创建唯一键
        createUk(ukFlag, tableInfo.getUniqueInfoList(), tableName, resultList, exitUkList);
        //创建索引
        createIdx(idxFlag, tableInfo.getIndexInfoList(), tableName, resultList, exitIdxList);
        if (defFlag) {
            //添加列默认值
            resultList.addAll(addColumnDefSqlList);
        }
        return resultList;
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
     * 添加要删除的默认值约束
     *
     * @param defaultInfoList
     * @param columnName
     */
    public static void addDelDefConstraintInfo(List<ConstraintInfo> defaultInfoList, List<ConstraintInfo> defaultInfoNewList, String columnName) {
        for (ConstraintInfo constraintInfo : defaultInfoList) {
            if (Objects.equals(constraintInfo.getConstraintColumnName(), columnName)) {
                defaultInfoNewList.add(constraintInfo);
            }
        }
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
                    && Objects.equals(constraintInfo.getConstraintFlag(), UK)) {
                return true;
            }
        }
        return false;
    }

    private static boolean handleIdxConstraintDatabase(String columnName, List<ConstraintInfo> constraintInfoList) {
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            if (Objects.equals(constraintInfo.getConstraintColumnName(), columnName)
                    && Objects.equals(constraintInfo.getConstraintFlag(), INDEX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 拼接列信息
     *
     * @param propertySb
     * @param propertyInfo
     * @param tableName
     */
    private static void splicingColumnInfo(StringBuilder propertySb, TableInfo.PropertyInfo propertyInfo, String tableName) {
        splicingColumnInfo(propertySb, propertyInfo, tableName, false, null);
    }

    /**
     * 拼接列信息
     *
     * @param propertySb
     * @param propertyInfo
     * @param tableName
     * @param isUpdate
     * @param addColumnDefSqlList
     */
    private static void splicingColumnInfo(StringBuilder propertySb, TableInfo.PropertyInfo propertyInfo,
                                           String tableName, boolean isUpdate, List<String> addColumnDefSqlList) {
        splicingColumnType(propertySb, propertyInfo, tableName);
        //是否自增
        if (propertyInfo.isAutoIncrement()) {
            if (!isUpdate) {
                propertySb.append(IDENTITY);
            }
        }
        //默认值
        if (StrUtil.isNotBlank(propertyInfo.getDefaultValue())) {
            if (isUpdate) {
                addColumnDefSqlList.add(StrUtil.format(ADD_DEFAULT, tableName, propertyInfo.getDefaultValue(), propertyInfo.getColumnName()));
            } else {
                propertySb.append(DEFAULT).append(propertyInfo.getDefaultValue());
            }
        }

        //是否为空
        if (propertyInfo.isNull() && !propertyInfo.isKey() && !propertyInfo.isAutoIncrement()) {
            propertySb.append(NULL);
        } else {
            propertySb.append(NOT_NULL);
        }
        propertySb.append(StringPool.COMMA);
    }

    /**
     * 拼接列类型
     *
     * @param propertySb
     * @param propertyInfo
     * @param tableName
     */
    private static void splicingColumnType(StringBuilder propertySb, TableInfo.PropertyInfo propertyInfo, String tableName) {
        String type = propertyInfo.getType();
        int length = propertyInfo.getLength();
        int decimalLength = propertyInfo.getDecimalLength();
        String columnName = propertyInfo.getColumnName();
        ColumnTypeEnums typeEnum = getSqlServerByValue(type);
        switch (typeEnum) {
            case VARCHAR:
            case NVARCHAR:
            case DATETIME2:
            case NCHAR:
            case CHAR:
                propertySb.append(StringPool.SPACE).append(type).append(StringPool.LEFT_BRACKET);
                if (Objects.equals(type, ColumnTypeEnums.DATETIME2.getType())) {
                    //对类型特殊处理
                    if (length > 7 || length < 0) {
                        log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 0);
                        propertySb.append(0);
                    } else {
                        propertySb.append(length);
                    }
                } else {
                    if (length < 0) {
                        log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 255);
                        propertySb.append(255);
                    } else {
                        propertySb.append(length);
                    }
                }
                propertySb.append(StringPool.RIGHT_BRACKET);
                break;

            case DECIMAL:
            case NUMERIC:
                propertySb.append(StringPool.SPACE).append(type).append(StringPool.LEFT_BRACKET);

                if (decimalLength > length) {
                    log.warn("表 [{}] 字段 [{}] {}精度长度 [{}] 大于类型长度 [{}] 存在问题，使用类型长度 [{}]", tableName, columnName, type, decimalLength, length, length);
                    decimalLength = length;
                }
                if (length > 38 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 18);
                    propertySb.append(18);
                } else {
                    propertySb.append(length);
                }
                if (decimalLength > 38 || decimalLength < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, decimalLength, 2);
                    propertySb.append(StringPool.COMMA).append(2);
                } else {
                    propertySb.append(StringPool.COMMA).append(decimalLength);
                }
                propertySb.append(StringPool.RIGHT_BRACKET);
                break;
            default:
                propertySb.append(StringPool.SPACE).append(type);
        }
    }


    /**
     * 数据库类型转java类型
     */
    private static final Map<String, ColumnTypeEnums> JAVA_TURN_SQL_SERVER_MAP = new HashMap<String, ColumnTypeEnums>() {{
        put("java.lang.String", ColumnTypeEnums.NVARCHAR);
        put("java.lang.Long", ColumnTypeEnums.BIGINT);
        put("long", ColumnTypeEnums.BIGINT);
        put("java.lang.Integer", ColumnTypeEnums.INT);
        put("int", ColumnTypeEnums.INT);
        put("java.lang.Boolean", ColumnTypeEnums.BIT);
        put("java.lang.boolean", ColumnTypeEnums.BIT);
        put("java.util.Date", ColumnTypeEnums.DATETIME2);
        put("java.sql.Timestamp", ColumnTypeEnums.DATETIME2);
        put("java.time.LocalDate", ColumnTypeEnums.DATETIME2);
        put("java.time.LocalDateTime", ColumnTypeEnums.DATETIME2);
        put("java.math.BigDecimal", ColumnTypeEnums.NUMERIC);
        put("java.lang.Double", ColumnTypeEnums.NUMERIC);
        put("double", ColumnTypeEnums.NUMERIC);
        put("java.lang.Float", ColumnTypeEnums.FLOAT);
        put("float", ColumnTypeEnums.FLOAT);
        put("char", ColumnTypeEnums.NCHAR);
    }};

    /**
     * java类型转数据库类型
     *
     * @param key
     * @return
     */
    public static String getJavaTurnSqlServerValue(String key) {
        ColumnTypeEnums columnTypeEnums = JAVA_TURN_SQL_SERVER_MAP.get(key);
        return Objects.isNull(columnTypeEnums) ? ColumnTypeEnums.NVARCHAR.getType() : columnTypeEnums.getType();
    }

    public static ColumnTypeEnums getSqlServerByValue(String type) {
        for (ColumnTypeEnums types : ColumnTypeEnums.values()) {
            if (Objects.equals(types.getType(), type)) {
                return types;
            }
        }
        return null;
    }
}
