package io.gitee.zerowsh.actable.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.constant.AcTableConstants;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.JavaTypeTurnColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.*;
import static io.gitee.zerowsh.actable.constant.StringConstants.LEFT_BRACKET;
import static io.gitee.zerowsh.actable.constant.StringConstants.RIGHT_BRACKET;

/**
 * 达梦数据库实现
 *
 * @author zero
 */
@Slf4j
public class DmImpl implements DatabaseService {

    /**
     * 获取创建表sql，达梦数据库支持创建表时直接指定列注释
     * CREATE TABLE "t_zero" (
     * "zero" int,
     * "id" bigint NOT NULL identity(1,1),
     * "name" varchar(20) NOT NULL DEFAULT '今天',
     * "create_time" datetime(5) DEFAULT CURRENT_TIMESTAMP(5),
     * "update_time" datetime(6))
     * <p>
     * COMMENT ON TABLE "t_zero" IS '测试'
     * COMMENT ON COLUMN "t_zero"."id" IS '主键'
     * COMMENT ON COLUMN "t_zero"."name" IS '名称'
     * COMMENT ON COLUMN "t_zero"."create_time" IS '创建时间'
     * COMMENT ON COLUMN "t_zero"."update_time" IS '修改时间'
     * <p>
     * <p>
     * ALTER TABLE "t_zero" ADD CONSTRAINT "pk_t_zero" PRIMARY KEY ("id","name")
     * ALTER TABLE "t_zero" ADD CONSTRAINT "uk_createTime1748258736633921536" UNIQUE ("create_time")
     * CREATE INDEX "idx_createTime1748258736642310144" ON "t_zero" ("update_time","create_time")
     * CREATE INDEX "idx_updateTime1748258736642310145" ON "t_zero" ("update_time")
     * <p>
     *
     * @param tableInfo
     * @return
     */
    @Override
    public List<String> getCreateTableSql(TableInfo tableInfo) {
        //存储需要执行的表相关sql
        List<String> resultList = new ArrayList<>();
        //存储需要执行的字段备注sql
        List<String> addColumnCommentSqlList = new ArrayList<>();
        String tableName = tableInfo.getName();
        String comment = tableInfo.getComment();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        StringBuilder propertySb = new StringBuilder();
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            String columnName = propertyInfo.getColumnName();
            String columnComment = propertyInfo.getColumnComment();
            this.splicingColumnInfo(propertySb, propertyInfo, tableName);
            if (StrUtil.isNotBlank(columnComment)) {
                addColumnCommentSqlList.add(this.addColumnCommentSql(tableName, columnName, columnComment));
            }
        }

