package io.gitee.zerowsh.actable.util.sql;

import cn.hutool.core.collection.CollectionUtil;
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
        List<String> keyList = tableInfo.getKeyList();
        if (CollectionUtil.isNotEmpty(keyList)) {
            StringBuilder pkSb = new StringBuilder();
            for (String key : tableInfo.getKeyList()) {
                pkSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, key)).append(StringPool.COMMA);
            }
            propertySb.append(StrUtil.format(PRIMARY_KEY, pkSb.deleteCharAt(pkSb.length() - 1))).append(StringPool.COMMA);
        }

        for (TableInfo.UniqueInfo uniqueInfo : tableInfo.getUniqueInfoList()) {
            String[] columns = uniqueInfo.getColumns();
            StringBuilder uniqueSb = new StringBuilder();
            for (String column : columns) {
                uniqueSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, column)).append(StringPool.COMMA);
            }
            propertySb.append(StrUtil.format(UNIQUE_KEY, uniqueInfo.getValue(), uniqueSb.deleteCharAt(uniqueSb.length() - 1))).append(StringPool.COMMA);
        }
        for (TableInfo.IndexInfo indexInfo : tableInfo.getIndexInfoList()) {
            String[] columns = indexInfo.getColumns();
            StringBuilder indexSb = new StringBuilder();
            for (String column : columns) {
                indexSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, column)).append(StringPool.COMMA);
            }
            propertySb.append(StrUtil.format(INDEX_KEY, indexInfo.getValue(), indexSb.deleteCharAt(indexSb.length() - 1))).append(StringPool.COMMA);
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
    private static void createPk(Set<String> delConstraintSet, TableInfo tableInfo) {
        List<String> keyList = tableInfo.getKeyList();
        String tableName = tableInfo.getName();
        //添加主键时肯定会删除主键
        if (delConstraintSet.contains(MYSQL_DEL_PK) && CollectionUtil.isNotEmpty(keyList)) {
            StringBuilder keySb = new StringBuilder();
            for (String key : keyList) {
                keySb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, key)).append(StringPool.COMMA);
            }
            delConstraintSet.add(StrUtil.format(MYSQL_ADD_PK, keySb.deleteCharAt(keySb.length() - 1)));
        }
    }

    /**
     * 创建唯一键
     *
     * @param delConstraintSet
     * @param tableInfo
     * @param constraintInfoList 数据库约束信息（主键、唯一键、索引）
     */
    private static void createUk(Set<String> delConstraintSet, TableInfo tableInfo, List<ConstraintInfo> constraintInfoList) {
        //实体类所有唯一键
        List<TableInfo.UniqueInfo> uniqueInfoList = tableInfo.getUniqueInfoList();
        if (CollectionUtil.isNotEmpty(uniqueInfoList)) {
            for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
                String value = uniqueInfo.getValue();
                String[] columns = uniqueInfo.getColumns();
                StringBuilder uniqueSb = new StringBuilder();
                if (handleUkList(constraintInfoList, columns)) {
                    continue;
                }
                for (String column : columns) {
                    uniqueSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, column)).append(StringPool.COMMA);
                }
                delConstraintSet.add(StrUtil.format(MYSQL_ADD_UNIQUE, value, uniqueSb.deleteCharAt(uniqueSb.length() - 1)));
            }
        }
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            if (Objects.equals(constraintInfo.getConstraintFlag(), UK)) {
                delConstraintSet.add(StrUtil.format(MYSQL_DEL_INDEX, constraintInfo.getConstraintName()));
            }
        }
    }


    /**
     * 创建索引
     *
     * @param delConstraintSet
     * @param tableInfo
     * @param constraintInfoList 数据库约束信息（主键、唯一键、索引）
     */
    private static void createIdx(Set<String> delConstraintSet, TableInfo tableInfo, List<ConstraintInfo> constraintInfoList) {
        List<TableInfo.IndexInfo> indexInfoList = tableInfo.getIndexInfoList();
        if (CollectionUtil.isNotEmpty(indexInfoList)) {
            for (TableInfo.IndexInfo indexInfo : indexInfoList) {
                String value = indexInfo.getValue();
                String[] columns = indexInfo.getColumns();
                StringBuilder uniqueSb = new StringBuilder();
                if (handleIdxList(constraintInfoList, columns)) {
                    continue;
                }
                for (String column : columns) {
                    uniqueSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, column)).append(StringPool.COMMA);
                }
                delConstraintSet.add(StrUtil.format(MYSQL_ADD_INDEX, value, uniqueSb.deleteCharAt(uniqueSb.length() - 1)));
            }
        }
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            if (Objects.equals(constraintInfo.getConstraintFlag(), INDEX)) {
                delConstraintSet.add(StrUtil.format(MYSQL_DEL_INDEX, constraintInfo.getConstraintName()));
            }
        }
    }

    /**
     * 获取修改表sql
     *
     * @param tableInfo
     * @param tableColumnInfoList
     * @param constraintInfoList
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
            resultList.add(StrUtil.format(MYSQL_ALTER_TABLE + MYSQL_COMMENT, tableName, tableInfo.getComment()));
        }

        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        //删除约束（主键，唯一键，索引）
        Set<String> delConstraintSet = new LinkedHashSet<>();
        StringBuilder updateColumnSql = new StringBuilder();
        for (TableColumnInfo tableColumnInfo : tableColumnInfoList) {
            boolean flag = false;
            Iterator<TableInfo.PropertyInfo> it = propertyInfoList.iterator();
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
                ColumnTypeEnums typeEnum = getMysqlByValue(type);
                switch (typeEnum) {
                    case VARCHAR:
                    case CHAR:
                        existUpdate = existUpdate || !(Objects.equals(tableColumnInfo.getLength(), AcTableUtils.handleStrLength(length)));
                        break;
                    case DATETIME:
                        existUpdate = existUpdate || !(Objects.equals(tableColumnInfo.getDecimalLength(), AcTableUtils.handleDateLength(length)));
                        break;
                    case DECIMAL:
                    case NUMERIC:
                        if (decimalLength > length) {
                            decimalLength = length;
                        }
                        length = length > 65 || length < 0 ? 10 : length;
                        decimalLength = decimalLength > 65 || decimalLength < 0 ? 2 : decimalLength;
                        existUpdate = existUpdate || !(Objects.equals(tableColumnInfo.getLength(), length))
                                || !(Objects.equals(tableColumnInfo.getDecimalLength(), decimalLength));
                        break;
                    default:
                }
                if (propertyInfo.isKey() != tableColumnInfo.isKey()) {
                    delConstraintSet.add(MYSQL_DEL_PK);
                }
                if (existUpdate) {
                    StringBuilder propertySb = new StringBuilder();
                    splicingColumnInfo(propertySb, propertyInfo, tableName);
                    updateColumnSql.append(StrUtil.format(MYSQL_UPDATE_COLUMN, propertyInfo.getColumnName(), propertySb));
                }
                flag = true;
                it.remove();
                break;
            }
            if (Objects.equals(modelEnums, ModelEnums.ADD_OR_UPDATE_OR_DEL)) {
                //如果数据库有但是实体类没有，进行删除
                if (!flag) {
                    if (tableColumnInfo.isKey()) {
                        delConstraintSet.add(MYSQL_DEL_PK);
                    }
                    updateColumnSql.append(StrUtil.format(MYSQL_DEL_COLUMN, tableColumnInfo.getColumnName())).append(StringPool.COMMA);
                }
            }
        }

        //如果实体类有但是数据库没有，进行新增
        if (CollectionUtil.isNotEmpty(propertyInfoList)) {
            for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
                if (propertyInfo.isKey()) {
                    delConstraintSet.add(MYSQL_DEL_PK);
                }
                StringBuilder propertySb = new StringBuilder();
                splicingColumnInfo(propertySb, propertyInfo, tableName);
                updateColumnSql.append(StrUtil.format(MYSQL_ADD_COLUMN, propertyInfo.getColumnName(), propertySb));
            }
        }
        //添加主键
        createPk(delConstraintSet, tableInfo);
        //添加唯一键
        createUk(delConstraintSet, tableInfo, constraintInfoList);
//        //添加索引
        createIdx(delConstraintSet, tableInfo, constraintInfoList);
        if (CollectionUtil.isNotEmpty(delConstraintSet)) {
            for (String s : delConstraintSet) {
                updateColumnSql.append(s).append(StringPool.COMMA);
            }
        }
        if (updateColumnSql.length() > 0) {
            String resultSql = StrUtil.format(MYSQL_ALTER_TABLE, tableName) + updateColumnSql.deleteCharAt(updateColumnSql.length() - 1);
            resultList.add(resultSql);
        }
        return resultList;
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
        ColumnTypeEnums typeEnum = getMysqlByValue(type);
        switch (typeEnum) {
            case VARCHAR:
            case DATETIME:
            case CHAR:
                propertySb.append(StringPool.SPACE).append(type).append(StringPool.LEFT_BRACKET);
                if (Objects.equals(type, ColumnTypeEnums.DATETIME.getType())) {
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
                if (length > 65 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 10);
                    propertySb.append(10);
                } else {
                    propertySb.append(length);
                }
                if (decimalLength > 65 || decimalLength < 0) {
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
     * 获取数据库唯一键集合
     *
     * @param constraintInfoList
     * @return
     */
    public static boolean handleUkList(List<ConstraintInfo> constraintInfoList, String[] columns) {
        Set<String> set = new HashSet<>();
        Iterator<ConstraintInfo> it = constraintInfoList.iterator();
        while (it.hasNext()) {
            ConstraintInfo constraintInfo = it.next();
            if (Objects.equals(constraintInfo.getConstraintFlag(), UK)) {
                Arrays.sort(columns);
                if (Objects.equals(StrUtil.join(StringPool.COMMA, columns), constraintInfo.getConstraintColumnName())) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取数据库唯一键集合
     *
     * @param constraintInfoList
     * @return
     */
    public static boolean handleIdxList(List<ConstraintInfo> constraintInfoList, String[] columns) {
        Set<String> set = new HashSet<>();
        Iterator<ConstraintInfo> it = constraintInfoList.iterator();
        while (it.hasNext()) {
            ConstraintInfo constraintInfo = it.next();
            if (Objects.equals(constraintInfo.getConstraintFlag(), INDEX)) {
                Arrays.sort(columns);
                if (Objects.equals(StrUtil.join(StringPool.COMMA, columns), constraintInfo.getConstraintColumnName())) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    private static final Map<String, ColumnTypeEnums> JAVA_TURN_MYSQL_MAP = new HashMap<String, ColumnTypeEnums>() {{
        put("java.lang.String", ColumnTypeEnums.VARCHAR);
        put("java.lang.Long", ColumnTypeEnums.BIGINT);
        put("long", ColumnTypeEnums.BIGINT);
        put("java.lang.Integer", ColumnTypeEnums.INT);
        put("int", ColumnTypeEnums.INT);
        put("java.lang.Boolean", ColumnTypeEnums.BIT);
        put("java.lang.boolean", ColumnTypeEnums.BIT);
        put("java.util.Date", ColumnTypeEnums.DATETIME);
        put("java.sql.Timestamp", ColumnTypeEnums.DATETIME);
        put("java.time.LocalDate", ColumnTypeEnums.DATETIME);
        put("java.time.LocalDateTime", ColumnTypeEnums.DATETIME);
        put("java.math.BigDecimal", ColumnTypeEnums.NUMERIC);
        put("java.lang.Double", ColumnTypeEnums.NUMERIC);
        put("double", ColumnTypeEnums.NUMERIC);
        put("java.lang.Float", ColumnTypeEnums.FLOAT);
        put("float", ColumnTypeEnums.FLOAT);
        put("char", ColumnTypeEnums.CHAR);
    }};

    /**
     * java类型转数据库类型
     *
     * @param key
     * @return
     */
    public static String getJavaTurnMysqlValue(String key) {
        ColumnTypeEnums ColumnTypeEnums = JAVA_TURN_MYSQL_MAP.get(key);
        return Objects.isNull(ColumnTypeEnums) ? ColumnTypeEnums.VARCHAR.getType() : ColumnTypeEnums.getType();
    }

    public static ColumnTypeEnums getMysqlByValue(String type) {
        for (ColumnTypeEnums types : ColumnTypeEnums.values()) {
            if (Objects.equals(types.getType(), type)) {
                return types;
            }
        }
        return null;
    }
}
