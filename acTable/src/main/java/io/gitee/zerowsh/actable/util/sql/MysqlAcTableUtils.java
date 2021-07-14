package io.gitee.zerowsh.actable.util.sql;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.emnus.MysqlColumnTypeEnums;
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
public class MysqlAcTableUtils {
    /**
     * 获取创建表sql
     * CREATE TABLE `t_zero` (
     * `id` varchar(36) NOT NULL DEFAULT '1',
     * `name` int(255) NOT NULL,
     * `date` datetime DEFAULT NULL,
     * `dd` decimal(10,2) NOT NULL,
     * `zero` int(11) NOT NULL AUTO_INCREMENT,
     * PRIMARY KEY (`zero`),
     * UNIQUE KEY `3` (`id`,`name`),
     * KEY `1` (`id`,`name`),
     * KEY `2` (`id`)
     * ) COMMENT='wewrwe'
     *
     * @param tableInfo
     * @return
     */
    public static List<String> getCreateTableSql(TableInfo tableInfo) {
        List<String> resultList = new ArrayList<>();
        String tableName = tableInfo.getName();
        String comment = tableInfo.getComment();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        StringBuilder propertySb = new StringBuilder();
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            String columnName = propertyInfo.getColumnName();
            String columnComment = propertyInfo.getColumnComment();
            propertySb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, columnName));
            splicingColumnInfo(propertySb, propertyInfo, tableName);
        }

        for (String key : tableInfo.getKeyList()) {
            propertySb.append(StrUtil.format(PRIMARY_KEY, key)).append(StringPool.COMMA);
        }
        for (TableInfo.UniqueInfo uniqueInfo : tableInfo.getUniqueInfoList()) {
            String[] columns = uniqueInfo.getColumns();
            for (String column : columns) {
                propertySb.append(StrUtil.format(UNIQUE_KEY, column, UK_ + uniqueInfo.getValue())).append(StringPool.COMMA);
            }
        }
        for (TableInfo.IndexInfo indexInfo : tableInfo.getIndexInfoList()) {
            String[] columns = indexInfo.getColumns();
            for (String column : columns) {
                propertySb.append(StrUtil.format(INDEX_KEY, column, IDX_ + indexInfo.getValue())).append(StringPool.COMMA);
            }
        }
        String createTable = CREATE_TABLE;
        if (StrUtil.isNotBlank(comment)) {
            //添加表备注
            createTable = createTable + StrUtil.format(MYSQL_COMMENT, comment);
        }
        //建表
        resultList.add(StrUtil.format(createTable, StrUtil.format(MYSQL_KEYWORD_HANDLE, tableName), propertySb.deleteCharAt(propertySb.length() - 1)));
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
    private static void createPk(boolean flag, String tableName, List<String> keyList, List<String> resultList) {
        if (flag && CollectionUtil.isNotEmpty(keyList)) {
            StringBuilder keySb = new StringBuilder();
            for (String key : keyList) {
                keySb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, key)).append(StringPool.COMMA);
            }
            resultList.add(StrUtil.format(MYSQL_ADD_PK, tableName, keySb.deleteCharAt(keySb.length() - 1)));
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
                    uniqueSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, column)).append(StringPool.COMMA);
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
                    indexSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, column)).append(StringPool.COMMA);
                }
                if (CollectionUtil.isEmpty(existIdxNameList) || !existIdxNameList.contains(indexInfo.getValue())) {
                    resultList.add(StrUtil.format(CREATE_INDEX, indexInfo.getValue(), tableName, indexSb.deleteCharAt(indexSb.length() - 1)));
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
    private static Map<String, Collection<String>> handleConstraint(String tableName, List<ConstraintInfo> constraintInfoList) {
        Map<String, Collection<String>> map = new HashMap<>(3);
        //删除所有约束（唯一键、主键、索引）
        List<String> delPkConstraintSqlList = new ArrayList<>();
        Set<String> delUkConstraintSqlSet = new HashSet<>();
        Set<String> delIdxConstraintSqlSet = new HashSet<>();
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            Integer constraintFlag = constraintInfo.getConstraintFlag();
            String constraintName = constraintInfo.getConstraintName();
            switch (constraintFlag) {
                case PK:
                    //主键
                    delPkConstraintSqlList.add(StrUtil.format(MYSQL_DEL_PK, tableName, constraintName));
                    break;
                case UK:
                    //唯一键
                    delUkConstraintSqlSet.add(StrUtil.format(DROP_CONSTRAINT, tableName, constraintName));
                    break;
                case INDEX:
                    //索引
                    delIdxConstraintSqlSet.add(StrUtil.format(DROP_INDEX, constraintName, tableName));
                    break;
                default:
            }
        }
        map.put(DEL_PK_C_SQL, delPkConstraintSqlList);
        map.put(DEL_UK_C_SQL, delUkConstraintSqlSet);
        map.put(DEL_INDEX_C_SQL, delIdxConstraintSqlSet);
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
                                                 ModelEnums modelEnums) {
        List<String> resultList = new ArrayList<>();
        TableColumnInfo firstTableColumnInfo = tableColumnInfoList.get(0);
        String tableName = firstTableColumnInfo.getTableName();
        List<ConstraintInfo> constraintInfoNewList = new ArrayList<>();
        List<ConstraintInfo> defaultInfoNewList = new ArrayList<>();
        //处理表备注
        if (!Objects.equals(tableInfo.getComment(), firstTableColumnInfo.getTableComment())) {
            resultList.add(StrUtil.format(MYSQL_UPDATE_TABLE_COMMENT, tableName, tableInfo.getComment()));
        }

        //调整列信息（删除、修改、新增）
        List<String> adjustClumnSqlList = new ArrayList<>();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();

        boolean pkFlag = false;
        for (TableColumnInfo tableColumnInfo : tableColumnInfoList) {
            boolean flag = false;
            Iterator<TableInfo.PropertyInfo> it = propertyInfoList.iterator();
            StringBuilder updateColumnSql = new StringBuilder();
            while (it.hasNext()) {
                TableInfo.PropertyInfo propertyInfo = it.next();
                if (!Objects.equals(tableColumnInfo.getColumnName(), propertyInfo.getColumnName())) {
                    //todo 这里可以写修改列名的逻辑
                    continue;
                }
                String type = propertyInfo.getType();
                //判断类型、是否为空、是否自增、默认值，这些是否存在修改
                boolean existUpdate = !(Objects.equals(tableColumnInfo.getTypeStr(), type))
                        || !(Objects.equals(propertyInfo.getDefaultValue(), tableColumnInfo.getDefaultValue()))
                        || !(tableColumnInfo.isNull() == (!propertyInfo.isKey() && !propertyInfo.isAutoIncrement() && propertyInfo.isNull()))
                        || tableColumnInfo.isAutoIncrement() != propertyInfo.isAutoIncrement();

                int length = propertyInfo.getLength();
                int decimalLength = propertyInfo.getDecimalLength();
                //判断长度、精度，是否修改
                MysqlColumnTypeEnums typeEnum = MysqlColumnTypeEnums.getByValue(type);
                switch (typeEnum) {
                    case VARCHAR:
                    case CHAR:
                        existUpdate = existUpdate || !(Objects.equals(tableColumnInfo.getLength(), handleStrLength(length)));
                        break;
                    case DATETIME:
                        existUpdate = existUpdate || !(Objects.equals(tableColumnInfo.getDecimalLength(), handleDateTimeLength(length)));
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
                if (propertyInfo.isKey() != tableColumnInfo.isKey()) {
                    if (propertyInfo.isKey() || tableColumnInfo.isKey()) {
                        pkFlag = true;
                    }
                }
                if (existUpdate) {
                    StringBuilder propertySb = new StringBuilder();
                    splicingColumnInfo(propertySb, propertyInfo, tableName);
                    adjustClumnSqlList.add(StrUtil.format(MYSQL_UPDATE_COLUMN,
                            tableName, propertyInfo.getColumnName(), propertySb.deleteCharAt(propertySb.length() - 1)));
                }
                flag = true;
                it.remove();
                break;
            }
            if (Objects.equals(modelEnums, ModelEnums.ADD_OR_UPDATE_OR_DEL)) {
                //如果数据库有但是实体类没有，进行删除
                if (!flag) {
                    if (tableColumnInfo.isKey()) {
                        pkFlag = true;
                    }
                    adjustClumnSqlList.add(StrUtil.format(MYSQL_DEL_COLUMN, tableName, tableColumnInfo.getColumnName()));
                }
            }
        }

        //如果实体类有但是数据库没有，进行新增
        if (CollectionUtil.isNotEmpty(propertyInfoList)) {
            for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
                if (propertyInfo.isKey()) {
                    pkFlag = true;
                }
                StringBuilder propertySb = new StringBuilder();
                splicingColumnInfo(propertySb, propertyInfo, tableName);
                adjustClumnSqlList.add(StrUtil.format(MYSQL_ADD_COLUMN, tableName, propertyInfo.getColumnName(), propertySb.deleteCharAt(propertySb.length() - 1)));
            }
        }
        //调整列信息（删除、修改、新增）
        resultList.addAll(adjustClumnSqlList);
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
    public static int handleDateTimeLength(int length) {
        return length > 7 || length < 0 ? 0 : length;
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
    private static void splicingColumnInfo(StringBuilder propertySb, TableInfo.PropertyInfo propertyInfo, String tableName) {
        splicingColumnType(propertySb, propertyInfo, tableName);
        //是否为空
        if (propertyInfo.isNull() && !propertyInfo.isKey() && !propertyInfo.isAutoIncrement()) {
            propertySb.append(NULL);
        } else {
            propertySb.append(NOT_NULL);
        }
        //是否自增
        if (propertyInfo.isAutoIncrement()) {
            propertySb.append(MYSQL_IDENTITY);
        } else {
            //默认值
            if (StrUtil.isNotBlank(propertyInfo.getDefaultValue())) {
                propertySb.append(DEFAULT).append(propertyInfo.getDefaultValue());
            }
        }
        //列备注
        if (StrUtil.isNotBlank(propertyInfo.getColumnComment())) {
            propertySb.append(StrUtil.format(COMMENT, propertyInfo.getColumnComment()));
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
        MysqlColumnTypeEnums typeEnum = MysqlColumnTypeEnums.getByValue(type);
        switch (typeEnum) {
            case VARCHAR:
            case DATETIME:
            case CHAR:
                propertySb.append(StringPool.SPACE).append(type).append(StringPool.LEFT_BRACKET);
                if (Objects.equals(type, MysqlColumnTypeEnums.DATETIME.getType())) {
                    //对类型特殊处理
                    if (length > 6 || length < 0) {
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
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 38);
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

}