        //存储建表sql
        resultList.add(this.addTableSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1).toString()));
        if (StrUtil.isNotBlank(comment)) {
            //存储表备注sql
            resultList.add(this.addTableCommentSql(tableName, comment));
        }
        //存储字段备注sql
        resultList.addAll(addColumnCommentSqlList);
        //创建主键
        this.createPk(tableInfo.getKeyList(), tableName, resultList);
        //创建索引
        this.createIdx(tableInfo.getIndexInfoList(), tableName, resultList);
        //创建唯一索引
        this.createUkIdx(tableInfo.getUniqueIndexInfoList(), tableName, resultList);
        //创建唯一约束
        this.createUk(tableInfo.getUniqueInfoList(), tableName, resultList);
        return resultList;
    }

    /**
     * 创建主键
     *
     * @param keyList
     * @param tableName
     * @param resultList
     */
    private void createPk(List<String> keyList, String tableName, List<String> resultList) {
        if (CollectionUtil.isNotEmpty(keyList)) {
            resultList.add(this.addPrimaryKeySql(tableName, PK_ + tableName + IdUtil.getSnowflakeNextId(), keyList));
        }
    }

    /**
     * 创建索引
     *
     * @param indexInfoList
     * @param tableName
     * @param resultList
     */
    private void createIdx(List<TableInfo.IndexInfo> indexInfoList, String tableName, List<String> resultList) {
        if (CollectionUtil.isNotEmpty(indexInfoList)) {
            for (TableInfo.IndexInfo indexInfo : indexInfoList) {
                resultList.add(this.addIndexSql(tableName, indexInfo.getValue() + IdUtil.getSnowflakeNextId(), indexInfo.getColumns()));
            }
        }
    }

    /**
     * 创建唯一索引
     *
     * @param indexInfoList
     * @param tableName
     * @param resultList
     */
    private void createUkIdx(List<TableInfo.UniqueIndexInfo> indexInfoList, String tableName, List<String> resultList) {
        if (CollectionUtil.isNotEmpty(indexInfoList)) {
            for (TableInfo.UniqueIndexInfo uniqueIndexInfo : indexInfoList) {
                resultList.add(this.addUniqueIndexSql(tableName, uniqueIndexInfo.getValue() + IdUtil.getSnowflakeNextId(), uniqueIndexInfo.getColumns()));
            }
        }
    }

    /**
     * 创建唯一约束
     *
     * @param uniqueInfoList
     * @param tableName
     * @param resultList
     */
    private void createUk(List<TableInfo.UniqueInfo> uniqueInfoList, String tableName, List<String> resultList) {
        if (CollectionUtil.isNotEmpty(uniqueInfoList)) {
            for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
                resultList.add(this.addUniqueSql(tableName, uniqueInfo.getValue() + IdUtil.getSnowflakeNextId(), uniqueInfo.getColumns()));
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
    private void handleTableComment(List<String> list, String comment, String tableComment, String tableName) {
        if (!Objects.equals(comment, tableComment)) {
            list.add(this.getUpdateTableCommentSql(tableName, tableComment));
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
//        for (ConstraintInfo constraintInfo : constraintInfoList) {
//            Integer constraintFlag = constraintInfo.getConstraintFlag();
//            String constraintName = constraintInfo.getConstraintName();
//            switch (constraintFlag) {
//                case PK:
//                    //主键
//                    delPkConstraintSqlList.add(StrUtil.format(DROP_CONSTRAINT, tableName, constraintName));
//                    break;
//                case UK:
//                    //唯一键
//                    delUkList.add(constraintName);
//                    delUkConstraintSqlSet.add(StrUtil.format(DROP_CONSTRAINT, tableName, constraintName));
//                    break;
//                case INDEX:
//                    //索引
//                    delIdxList.add(constraintName);
//                    delIdxConstraintSqlSet.add(StrUtil.format(DROP_INDEX, constraintName, tableName));
//                    break;
//                default:
//            }
//        }
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
     * @param tableInfo           实体类获得
     * @param tableColumnInfoList 表结构获得
     * @param constraintInfoList  表结构获得
     * @param defaultInfoList     表结构获得
     * @param modelEnums
     * @return
     */
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
            resultList.add(this.getUpdateTableCommentSql(tableName, comment));
        }

        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        //删除约束（主键，唯一键，索引）
        Set<String> delConstraintSet = new LinkedHashSet<>();
        StringBuilder updateColumnSql = new StringBuilder();
        //表结构获得循环比对实体类获得
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
                        existUpdate = existUpdate || tableColumnInfo.getLength() != this.handleStrLength(length);
                        break;
                    case DATETIME:
                        existUpdate = existUpdate || tableColumnInfo.getDecimalLength() != this.handleDateLength(length);
                        break;
                    case DECIMAL:
                    case NUMERIC:
                    case DOUBLE:
                    case FLOAT:
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
                    updateColumnSql.append(StrUtil.format(MYSQL_DEL_COLUMN, tableColumnInfo.getColumnName())).append(StrUtil.COMMA);
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
//        createPk(delConstraintSet, tableInfo, tableExistPk);
//        //添加唯一键
//        createUk(delConstraintSet, tableInfo, constraintInfoList);
//        //添加索引
//        createIdx(delConstraintSet, tableInfo, constraintInfoList);
        if (CollectionUtil.isNotEmpty(delConstraintSet)) {
            for (String s : delConstraintSet) {
                updateColumnSql.append(s).append(StrUtil.COMMA);
            }
        }
        if (updateColumnSql.length() > 0) {
            String resultSql = StrUtil.format(MYSQL_ALTER_TABLE, tableName) + updateColumnSql.deleteCharAt(updateColumnSql.length() - 1);
            resultList.add(resultSql);
        }
        return resultList;
    }

    @Override
    public String delKeywordHandle(String var) {
        //左右都相同的处理
        String leftKeyword = this.leftKeyword();
        if (var.startsWith(leftKeyword) && var.endsWith(leftKeyword)) {
            var = var.replace(leftKeyword, "");
        }
        return var;
    }

    @Override
    public String addKeywordHandle(String var) {
        //左右都相同的处理
        String leftKeyword = this.leftKeyword();
        return leftKeyword + var + leftKeyword;
    }

    @Override
    public String javaTypeTurnColumnType(String fieldType, String columnType) {
        return Objects.equals(columnType, ColumnTypeConstants.DEFAULT_VALUE)
                ? JavaTypeTurnColumnTypeEnums.getDmByValue(fieldType) : columnType;
    }

    @Override
    public String leftKeyword() {
        return "\"";
    }

    @Override
    public String rightKeyword() {
        return this.leftKeyword();
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

//    /**
//     * 获取字段唯一键集合
//     *
//     * @param uniqueInfoList
//     * @return
//     */
//    public static Set<String> getPropertyUniqueSet(List<TableInfo.UniqueInfo> uniqueInfoList) {
//        Set<String> set = new HashSet<>();
//        for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
//            String[] columns = uniqueInfo.getColumns();
//            if (ArrayUtil.isNotEmpty(columns)) {
//                Arrays.sort(columns);
//                set.add(StrUtil.join(StrUtil.COMMA, columns));
//            }
//        }
//        return set;
//    }

//    /**
//     * 获取数据库唯一键集合
//     *
//     * @param constraintInfoList
//     * @return
//     */
//    public static Set<String> getDatabaseUniqueSet(List<ConstraintInfo> constraintInfoList) {
//        Set<String> set = new HashSet<>();
//        for (ConstraintInfo constraintInfo : constraintInfoList) {
//            if (Objects.equals(constraintInfo.getConstraintFlag(), UK)) {
//                set.add(constraintInfo.getConstraintColumnName());
//            }
//        }
//        return set;
//    }

//    /**
//     * 获取字段索引集合
//     *
//     * @param indexInfoList
//     * @return
//     */
//    public static Set<String> getPropertyIndexSet(List<TableInfo.IndexInfo> indexInfoList) {
//        Set<String> set = new HashSet<>();
//        for (TableInfo.IndexInfo indexInfo : indexInfoList) {
//            String[] columns = indexInfo.getColumns();
//            if (ArrayUtil.isNotEmpty(columns)) {
//                Arrays.sort(columns);
//                set.add(StrUtil.join(StrUtil.COMMA, columns));
//            }
//        }
//        return set;
//    }

//    /**
//     * 获取数据库索引集合
//     *
//     * @param constraintInfoList
//     * @return
//     */
//    public static Set<String> getDatabaseIndexSet(List<ConstraintInfo> constraintInfoList) {
//        Set<String> set = new HashSet<>();
//        for (ConstraintInfo constraintInfo : constraintInfoList) {
//            if (Objects.equals(constraintInfo.getConstraintFlag(), INDEX)) {
//                set.add(constraintInfo.getConstraintColumnName());
//            }
//        }
//        return set;
//    }

//    private static boolean handleUkConstraint(TableInfo tableInfo, TableInfo.PropertyInfo propertyInfo) {
//        List<TableInfo.UniqueInfo> uniqueInfoList = tableInfo.getUniqueInfoList();
//        for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
//            String[] columns = uniqueInfo.getColumns();
//            for (String column : columns) {
//                if (Objects.equals(column, propertyInfo.getColumnName())) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }


//    private static boolean handleIdxConstraint(TableInfo tableInfo, TableInfo.PropertyInfo propertyInfo) {
//        List<TableInfo.IndexInfo> indexInfoList = tableInfo.getIndexInfoList();
//        for (TableInfo.IndexInfo indexInfo : indexInfoList) {
//            String[] columns = indexInfo.getColumns();
//            for (String column : columns) {
//                if (Objects.equals(column, propertyInfo.getColumnName())) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

//    private static boolean handleUkConstraintDatabase(String columnName, List<ConstraintInfo> constraintInfoList) {
//        for (ConstraintInfo constraintInfo : constraintInfoList) {
//            if (Objects.equals(constraintInfo.getConstraintColumnName(), columnName)
//                    && Objects.equals(constraintInfo.getConstraintFlag(), UK)) {
//                return true;
//            }
//        }
//        return false;
//    }

//    private static boolean handleIdxConstraintDatabase(String columnName, List<ConstraintInfo> constraintInfoList) {
//        for (ConstraintInfo constraintInfo : constraintInfoList) {
//            if (Objects.equals(constraintInfo.getConstraintColumnName(), columnName)
//                    && Objects.equals(constraintInfo.getConstraintFlag(), INDEX)) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * 拼接列信息
     *
     * @param propertySb
     * @param propertyInfo
     * @param tableName
     */
    private void splicingColumnInfo(StringBuilder propertySb, TableInfo.PropertyInfo propertyInfo, String tableName) {
        String type = propertyInfo.getType();
        int length = propertyInfo.getLength();
        int decimalLength = propertyInfo.getDecimalLength();
        String columnName = propertyInfo.getColumnName();
        boolean typeLimit = propertyInfo.isTypeLimit();
        //DATETIME 6 9
        //DATE
        //DATETIME WITH TIME ZONE 0 9
        propertySb.append(StrPool.CRLF).append(this.addKeywordHandle(columnName)).append(StrPool.C_SPACE);
        switch (type) {
            case ColumnTypeConstants.DATETIME:
                propertySb.append(type).append(LEFT_BRACKET);
                if (length > 9 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 6);
                    propertySb.append(6);
                } else {
                    propertySb.append(length);
                }
                propertySb.append(RIGHT_BRACKET);
                break;
            case ColumnTypeConstants.VARCHAR:
            case ColumnTypeConstants.CHAR:
                propertySb.append(type).append(LEFT_BRACKET);
                if (length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 255);
                    propertySb.append(255);
                } else {
                    propertySb.append(length);
                }
                propertySb.append(RIGHT_BRACKET);
                break;
            case ColumnTypeConstants.FLOAT:
            case ColumnTypeConstants.DOUBLE:
                propertySb.append(type).append(LEFT_BRACKET);
                if (length > 126 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 53);
                    propertySb.append(53);
                } else {
                    propertySb.append(length);
                }
                propertySb.append(RIGHT_BRACKET);
                break;
            case ColumnTypeConstants.DECIMAL:
            case ColumnTypeConstants.NUMERIC:
                propertySb.append(type).append(LEFT_BRACKET);
                if (decimalLength > length) {
                    log.warn(COLUMN_DECIMAL_LENGTH_VALID_STR, tableName, columnName, type, decimalLength, length, length);
                    decimalLength = length;
                }
                if (length > 38 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 10);
                    propertySb.append(22);
                } else {
                    propertySb.append(length);
                }
                if (decimalLength > 38 || decimalLength < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, decimalLength, 2);
                    propertySb.append(StrUtil.COMMA).append(2);
                } else {
                    propertySb.append(StrUtil.COMMA).append(decimalLength);
                }
                propertySb.append(RIGHT_BRACKET);
                break;
            default:
                if (typeLimit && !ColumnTypeConstants.dmContains(type)) {
                    //类型限制并且没有定义这个类型使用默认字符串
                    propertySb.append(ColumnTypeConstants.VARCHAR);
                } else {
                    propertySb.append(type);
                }
        }

        //是否为空
        if (propertyInfo.isKey() || propertyInfo.isAutoIncrement() || !propertyInfo.isNull()) {
            propertySb.append(NOT_NULL);
        }
        //是否自增
        if (propertyInfo.isAutoIncrement()) {
            propertySb.append(IDENTITY);
        } else {
            //自增不能设置默认值
            if (Objects.nonNull(propertyInfo.getDefaultValue())) {
                propertySb.append(StrUtil.format(DEFAULT, propertyInfo.getDefaultValue()));
            }
        }
        propertySb.append(StrUtil.COMMA);
    }

    @Override
    public String getAllTableSql() {
        return "SELECT TABLE_NAME AS NAME FROM USER_TABLES";
    }

    @Override
    public String existTableSql(String tableName) {
        return StrUtil.format("SELECT COUNT(1) FROM USER_TABLES WHERE TABLE_NAME = '{}'", tableName);
    }

    @Override
    public String getTableStructureSql(String tableName) {
        return StrUtil.format("SELECT t.TABLE_NAME tableName,\n" +
                "\t   (select COMMENTS from user_tab_comments A1 \n" +
                "\t   WHERE t.TABLE_NAME=A1.TABLE_NAME) tableComment,\n" +
                "       t.COLUMN_NAME columnName,\n" +
                "       (select COMMENTS from user_COL_comments A2 \n" +
                "       WHERE t.COLUMN_NAME=A2.COLUMN_NAME AND t.TABLE_NAME=A2.TABLE_NAME) columnComment,\n" +
                "       t.DATA_DEFAULT columnDefault,\n" +
                "       t.CHARACTER_SET_NAME columnCharacterSetName,\n" +
                "       CASE WHEN t.NULLABLE = 'Y' THEN 1 ELSE 0 END isNull,\n" +
                "       CASE WHEN t.DATA_TYPE = 'NUMBER' THEN\n" +
                "           (CASE WHEN t.DATA_PRECISION IS NULL THEN t.DATA_TYPE\n" +
                "                 WHEN NVL(t.DATA_SCALE, 0) > 0 THEN t.DATA_TYPE || '(' || t.DATA_PRECISION || ',' || t.DATA_SCALE || ')'\n" +
                "                 ELSE t.DATA_TYPE || '(' || t.DATA_PRECISION || ')' END)\n" +
                "           ELSE t.DATA_TYPE END typeStr,\n" +
                "       CASE WHEN (SELECT count(1) FROM USER_CONS_COLUMNS T4, USER_CONSTRAINTS T5\n" +
                "                  WHERE T4.CONSTRAINT_NAME = T5.CONSTRAINT_NAME AND T5.CONSTRAINT_TYPE = 'P'\n" +
                "                 AND t.TABLE_NAME = T5.TABLE_NAME(+) AND t.COLUMN_NAME = T4.COLUMN_NAME(+)) > 0 THEN 1\n" +
                "            ELSE 0 END isKey,\n" +
                "       t.DATA_DEFAULT defaultValue,\n" +
                "       t.DATA_LENGTH length,\n" +
                "       t.DATA_SCALE decimalLength,\n" +
                "       CASE WHEN (SELECT count(1) FROM SYS.SYSCOLUMNS a,user_tables b,sys.sysobjects c\n" +
                "                 WHERE a.INFO2 & 0x01 = 0x01 AND a.id = c.id AND c.name = b.table_name AND\n" +
                "       b.table_name = t.TABLE_NAME AND a.name = t.COLUMN_NAME) > 0 THEN 1 ELSE 0 END  isAutoIncrement\n" +
                "FROM user_TAB_COLUMNS t WHERE t.TABLE_NAME = '{}'", tableName);
    }

    @Override
    public String getConstraintInfoSql(String tableName) {
        return StrUtil.format("SELECT T5.CONSTRAINT_TYPE,T5.CONSTRAINT_NAME,wm_concat(T4.COLUMN_NAME) COLUMN_NAME \n" +
                "FROM USER_CONS_COLUMNS T4, USER_CONSTRAINTS T5\n" +
                "WHERE T4.CONSTRAINT_NAME = T5.CONSTRAINT_NAME and T5.TABLE_NAME = '{}' \n" +
                "group by T5.CONSTRAINT_TYPE,T5.CONSTRAINT_NAME", tableName);
    }

    @Override
    public String getDefaultInfoSql(String tableName) {
        return null;
    }

    @Override
    public String addTableCommentSql(String tableName, String comment) {
        //COMMENT ON TABLE "t_zero" IS '测试'
        return StrUtil.format("COMMENT ON TABLE {} IS '{}'", this.addKeywordHandle(tableName), comment);
    }

    @Override
    public String addColumnCommentSql(String tableName, String columnName, String comment) {
        //COMMENT ON COLUMN "t_zero"."id" IS '主键'
        return StrUtil.format("COMMENT ON COLUMN {}.{} IS '{}'",
                this.addKeywordHandle(tableName), this.addKeywordHandle(columnName), comment);
    }

    @Override
    public String addPrimaryKeySql(String tableName, String constraintName, List<String> columnList) {
        //ALTER TABLE "t_zero" ADD CONSTRAINT "pk_t_zero" PRIMARY KEY ("id","name")
        String column = CollectionUtil.join(columnList, StrPool.COMMA, this::addKeywordHandle);
        return StrUtil.format("ALTER TABLE {} ADD CONSTRAINT {} PRIMARY KEY ({})",
                this.addKeywordHandle(tableName), this.addKeywordHandle(constraintName), column);
    }

    @Override
    public String addIndexSql(String tableName, String indexName, List<TableInfo.Index> columns) {
        //CREATE INDEX "idx_createTime" ON "t_zero" ("update_time" desc,"create_time" asc)
        String column = CollectionUtil.join(columns, StrPool.COMMA, (index) ->
                this.addKeywordHandle(index.getColumn()) + (index.isAsc() ? ASC : DESC));
        return StrUtil.format("CREATE INDEX {} ON {} ({})",
                this.addKeywordHandle(indexName), this.addKeywordHandle(tableName), column);
    }

    @Override
    public String addUniqueIndexSql(String tableName, String constraintName, List<TableInfo.Index> columns) {
        //CREATE UNIQUE INDEX id_idx ON T_ZERO("ID" DESC)
        String column = CollectionUtil.join(columns, StrPool.COMMA, (index) ->
                this.addKeywordHandle(index.getColumn()) + (index.isAsc() ? ASC : DESC));
        return StrUtil.format("CREATE UNIQUE INDEX {} ON {}({})",
                this.addKeywordHandle(constraintName), this.addKeywordHandle(tableName), column);
    }

    @Override
    public String addUniqueSql(String tableName, String constraintName, List<TableInfo.Index> columns) {
        //ALTER TABLE "t_zero" ADD CONSTRAINT "uk_createTime" UNIQUE ("create_time")
        String column = CollectionUtil.join(columns, StrPool.COMMA, (index) -> this.addKeywordHandle(index.getColumn()));
        return StrUtil.format("ALTER TABLE {} ADD CONSTRAINT {} UNIQUE ({})",
                this.addKeywordHandle(tableName), this.addKeywordHandle(constraintName), column);
    }

    @Override
    public String getUpdateTableCommentSql(String tableName, String tableComment) {
        //comment on table  "t_zero1" is '111';
        return StrUtil.format("comment on table  {} is '{}'", this.addKeywordHandle(tableName), tableComment);
    }

    @Override
    public String getUpdateColumnCommentSql(String tableName, String columnName, String columnComment) {
        //comment on column "t_zero1"."zero" is '212';
        return StrUtil.format("comment on column {}.{} is '{}'",
                this.addKeywordHandle(tableName), this.addKeywordHandle(columnName), columnComment);
    }
}
