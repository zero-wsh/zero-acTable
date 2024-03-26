package io.gitee.zerowsh.actable.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import io.gitee.zerowsh.actable.dto.ColumnTypeInfo;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.JavaTypeTurnColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

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
        resultList.add(this.addTableSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1).toString(), null));
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
     * 获取修改表sql
     *
     * @param tableInfo          实体类获得
     * @param tableColumnInfoMap 表结构获得
     * @param constraintInfoList 表结构获得
     * @param defaultInfoList    表结构获得
     * @param modelEnums
     * @return
     */
    @Override
    public List<String> getUpdateTableSql(TableInfo tableInfo,
                                          Map<String, TableColumnInfo> tableColumnInfoMap,
                                          List<ConstraintInfo> constraintInfoList,
                                          List<ConstraintInfo> defaultInfoList,
                                          ModelEnums modelEnums) {
        List<String> resultList = new ArrayList<>();
        String comment = tableInfo.getComment();
        String tableName = tableInfo.getName();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        int count = 0;
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            String columnName = propertyInfo.getColumnName();
            String oldColumnName = propertyInfo.getOldColumnName();
            TableColumnInfo tableColumnInfo = tableColumnInfoMap.get(columnName);
            TableColumnInfo oldTableColumnInfo = tableColumnInfoMap.get(oldColumnName);
            if (!Objects.equals(columnName, oldColumnName)) {
                if (Objects.nonNull(tableColumnInfo) && Objects.nonNull(oldTableColumnInfo)) {
                    //如果columnName、oldColumnName在数据库中都有，存在问题
                    throw new RuntimeException(StrUtil.format("无法将表【{}】，字段【{}】修改成【{}】，两个字段都在表中存在！",
                            tableName, oldColumnName, columnName));
                }
                if (Objects.nonNull(oldTableColumnInfo)) {
                    //修改
                    resultList.add(this.getUpdateColumnNameSql(tableName, oldColumnName, columnName, null));
                    tableColumnInfo = oldTableColumnInfo;
                } else if (Objects.isNull(tableColumnInfo)) {
                    //写错了会导致新增字段和删除字段
                    throw new RuntimeException(StrUtil.format("无法将表【{}】，字段【{}】修改成【{}】，两个字段都不存在于表中！",
                            tableName, oldColumnName, columnName));
                }
            }
            if (Objects.isNull(tableColumnInfo) && Objects.isNull(oldTableColumnInfo)) {
                //如果columnName、oldColumnName在数据库中都没有，新增columnName
                StringBuilder propertySb = new StringBuilder();
                this.splicingColumnInfo(propertySb, propertyInfo, tableName);
                resultList.add(this.getAddColumnSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1)));
                //添加字段备注
                resultList.add(this.addColumnCommentSql(tableName, columnName, propertyInfo.getColumnComment()));
                continue;
            }
            //新旧字段一起删除，代表处理过
            tableColumnInfoMap.remove(oldColumnName);
            tableColumnInfoMap.remove(columnName);
            if (count == 0) {
                //处理表备注
                if (!Objects.equals(comment, tableColumnInfo.getTableComment())) {
                    resultList.add(this.getUpdateTableCommentSql(tableName, comment));
                }
            }
            count++;
            //修改--判断类型、是否为空、是否自增、默认值 ,这些是否存在修改
            boolean existUpdate = (propertyInfo.isTypeLimit() && !StrUtil.equalsIgnoreCase(tableColumnInfo.getTypeStr(), propertyInfo.getType()))
                    || !(tableColumnInfo.isNull() == (!propertyInfo.isKey() && !propertyInfo.isAutoIncrement()
                    && propertyInfo.isNull()))
                    || tableColumnInfo.isAutoIncrement() != propertyInfo.isAutoIncrement()
                    || !(StrUtil.equalsIgnoreCase(tableColumnInfo.getDefaultValue(), propertyInfo.getDefaultValue()));

            //判断长度、精度，是否修改
            long length1 = tableColumnInfo.getLength();
            long decimalLength1 = tableColumnInfo.getDecimalLength();
            if (!existUpdate) {
                ColumnTypeInfo columnTypeInfo = this.handleType(propertyInfo);
                Long length = columnTypeInfo.getLength();
                Long decimalLength = columnTypeInfo.getDecimalLength();
                boolean flag = columnTypeInfo.isFlag();
                if (flag) {
                    //其他的类型不用验证长度和精度
                    if (!propertyInfo.isTypeLimit()) {
                        //拼接数据库类型和自定义的字符串比较
                        String typeStr = tableColumnInfo.getTypeStr() + LEFT_BRACKET + length1;
                        if (Objects.nonNull(decimalLength)) {
                            typeStr = typeStr + StrUtil.COMMA + decimalLength1;
                        }
                        typeStr = typeStr + RIGHT_BRACKET;
                        if (!StrUtil.equalsIgnoreCase(typeStr, propertyInfo.getType())) {
                            existUpdate = true;
                        }
                    }
                } else {
                    if (Objects.isNull(decimalLength)) {
                        //没有精度，只比较长度
                        if (Objects.equals(columnTypeInfo.getTypeStr(), ColumnTypeConstants.DATETIME)) {
                            //todo 存在差异单独处理
                            existUpdate = !Objects.equals(decimalLength1, length);
                        } else {
                            existUpdate = !Objects.equals(length1, length);
                        }

                    } else {
                        existUpdate = !Objects.equals(length1, length) || !Objects.equals(decimalLength1, decimalLength);
                    }
                }
            }
            if (existUpdate) {
                //存在修改
                StringBuilder propertySb = new StringBuilder();
                this.splicingColumnInfo(propertySb, propertyInfo, tableName);
                resultList.add(this.getUpdateColumnSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1)));

            }
            if (!StrUtil.equalsIgnoreCase(tableColumnInfo.getColumnComment(), propertyInfo.getColumnComment())) {
                //修改字段备注
                resultList.add(this.getUpdateColumnCommentSql(tableName, columnName, propertyInfo.getColumnComment()));
            }
        }

        //主键
        List<String> keyList = tableInfo.getKeyList();
        //唯一键
        List<TableInfo.UniqueInfo> uniqueInfoList = tableInfo.getUniqueInfoList();
        //唯一索引
        List<TableInfo.UniqueIndexInfo> uniqueIndexInfoList = tableInfo.getUniqueIndexInfoList();
        //普通索引
        List<TableInfo.IndexInfo> indexInfoList = tableInfo.getIndexInfoList();
        Iterator<ConstraintInfo> iterator = constraintInfoList.iterator();
        /*
         * 主键：可以比对并修改主键字段
         * 唯一键：只能判断是否完全相同，无法判断是修改；如果不相同就只有新增
         * 唯一索引：只能判断是否完全相同，无法判断是修改；如果不相同就只有新增
         * 普通索引：只能判断是否完全相同，无法判断是修改；如果不相同就只有新增
         * 唯一索引和普通索引支持创建索引字段排序，所以不仅要比对字段还要比对排序
         */
        while (iterator.hasNext()) {
            ConstraintInfo constraintInfo = iterator.next();
            List<String> list = Arrays.asList(constraintInfo.getConstraintColumnName().split(StrPool.COMMA));
            List<String> sortList = Arrays.asList(constraintInfo.getIndexSortStr().split(StrPool.COMMA));
            if (Objects.equals(constraintInfo.getConstraintFlag(), PK)) {
                //判断是否完全相等
                // 一个表只会查出来一个主键名称，一个主键名称对应多个字段
                if (!new HashSet<>(list).equals(new HashSet<>(keyList))) {
                    //修改
                    resultList.add(this.getUpdatePkSql(tableName, constraintInfo.getConstraintName(), keyList));
                }
                //处理过了
                keyList.clear();
                iterator.remove();
            } else if (Objects.equals(constraintInfo.getConstraintFlag(), UK)) {
                //唯一键
                Iterator<TableInfo.UniqueInfo> uniqueInfoIterator = uniqueInfoList.iterator();
                while (uniqueInfoIterator.hasNext()) {
                    TableInfo.UniqueInfo next = uniqueInfoIterator.next();
                    List<String> uniqueList = next.getColumns().stream().map(TableInfo.Index::getColumn).collect(Collectors.toList());
                    //如果完全相等就在集合中删除，否者新增
                    if (new HashSet<>(list).equals(new HashSet<>(uniqueList))) {
                        uniqueInfoIterator.remove();
                        iterator.remove();
                        break;
                    }
                }
            } else if (Objects.equals(constraintInfo.getConstraintFlag(), INDEX)) {
                //普通索引
                Iterator<TableInfo.IndexInfo> indexInfoIterator = indexInfoList.iterator();
                while (indexInfoIterator.hasNext()) {
                    if (this.handleIndex(indexInfoIterator.next().getColumns(), list, sortList)) {
                        indexInfoIterator.remove();
                        iterator.remove();
                        break;
                    }
                }
            } else {
                //唯一索引
                Iterator<TableInfo.UniqueIndexInfo> uniqueIndexInfoIterator = uniqueIndexInfoList.iterator();
                while (uniqueIndexInfoIterator.hasNext()) {
                    if (this.handleIndex(uniqueIndexInfoIterator.next().getColumns(), list, sortList)) {
                        uniqueIndexInfoIterator.remove();
                        iterator.remove();
                        break;
                    }
                }
            }
        }
        //删除表中多余的
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            if (Objects.equals(constraintInfo.getConstraintFlag(), UK)) {
                //唯一约束
                resultList.add(this.getDropConstraintSql(tableName, constraintInfo.getConstraintName()));
            } else {
                //普通索引+唯一索引
                resultList.add(this.getDropIndexSql(constraintInfo.getConstraintName()));
            }
        }
        if (CollectionUtil.isNotEmpty(keyList)) {
            //新增主键
            this.createPk(keyList, tableName, resultList);
        }
        if (CollectionUtil.isNotEmpty(uniqueInfoList)) {
            //新增唯一键
            this.createUk(uniqueInfoList, tableName, resultList);
        }
        if (CollectionUtil.isNotEmpty(indexInfoList)) {
            //新增普通索引
            this.createIdx(indexInfoList, tableName, resultList);
        }
        if (CollectionUtil.isNotEmpty(uniqueIndexInfoList)) {
            //新增唯一索引
            this.createUkIdx(uniqueIndexInfoList, tableName, resultList);
        }

        if (Objects.equals(modelEnums, ModelEnums.ADD_OR_UPDATE_OR_DEL)) {
            //如果数据库有但是实体类没有，进行删除
            if (CollectionUtil.isNotEmpty(tableColumnInfoMap)) {
                for (Map.Entry<String, TableColumnInfo> map : tableColumnInfoMap.entrySet()) {
                    TableColumnInfo value = map.getValue();
                    resultList.add(this.getDelColumnSql(value.getTableName(), value.getColumnName()));
                }
            }
        }
        return resultList;
    }

    private boolean handleIndex(List<TableInfo.Index> columns, List<String> list, List<String> sortList) {
        List<String> uniqueList = columns.stream().map(TableInfo.Index::getColumn).collect(Collectors.toList());
        List<String> uniqueSortList = columns.stream().map(a -> {
            if (a.isAsc()) {
                return StrUtil.trim(ASC);
            } else {
                return StrUtil.trim(DESC);
            }
        }).collect(Collectors.toList());
        //如果完全相等就在集合中删除，否者新增
        return (new HashSet<>(list).equals(new HashSet<>(uniqueList)))
                && (new HashSet<>(sortList).equals(new HashSet<>(uniqueSortList)));
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
     * 拼接列信息
     *
     * @param propertySb
     * @param propertyInfo
     * @param tableName
     */
    private void splicingColumnInfo(StringBuilder propertySb, TableInfo.PropertyInfo propertyInfo, String tableName) {
        String type = propertyInfo.getType();
        String columnName = propertyInfo.getColumnName();
        boolean typeLimit = propertyInfo.isTypeLimit();
        propertySb.append(StrPool.CRLF).append(this.addKeywordHandle(columnName)).append(StrPool.C_SPACE);
        ColumnTypeInfo columnTypeInfo = this.handleType(propertyInfo);
        Long length = columnTypeInfo.getLength();
        Long decimalLength = columnTypeInfo.getDecimalLength();
        if (columnTypeInfo.isFlag()) {
            if (typeLimit && !ColumnTypeConstants.dmContains(type)) {
                //类型限制并且没有定义这个类型使用默认字符串
                propertySb.append(ColumnTypeConstants.VARCHAR);
            } else {
                propertySb.append(type);
            }
        } else {
            propertySb.append(type).append(LEFT_BRACKET).append(length);
            if (Objects.nonNull(decimalLength)) {
                propertySb.append(StrUtil.COMMA);
                propertySb.append(decimalLength);
            }
            propertySb.append(RIGHT_BRACKET);
        }

        //是否为空
        if (propertyInfo.isKey() || propertyInfo.isAutoIncrement() || !propertyInfo.isNull()) {
            propertySb.append(NOT_NULL);
        } else {
            propertySb.append(NULL);
        }
        //是否自增
        if (propertyInfo.isAutoIncrement()) {
            propertySb.append(IDENTITY);
        } else {
            //自增不能设置默认值
            String defaultValue = propertyInfo.getDefaultValue();
            if (Objects.nonNull(defaultValue)) {
                propertySb.append(StrUtil.format(NumberUtil.isNumber(defaultValue) ? DEFAULT : DEFAULT2, defaultValue));
            }
        }
        propertySb.append(StrUtil.COMMA);
    }

    private ColumnTypeInfo handleType(TableInfo.PropertyInfo propertyInfo) {
        long length = propertyInfo.getLength();
        long decimalLength = propertyInfo.getDecimalLength();
        String type = propertyInfo.getType();
        String columnName = propertyInfo.getColumnName();
        String tableName = propertyInfo.getTableName();
        ColumnTypeInfo columnTypeInfo = new ColumnTypeInfo();
        switch (type) {
            case ColumnTypeConstants.DATETIME:
                if (length > 9 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 6);
                    length = 6;
                }
                columnTypeInfo.setLength(length).setTypeStr(type);
                break;
            case ColumnTypeConstants.VARCHAR:
            case ColumnTypeConstants.CHAR:
                if (length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 255);
                    length = 255;
                }
                columnTypeInfo.setLength(length).setTypeStr(type);
                break;
            case ColumnTypeConstants.FLOAT:
            case ColumnTypeConstants.DOUBLE:
                if (length > 126 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 53);
                    length = 53;
                }
                columnTypeInfo.setLength(length).setTypeStr(type);
                break;
            case ColumnTypeConstants.DECIMAL:
            case ColumnTypeConstants.NUMERIC:
                if (length > 38 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 22);
                    length = 22;
                }
                if (decimalLength > 38 || decimalLength > length || decimalLength < 0) {
                    log.warn(COLUMN_DECIMAL_LENGTH_VALID_STR, tableName, columnName, type, decimalLength, length, 2);
                    decimalLength = 2;
                }
                columnTypeInfo.setLength(length).setTypeStr(type).setDecimalLength(decimalLength);
                break;
            default:
                //其他的类型不用验证长度和精度
                columnTypeInfo.setLength(length).setTypeStr(type).setDecimalLength(decimalLength).setFlag(true);
        }
        return columnTypeInfo;
    }

    @Override
    public String getAllTableSql() {
        return "SELECT TABLE_NAME AS NAME FROM ALL_TABLES WHERE OWNER=SF_GET_SCHEMA_NAME_BY_ID(CURRENT_SCHID)";
    }

    @Override
    public String existTableSql(String tableName) {
        return StrUtil.format("SELECT COUNT(1) FROM ALL_TABLES WHERE TABLE_NAME = '{}' and OWNER=SF_GET_SCHEMA_NAME_BY_ID(CURRENT_SCHID)", tableName);
    }

    @Override
    public String getTableStructureSql(String tableName) {
        return StrUtil.format("SELECT t.TABLE_NAME tableName,\n" +
                "\t   (select COMMENTS from all_tab_comments A1 \n" +
                "\t   WHERE t.TABLE_NAME=A1.TABLE_NAME and t.OWNER=A1.OWNER) tableComment,\n" +
                "       t.COLUMN_NAME columnName,\n" +
                "       (select COMMENTS from all_COL_comments A2 \n" +
                "       WHERE  t.TABLE_NAME=A2.TABLE_NAME \n" +
                "       AND t.OWNER=A2.SCHEMA_NAME AND t.COLUMN_NAME=A2.COLUMN_NAME ) columnComment,\n" +
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
                "       case when t.DATA_PRECISION is null then t.DATA_LENGTH else t.DATA_PRECISION end length,\n" +
                "       t.DATA_SCALE decimalLength,\n" +
                "       CASE WHEN (SELECT count(1) FROM SYS.SYSCOLUMNS a,user_tables b,sys.sysobjects c\n" +
                "                 WHERE a.INFO2 & 0x01 = 0x01 AND a.id = c.id AND c.name = b.table_name AND\n" +
                "       b.table_name = t.TABLE_NAME AND a.name = t.COLUMN_NAME) > 0 THEN 1 ELSE 0 END  isAutoIncrement\n" +
                "FROM all_TAB_COLUMNS t WHERE t.TABLE_NAME = '{}' and t.OWNER=SF_GET_SCHEMA_NAME_BY_ID(CURRENT_SCHID)", tableName);
    }

    @Override
    public String getConstraintInfoSql(String tableName) {
        return StrUtil.format("SELECT \n" +
                "    IFNULL(C.CONSTRAINT_NAME,IC.INDEX_NAME) constraintName,\n" +
                "    LISTAGG(IC.COLUMN_NAME, ',') WITHIN GROUP (ORDER BY IC.COLUMN_POSITION) AS constraintColumnName,\n" +
                "    LISTAGG(IC.descend, ',') WITHIN GROUP (ORDER BY IC.COLUMN_POSITION) AS indexSortStr,\n" +
                "     case when C.CONSTRAINT_TYPE='P' then 1\n" +
                "     when C.CONSTRAINT_TYPE='U' then 2 \n" +
                "     when I.UNIQUENESS='NONUNIQUE' then 3 \n" +
                "     else 4 end constraintFlag\n" +
                "FROM \n" +
                "    USER_INDEXES I\n" +
                "    LEFT JOIN USER_IND_COLUMNS IC ON I.INDEX_NAME = IC.INDEX_NAME\n" +
                "    LEFT JOIN USER_CONSTRAINTS C ON I.TABLE_NAME = C.TABLE_NAME AND I.INDEX_NAME = C.INDEX_NAME\n" +
                "WHERE \n" +
                "    I.TABLE_NAME = '{}' and I.TABLE_OWNER=SF_GET_SCHEMA_NAME_BY_ID(CURRENT_SCHID) and IC.COLUMN_NAME is not null\n" +
                "    group by IC.INDEX_NAME, C.CONSTRAINT_TYPE,C.CONSTRAINT_NAME,I.UNIQUENESS", tableName);
    }

    @Override
    public String getUpdatePkSql(String tableName, String constraintName, List<String> columnList) {
        //alter table "t_zero" modify constraint "pk_t_zero1760210543954223104" to primary key ("ddd");
        String column = CollectionUtil.join(columnList, StrPool.COMMA, this::addKeywordHandle);
        return StrUtil.format("ALTER TABLE {} MODIFY CONSTRAINT {} TO PRIMARY KEY ({})", this.addKeywordHandle(tableName),
                this.addKeywordHandle(constraintName), column);
    }

    @Override
    public String getDropPkSql(String tableName) {
        return null;
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
        //comment on table  "t_zero1" is '111'; 添加和修改是一样的
        return this.addTableCommentSql(tableName, tableComment);
    }

    @Override
    public String getUpdateColumnCommentSql(String tableName, String columnName, String columnComment) {
        //comment on column "t_zero1"."zero" is '212'; 添加和修改是一样的语句
        return this.addColumnCommentSql(tableName, columnName, columnComment);
    }

    @Override
    public String getAddColumnSql(String tableName, StringBuilder columnNameDetails) {
        //alter table "ARCHIVE_INFOR1" add column("COLUMN_1" CHAR(10));
        return StrUtil.format("ALTER TABLE {} ADD COLUMN({})",
                this.addKeywordHandle(tableName), columnNameDetails);
    }

    @Override
    public String getUpdateColumnSql(String tableName, StringBuilder columnNameDetails) {
        //alter table "TABLE_1" modify "COLUMN_2" VARCHAR(50)  DEFAULT 22  not null
        return StrUtil.format("ALTER TABLE {} MODIFY {}",
                this.addKeywordHandle(tableName), columnNameDetails);
    }

    @Override
    public String getDelColumnSql(String tableName, String columnName) {
        //alter table "TABLE_1" drop column "COLUMN_1";
        return StrUtil.format("ALTER TABLE {} DROP COLUMN {}",
                this.addKeywordHandle(tableName), this.addKeywordHandle(columnName));
    }

    @Override
    public String getUpdateColumnNameSql(String tableName, String oldColumnName, String newColumnName, String columnNameDetails) {
        //alter table "t_zero" alter column "ddd" rename to "ddd2";
        return StrUtil.format("ALTER TABLE {} ALTER COLUMN {} RENAME TO {}",
                this.addKeywordHandle(tableName), this.addKeywordHandle(oldColumnName), this.addKeywordHandle(newColumnName));
    }

    @Override
    public String getDropIndexSql(String indexName) {
        //drop index "idx_realA1759856289930014720";
        return StrUtil.format("DROP INDEX {}", this.addKeywordHandle(indexName));
    }

    @Override
    public String getDropConstraintSql(String tableName, String constraintName) {
        //alter table "t_zero" drop constraint "uk_realB1759856289930014723";
        return StrUtil.format("ALTER TABLE {} DROP CONSTRAINT {}",
                this.addKeywordHandle(tableName), this.addKeywordHandle(constraintName));
    }
}
