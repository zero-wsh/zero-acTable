package io.gitee.zerowsh.actable.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.constant.AcTableConstants;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.JavaTypeTurnColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.emnus.SqlTypeEnums;
import io.gitee.zerowsh.actable.service.DatabaseService;
import io.gitee.zerowsh.actable.util.AcTableUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.*;
import static io.gitee.zerowsh.actable.constant.StringConstants.*;

@Slf4j
public class MysqlImpl implements DatabaseService {

    public static final HashMap<SqlTypeEnums, String> MYSQL_EXECUTE_SQL = new HashMap<SqlTypeEnums, String>() {{
        put(SqlTypeEnums.GET_ALL_TABLE, "select table_name as name from information_schema.tables where table_schema = (select database())");
        put(SqlTypeEnums.DROP_TABLE, "drop table if exists `{}`");
        put(SqlTypeEnums.EXIST_TABLE, "select count(1) from information_schema.tables where table_name ='{}' and table_schema = (select database())");
        put(SqlTypeEnums.TABLE_STRUCTURE, "SELECT t.table_name tableName,t.table_comment tableComment," +
                " case when c.IS_NULLABLE='YES' then 1 else 0 end isNull," +
                " c.column_name columnName,c.column_comment columnComment,c.DATA_TYPE typeStr,c.COLUMN_DEFAULT defaultValue," +
                " case when c.NUMERIC_PRECISION !='' and  c.NUMERIC_PRECISION is not null then c.NUMERIC_PRECISION else  c.CHARACTER_MAXIMUM_LENGTH end length," +
                " case when c.NUMERIC_SCALE!='' and c.NUMERIC_SCALE is not null then c.NUMERIC_SCALE else c.DATETIME_PRECISION end decimalLength," +
                " case when c.column_key='PRI' then 1 else 0 end isKey,case when c.EXTRA='auto_increment' then 1 else 0 end isAutoIncrement" +
                " FROM information_schema.columns c,information_schema.tables t WHERE c.table_name = t.table_name and c.table_name='{}'" +
                " and c.table_schema = (select database()) AND t.table_schema = (SELECT DATABASE ())");
        put(SqlTypeEnums.CONSTRAINT_INFO, "select index_name constraintName ,GROUP_CONCAT(column_name order by column_name) constraintColumnName," +
                " case when non_unique=0 then case when index_name='PRIMARY' then 1 else 2 end else 3 end constraintFlag" +
                " from information_schema.statistics where table_name = '{}' and table_schema = (select database())" +
                " GROUP BY constraintName,constraintFlag");
    }};

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
    @Override
    public List<String> getCreateTableSql(TableInfo tableInfo) {
        List<String> resultList = new ArrayList<>();
        String tableName = tableInfo.getName();
        String comment = tableInfo.getComment();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        StringBuilder propertySb = new StringBuilder();
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            String columnName = propertyInfo.getColumnName();
            propertySb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, columnName));
            splicingColumnInfo(propertySb, propertyInfo, tableName);
        }
        List<String> keyList = tableInfo.getKeyList();
        if (CollectionUtil.isNotEmpty(keyList)) {
            StringBuilder pkSb = new StringBuilder();
            for (String key : tableInfo.getKeyList()) {
                pkSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, key)).append(COMMA);
            }
            propertySb.append(StrUtil.format(PRIMARY_KEY, pkSb.deleteCharAt(pkSb.length() - 1))).append(COMMA);
        }

        for (TableInfo.UniqueInfo uniqueInfo : tableInfo.getUniqueInfoList()) {
            String[] columns = uniqueInfo.getColumns();
            StringBuilder uniqueSb = new StringBuilder();
            for (String column : columns) {
                uniqueSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, column)).append(COMMA);
            }
            propertySb.append(StrUtil.format(UNIQUE_KEY, uniqueInfo.getValue(), uniqueSb.deleteCharAt(uniqueSb.length() - 1))).append(COMMA);
        }
        for (TableInfo.IndexInfo indexInfo : tableInfo.getIndexInfoList()) {
            String[] columns = indexInfo.getColumns();
            StringBuilder indexSb = new StringBuilder();
            for (String column : columns) {
                indexSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, column)).append(COMMA);
            }
            propertySb.append(StrUtil.format(INDEX_KEY, indexInfo.getValue(), indexSb.deleteCharAt(indexSb.length() - 1))).append(COMMA);
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
     * @param delConstraintSet
     * @param tableInfo
     * @param tableExistPk
     */
    private static void createPk(Set<String> delConstraintSet, TableInfo tableInfo, boolean tableExistPk) {
        List<String> keyList = tableInfo.getKeyList();
        //添加主键
        if (CollectionUtil.isNotEmpty(keyList)) {
            if ((tableExistPk && delConstraintSet.contains(MYSQL_DEL_PK)) || !tableExistPk) {
                StringBuilder keySb = new StringBuilder();
                for (String key : keyList) {
                    keySb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, key)).append(COMMA);
                }
                delConstraintSet.add(StrUtil.format(MYSQL_ADD_PK, keySb.deleteCharAt(keySb.length() - 1)));
            }
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
                if (handleUkList(constraintInfoList, uniqueInfo)) {
                    continue;
                }
                for (String column : columns) {
                    uniqueSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, column)).append(COMMA);
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
                if (handleIdxList(constraintInfoList, indexInfo)) {
                    continue;
                }
                for (String column : columns) {
                    uniqueSb.append(StrUtil.format(MYSQL_KEYWORD_HANDLE, column)).append(COMMA);
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

    @Override
    public List<String> getUpdateTableSql(TableInfo tableInfo,
                                          List<TableColumnInfo> tableColumnInfoList,
                                          List<ConstraintInfo> constraintInfoList,
                                          List<ConstraintInfo> defaultInfoList,
                                          ModelEnums modelEnums) {
        boolean tableExistPk = false;
        if (CollectionUtil.isNotEmpty(constraintInfoList)) {
            tableExistPk = constraintInfoList.stream().anyMatch(constraintInfo -> Objects.equals(constraintInfo.getConstraintFlag(), AcTableConstants.PK));
        }
        List<String> resultList = new ArrayList<>();
        TableColumnInfo firstTableColumnInfo = tableColumnInfoList.get(0);
        String tableName = firstTableColumnInfo.getTableName();
        String comment = Objects.isNull(tableInfo.getComment()) ? "" : tableInfo.getComment();
        //处理表备注
        if (!Objects.equals(comment, firstTableColumnInfo.getTableComment())) {
            resultList.add(StrUtil.format(MYSQL_ALTER_TABLE + MYSQL_COMMENT, tableName, comment));
        }

        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        //删除约束（主键，唯一键，索引）
        Set<String> delConstraintSet = new LinkedHashSet<>();
        StringBuilder updateColumnSql = new StringBuilder();
        for (TableColumnInfo tableColumnInfo : tableColumnInfoList) {
            boolean flag = false;
            boolean updateNameFlag = false;
            Iterator<TableInfo.PropertyInfo> it = propertyInfoList.iterator();
            while (it.hasNext()) {
                TableInfo.PropertyInfo propertyInfo = it.next();
                if (!Objects.equals(tableColumnInfo.getColumnName(), propertyInfo.getColumnName())) {
                    //修改列名的逻辑
                    String oldColumnName = propertyInfo.getOldColumnName();
                    if (Objects.equals(tableColumnInfo.getColumnName(), oldColumnName)) {
                        //满足条件，字段从propertyInfo.getOldColumnName()修改成propertyInfo.getColumnName()
                        updateNameFlag = true;
                    } else {
                        //根据io.gitee.zerowsh.actable.emnus.ModelEnums策略判断是否保留改字段
                        continue;
                    }
                }
                String type = propertyInfo.getType();
                String columnComment = Objects.isNull(propertyInfo.getColumnComment()) ? "" : propertyInfo.getColumnComment();
                //判断类型、是否为空、是否自增、默认值，这些是否存在修改
                boolean existUpdate = !(Objects.equals(tableColumnInfo.getTypeStr(), type))
                        || !(Objects.equals(propertyInfo.getDefaultValue(), tableColumnInfo.getDefaultValue()))
                        || !(tableColumnInfo.isNull() == (!propertyInfo.isKey() && !propertyInfo.isAutoIncrement() && propertyInfo.isNull()))
                        || tableColumnInfo.isAutoIncrement() != propertyInfo.isAutoIncrement()
                        || !Objects.equals(columnComment, tableColumnInfo.getColumnComment());

                int length = propertyInfo.getLength();
                int decimalLength = propertyInfo.getDecimalLength();
                //判断长度、精度，是否修改
                ColumnTypeEnums typeEnum = ColumnTypeEnums.getMysqlByValue(type);
                switch (typeEnum) {
                    case VARCHAR:
                    case CHAR:
                        existUpdate = existUpdate || tableColumnInfo.getLength() != AcTableUtils.handleStrLength(length);
                        break;
                    case DATETIME:
                        existUpdate = existUpdate || tableColumnInfo.getDecimalLength() != AcTableUtils.handleDateLength(length);
                        break;
                    case DECIMAL:
                    case NUMERIC:
                    case DOUBLE:
                        if (decimalLength > length) {
                            decimalLength = length;
                        }
                        length = length > 65 || length < 0 ? 10 : length;
                        decimalLength = decimalLength > 65 || decimalLength < 0 ? 2 : decimalLength;
                        existUpdate = existUpdate || tableColumnInfo.getLength() != length
                                || tableColumnInfo.getDecimalLength() != decimalLength;
                        break;
                    default:
                }
                if ((propertyInfo.isKey() != tableColumnInfo.isKey()) && tableExistPk) {
                    delConstraintSet.add(MYSQL_DEL_PK);
                }
                if (updateNameFlag) {
                    //从字段上判断是否修改了字段名
                    StringBuilder propertySb = new StringBuilder();
                    splicingColumnInfo(propertySb, propertyInfo, tableName);
                    updateColumnSql.append(StrUtil.format(MYSQL_CHANGE_COLUMN, propertyInfo.getOldColumnName(), propertyInfo.getColumnName(), propertySb));
                } else {
                    if (existUpdate) {
                        StringBuilder propertySb = new StringBuilder();
                        splicingColumnInfo(propertySb, propertyInfo, tableName);
                        updateColumnSql.append(StrUtil.format(MYSQL_MODIFY_COLUMN, propertyInfo.getColumnName(), propertySb));
                    }
                }

                flag = true;
                it.remove();
                break;
            }
            if (Objects.equals(modelEnums, ModelEnums.ADD_OR_UPDATE_OR_DEL)) {
                //如果数据库有但是实体类没有，进行删除
                if (!flag) {
                    if (tableColumnInfo.isKey() && tableExistPk) {
                        delConstraintSet.add(MYSQL_DEL_PK);
                    }
                    updateColumnSql.append(StrUtil.format(MYSQL_DEL_COLUMN, tableColumnInfo.getColumnName())).append(COMMA);
                }
            }
        }

        //如果实体类有但是数据库没有，进行新增
        if (CollectionUtil.isNotEmpty(propertyInfoList)) {
            for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
                if (propertyInfo.isKey() && tableExistPk) {
                    delConstraintSet.add(MYSQL_DEL_PK);
                }
                StringBuilder propertySb = new StringBuilder();
                splicingColumnInfo(propertySb, propertyInfo, tableName);
                updateColumnSql.append(StrUtil.format(MYSQL_ADD_COLUMN, propertyInfo.getColumnName(), propertySb));
            }
        }
        //添加主键
        createPk(delConstraintSet, tableInfo, tableExistPk);
        //添加唯一键
        createUk(delConstraintSet, tableInfo, constraintInfoList);
        //添加索引
        createIdx(delConstraintSet, tableInfo, constraintInfoList);
        if (CollectionUtil.isNotEmpty(delConstraintSet)) {
            for (String s : delConstraintSet) {
                updateColumnSql.append(s).append(COMMA);
            }
        }
        if (updateColumnSql.length() > 0) {
            String resultSql = StrUtil.format(MYSQL_ALTER_TABLE, tableName) + updateColumnSql.deleteCharAt(updateColumnSql.length() - 1);
            resultList.add(resultSql);
        }
        return resultList;
    }

    @Override
    public String handleKeyword(String var) {
        if (var.startsWith(BACKTICK) && var.endsWith(BACKTICK)) {
            var = var.replace(BACKTICK, "");
        }
        return var;
    }

    @Override
    public String javaTypeTurnColumnType(String fieldType, ColumnTypeEnums type) {
        return Objects.equals(type, ColumnTypeEnums.DEFAULT) ? JavaTypeTurnColumnTypeEnums.getMysqlByValue(fieldType) : type.getMysql();
    }

    @Override
    public String getExecuteSql(SqlTypeEnums sqlTypeEnums) {
        return MYSQL_EXECUTE_SQL.get(sqlTypeEnums);
    }

    /**
     * 拼接列信息
     *
     * @param propertySb
     * @param propertyInfo
     * @param tableName
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
            if (Objects.nonNull(propertyInfo.getDefaultValue())) {
                propertySb.append(StrUtil.format(DEFAULT, propertyInfo.getDefaultValue()));
            }
        }
        //列备注
        if (StrUtil.isNotBlank(propertyInfo.getColumnComment())) {
            propertySb.append(StrUtil.format(COMMENT, propertyInfo.getColumnComment()));
        }
        propertySb.append(COMMA);
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
        ColumnTypeEnums typeEnum = ColumnTypeEnums.getMysqlByValue(type);
        switch (typeEnum) {
            case VARCHAR:
            case DATETIME:
            case CHAR:
            case BIGINT:
                propertySb.append(SPACE).append(type).append(LEFT_BRACKET);
                if (Objects.equals(type, ColumnTypeEnums.DATETIME.getSqlServer())) {
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
                propertySb.append(RIGHT_BRACKET);
                break;

            case DECIMAL:
            case NUMERIC:
            case DOUBLE:
                propertySb.append(SPACE).append(type).append(LEFT_BRACKET);

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
                    propertySb.append(COMMA).append(2);
                } else {
                    propertySb.append(COMMA).append(decimalLength);
                }
                propertySb.append(RIGHT_BRACKET);
                break;
            default:
                propertySb.append(SPACE).append(type);
        }
    }

    /**
     * 获取数据库唯一键集合
     *
     * @param constraintInfoList
     * @return
     */
    public static boolean handleUkList(List<ConstraintInfo> constraintInfoList, TableInfo.UniqueInfo uniqueInfo) {
        Iterator<ConstraintInfo> it = constraintInfoList.iterator();
        while (it.hasNext()) {
            ConstraintInfo constraintInfo = it.next();
            if (Objects.equals(constraintInfo.getConstraintFlag(), UK)) {
                String[] columns = uniqueInfo.getColumns();
                String[] constraintColumnName = constraintInfo.getConstraintColumnName().split(COMMA);
                if (ArrayUtil.containsAll(constraintColumnName, columns)) {
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
    public static boolean handleIdxList(List<ConstraintInfo> constraintInfoList, TableInfo.IndexInfo indexInfo) {
        Iterator<ConstraintInfo> it = constraintInfoList.iterator();
        while (it.hasNext()) {
            ConstraintInfo constraintInfo = it.next();
            if (Objects.equals(constraintInfo.getConstraintFlag(), INDEX)) {
                String[] columns = indexInfo.getColumns();
                String[] constraintColumnName = constraintInfo.getConstraintColumnName().split(COMMA);
                if (ArrayUtil.containsAll(constraintColumnName, columns)) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }
}
