package io.gitee.zerowsh.actable.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
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
import static io.gitee.zerowsh.actable.constant.ColumnTypeConstants.*;
import static io.gitee.zerowsh.actable.constant.StringConstants.LEFT_BRACKET;
import static io.gitee.zerowsh.actable.constant.StringConstants.RIGHT_BRACKET;

/**
 * mysql数据库实现
 *
 * @author zero
 */
@Slf4j
public class MysqlImpl implements DatabaseService {


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
        StringBuilder propertySb = new StringBuilder();
        for (TableInfo.PropertyInfo propertyInfo : tableInfo.getPropertyInfoList()) {
            this.splicingColumnInfo(propertySb, propertyInfo, tableName);
        }
        //主键
        if (CollectionUtil.isNotEmpty(tableInfo.getKeyList())) {
            propertySb.append(StrPool.CRLF)
                    .append(StrUtil.format(PRIMARY_KEY, CollectionUtil.join(tableInfo.getKeyList(), StrPool.COMMA, this::addKeywordHandle)));
        }
        //唯一约束
        for (TableInfo.UniqueInfo uniqueInfo : tableInfo.getUniqueInfoList()) {
            this.splicingIdx(uniqueInfo.getColumns(), propertySb, uniqueInfo.getValue(), UNIQUE_KEY);
        }
        //唯一索引
        for (TableInfo.UniqueIndexInfo uniqueIndexInfo : tableInfo.getUniqueIndexInfoList()) {
            this.splicingIdx(uniqueIndexInfo.getColumns(), propertySb, uniqueIndexInfo.getValue(), UNIQUE_KEY);

        }
        //普通索引
        for (TableInfo.IndexInfo indexInfo : tableInfo.getIndexInfoList()) {
            this.splicingIdx(indexInfo.getColumns(), propertySb, indexInfo.getValue(), INDEX_KEY);
        }
        resultList.add(this.addTableSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1).toString(), tableInfo.getComment()));
        return resultList;
    }

    /**
     * 拼接索引（唯一约束、唯一索引、普通索引）
     *
     * @param columns
     * @param propertySb
     * @param value
     * @param fillingStr 填充字符串
     */
    public void splicingIdx(List<TableInfo.Index> columns, StringBuilder propertySb, String value, String fillingStr) {
        String column = CollectionUtil.join(columns, StrPool.COMMA, (index) ->
                this.addKeywordHandle(index.getColumn()) + (index.isAsc() ? ASC : DESC));
        propertySb.append(StrPool.CRLF)
                .append(StrUtil.format(fillingStr, this.addKeywordHandle(value + IdUtil.getSnowflakeNextId()), column));
    }

    @Override
    public List<String> getUpdateTableSql(TableInfo tableInfo,
                                          Map<String, TableColumnInfo> tableColumnInfoMap,
                                          List<ConstraintInfo> constraintInfoList,
                                          List<ConstraintInfo> defaultInfoList,
                                          ModelEnums modelEnums) {
        List<String> resultList = new ArrayList<>();
        List<String> columnList = new ArrayList<>();
        String comment = tableInfo.getComment();
        String tableName = tableInfo.getName();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        int count = 0;
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            String columnName = propertyInfo.getColumnName();
            String oldColumnName = propertyInfo.getOldColumnName();
            TableColumnInfo tableColumnInfo = tableColumnInfoMap.get(columnName);
            TableColumnInfo oldTableColumnInfo = tableColumnInfoMap.get(oldColumnName);
            //新旧字段一起删除，代表处理过
            tableColumnInfoMap.remove(oldColumnName);
            tableColumnInfoMap.remove(columnName);
            if (!Objects.equals(columnName, oldColumnName)) {
                if (Objects.nonNull(tableColumnInfo) && Objects.nonNull(oldTableColumnInfo)) {
                    //如果columnName、oldColumnName在数据库中都有，存在问题
                    throw new RuntimeException(StrUtil.format("无法将表【{}】，字段【{}】修改成【{}】，两个字段都在表中存在！",
                            tableName, oldColumnName, columnName));
                }
                if (Objects.nonNull(oldTableColumnInfo)) {
                    //修改
                    StringBuilder propertySb = new StringBuilder();
                    this.splicingColumnInfo(propertySb, propertyInfo, tableName);
                    resultList.add(this.getUpdateColumnNameSql(tableName, oldColumnName, columnName,
                            propertySb.deleteCharAt(propertySb.length() - 1).toString()));
                    continue;
                }
                //写错了会导致新增字段和删除字段
                throw new RuntimeException(StrUtil.format("无法将表【{}】，字段【{}】修改成【{}】，两个字段都不存在于表中！",
                        tableName, oldColumnName, columnName));
            }
            if (Objects.isNull(tableColumnInfo) && Objects.isNull(oldTableColumnInfo)) {
                //如果columnName、oldColumnName在数据库中都没有，新增columnName
                StringBuilder propertySb = new StringBuilder();
                this.splicingColumnInfo(propertySb, propertyInfo, tableName);
                //mysql如果是自增字段就必须是主键
                if (propertyInfo.isAutoIncrement()) {
                    //先删除，在添加
                    propertySb.append(StrUtil.format(" DROP PRIMARY KEY, ADD PRIMARY KEY ({}),", this.addKeywordHandle(propertyInfo.getColumnName())));
                } else {
                    //非自增主键
                    if (propertyInfo.isKey()) {
                        //删除主键
                        propertySb.append(StrUtil.format(" DROP PRIMARY KEY,"));
                    }
                }
                resultList.add(this.getAddColumnSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1)));
                continue;
            }
            if (count == 0) {
                //处理表备注
                if (!Objects.equals(comment, tableColumnInfo.getTableComment())) {
                    columnList.add(this.getUpdateTableCommentSql(tableName, comment));
                }
            }
            count++;

            //修改--判断类型、是否为空、是否自增、默认值、字段备注,这些是否存在修改
            boolean existUpdate = (propertyInfo.isTypeLimit() && !StrUtil.equalsIgnoreCase(tableColumnInfo.getTypeStr(), propertyInfo.getType()))
                    || !(tableColumnInfo.isNull() == (!propertyInfo.isKey() && !propertyInfo.isAutoIncrement()
                    && propertyInfo.isNull()))
                    || tableColumnInfo.isAutoIncrement() != propertyInfo.isAutoIncrement()
                    || !StrUtil.equalsIgnoreCase(tableColumnInfo.getColumnComment(), propertyInfo.getColumnComment());
            if (!existUpdate) {
                if (Objects.equals(tableColumnInfo.getTypeStr(), BIT)) {
                    existUpdate = !StrUtil.equalsIgnoreCase(tableColumnInfo.getDefaultValue(), StrUtil.format("b'{}'", propertyInfo.getDefaultValue()));
                } else {
                    existUpdate = !StrUtil.equalsIgnoreCase(tableColumnInfo.getDefaultValue(), propertyInfo.getDefaultValue());
                }
            }
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
                //mysql如果是自增字段就必须是主键
                if (propertyInfo.isAutoIncrement()) {
                    //先删除，在添加
                    propertySb.append(StrUtil.format(" DROP PRIMARY KEY, ADD PRIMARY KEY ({}),", this.addKeywordHandle(propertyInfo.getColumnName())));
                } else {
                    //非自增主键
                    if (propertyInfo.isKey()) {
                        //删除主键
                        propertySb.append(StrUtil.format(" DROP PRIMARY KEY,"));
                    }
                }
                columnList.add(this.getUpdateColumnSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1)));
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
                //唯一索引
                Iterator<TableInfo.UniqueIndexInfo> uniqueIndexInfoIterator = uniqueIndexInfoList.iterator();
                while (uniqueIndexInfoIterator.hasNext()) {
                    if (this.handleIndex(uniqueIndexInfoIterator.next().getColumns(), list, sortList)) {
                        uniqueIndexInfoIterator.remove();
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
            }
        }
        //删除表中多余的
        for (ConstraintInfo constraintInfo : constraintInfoList) {
            resultList.add(this.getDropConstraintSql(tableName, constraintInfo.getConstraintName()));
        }
        resultList.addAll(columnList);

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

    /**
     * 创建主键
     *
     * @param keyList
     * @param tableName
     * @param resultList
     */
    private void createPk(List<String> keyList, String tableName, List<String> resultList) {
        if (CollectionUtil.isNotEmpty(keyList)) {
            resultList.add(this.addPrimaryKeySql(tableName, null, keyList));
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

    @Override
    public boolean autoincrementIsPk() {
        return true;
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
                ? JavaTypeTurnColumnTypeEnums.getMysqlByValue(fieldType) : columnType;
    }

    @Override
    public String leftKeyword() {
        return "`";
    }

    @Override
    public String rightKeyword() {
        return this.leftKeyword();
    }

    @Override
    public String getAllTableSql() {
        return "select table_name as name from information_schema.tables \n" +
                " where table_schema = (select database())";
    }

    @Override
    public String existTableSql(String tableName) {
        return StrUtil.format("select count(1) from information_schema.tables \n" +
                " where table_name ='{}' and table_schema = (select database())", tableName);
    }

    @Override
    public String getTableStructureSql(String tableName) {
        return StrUtil.format("SELECT t.table_name tableName,\n" +
                "t.table_comment tableComment,\n" +
                "case when c.IS_NULLABLE='YES' then 1 else 0 end isNull,\n" +
                "c.column_name columnName,\n" +
                "c.column_comment columnComment,\n" +
                "c.DATA_TYPE typeStr,\n" +
                "c.COLUMN_DEFAULT defaultValue,\n" +
                "case when c.NUMERIC_PRECISION !='' and  c.NUMERIC_PRECISION is not null then c.NUMERIC_PRECISION else  c.CHARACTER_MAXIMUM_LENGTH end length,\n" +
                "case when c.NUMERIC_SCALE!='' and c.NUMERIC_SCALE is not null then c.NUMERIC_SCALE else c.DATETIME_PRECISION end decimalLength,\n" +
                "case when c.column_key='PRI' then 1 else 0 end isKey,case when c.EXTRA='auto_increment' then 1 else 0 end isAutoIncrement \n" +
                "FROM information_schema.columns c,information_schema.tables t \n" +
                "WHERE c.table_name = t.table_name and c.table_name='{}' and c.table_schema = (select database()) AND t.table_schema = (SELECT DATABASE ())", tableName);
    }


    @Override
    public String getConstraintInfoSql(String tableName) {
        return StrUtil.format("select index_name constraintName ,\n" +
                "GROUP_CONCAT(column_name order by column_name) constraintColumnName, \n" +
                "GROUP_CONCAT(case WHEN collation='D' then 'DESC' else 'ASC' end order by collation) indexSortStr, \n" +
                "case when non_unique=0 then case when index_name='PRIMARY' then 1 else 2 end else 3 end constraintFlag \n" +
                "from information_schema.statistics where table_name = '{}' and table_schema = (select database()) \n" +
                "GROUP BY constraintName,constraintFlag", tableName);
    }

    @Override
    public String addTableSql(String tableName, String columnInfo, String tableComment) {
        return StrUtil.format("CREATE TABLE {} ({}) COMMENT='{}'", this.addKeywordHandle(tableName), columnInfo, tableComment);
    }

    @Override
    public String getUpdatePkSql(String tableName, String constraintName, List<String> columnList) {
        //ALTER TABLE `test`.`t_zero`  DROP PRIMARY KEY, ADD PRIMARY KEY (`id`, `cc`)
        String column = CollectionUtil.join(columnList, StrPool.COMMA, this::addKeywordHandle);
        return StrUtil.format("ALTER TABLE {} DROP PRIMARY KEY, ADD PRIMARY KEY ({})",
                this.addKeywordHandle(tableName), column);
    }

    @Override
    public String getDropPkSql(String tableName) {
        //ALTER TABLE `test`.`t_zero`  DROP PRIMARY KEY
        return StrUtil.format("ALTER TABLE {} DROP PRIMARY KEY",
                this.addKeywordHandle(tableName));
    }

    @Override
    public String getDefaultInfoSql(String tableName) {
        return null;
    }

    @Override
    public String addTableCommentSql(String tableName, String comment) {
        return null;
    }

    @Override
    public String addColumnCommentSql(String tableName, String columnName, String comment) {
        return null;
    }

    @Override
    public String addPrimaryKeySql(String tableName, String constraintName, List<String> columnList) {
        //ALTER TABLE `test`.`t_zero` ADD PRIMARY KEY (`cc`);
        String column = CollectionUtil.join(columnList, StrPool.COMMA, this::addKeywordHandle);
        return StrUtil.format("ALTER TABLE {} ADD PRIMARY KEY ({})",
                this.addKeywordHandle(tableName), column);
    }

    @Override
    public String addIndexSql(String tableName, String indexName, List<TableInfo.Index> columns) {
        //ALTER TABLE `test`.`t_zero`  ADD INDEX `dfdfd`(`ddd`);
        String column = CollectionUtil.join(columns, StrPool.COMMA, (index) ->
                this.addKeywordHandle(index.getColumn()) + (index.isAsc() ? ASC : DESC));
        return StrUtil.format("ALTER TABLE {} ADD INDEX {}({})",
                this.addKeywordHandle(tableName), this.addKeywordHandle(indexName), column);
    }

    @Override
    public String addUniqueIndexSql(String tableName, String constraintName, List<TableInfo.Index> columns) {
        //ALTER TABLE `test`.`t_zero`  ADD UNIQUE INDEX `dfdfd`(`ddd`);
        String column = CollectionUtil.join(columns, StrPool.COMMA, (index) ->
                this.addKeywordHandle(index.getColumn()) + (index.isAsc() ? ASC : DESC));
        return StrUtil.format("ALTER TABLE {} ADD UNIQUE INDEX {}({})",
                this.addKeywordHandle(tableName), this.addKeywordHandle(constraintName), column);
    }

    @Override
    public String addUniqueSql(String tableName, String constraintName, List<TableInfo.Index> columns) {
        //ALTER TABLE `test`.`t_zero` ADD UNIQUE INDEX `dfdfd`(`ddd`);
        String column = CollectionUtil.join(columns, StrPool.COMMA, (index) -> this.addKeywordHandle(index.getColumn()));
        return StrUtil.format("ALTER TABLE {} ADD UNIQUE INDEX {}({})",
                this.addKeywordHandle(tableName), this.addKeywordHandle(constraintName), column);
    }

    @Override
    public String getUpdateTableCommentSql(String tableName, String tableComment) {
        //ALTER TABLE `test`.`t_zero` COMMENT = '测试11';
        return StrUtil.format("ALTER TABLE {} COMMENT = '{}'", this.addKeywordHandle(tableName), tableComment);
    }

    @Override
    public String getUpdateColumnCommentSql(String tableName, String columnName, String columnComment) {
        return null;
    }

    @Override
    public String getAddColumnSql(String tableName, StringBuilder columnNameDetails) {
        //ALTER TABLE `test`.`t_zero` ADD COLUMN `ddd` varchar(255) NULL COMMENT 'dfadf';
        return StrUtil.format("ALTER TABLE {} ADD COLUMN {}", this.addKeywordHandle(tableName), columnNameDetails);
    }

    @Override
    public String getUpdateColumnSql(String tableName, StringBuilder columnNameDetails) {
        //这里不会存在改列名字
        //ALTER TABLE `test`.`t_zero` MODIFY COLUMN `test222` char(11) NULL DEFAULT b'1' AFTER `ddd`;
        return StrUtil.format("ALTER TABLE {} MODIFY COLUMN {}",
                this.addKeywordHandle(tableName), columnNameDetails);
    }

    @Override
    public String getDelColumnSql(String tableName, String columnName) {
        //ALTER TABLE `test`.`t_zero` DROP COLUMN `zero`;
        return StrUtil.format("ALTER TABLE {} DROP COLUMN {}",
                this.addKeywordHandle(tableName), this.addKeywordHandle(columnName));
    }

    @Override
    public String getUpdateColumnNameSql(String tableName, String oldColumnName, String newColumnName, String columnNameDetails) {
        //ALTER TABLE `test`.`t_zero` CHANGE COLUMN `zero` `zero1` int NOT NULL
        return StrUtil.format("ALTER TABLE {} CHANGE COLUMN {} {}",
                this.addKeywordHandle(tableName), this.addKeywordHandle(oldColumnName), columnNameDetails);
    }

    @Override
    public String getDropIndexSql(String indexName) {
        return null;
    }

    @Override
    public String getDropConstraintSql(String tableName, String constraintName) {
        //ALTER TABLE `test`.`t_zero` DROP INDEX `uk_realD1764853286143889410`;
        return StrUtil.format("ALTER TABLE {} DROP INDEX {}",
                this.addKeywordHandle(tableName), this.addKeywordHandle(constraintName));
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
            if (typeLimit && !ColumnTypeConstants.mysqlContains(type)) {
                //类型限制并且没有定义这个类型使用默认字符串
                propertySb.append(ColumnTypeConstants.VARCHAR).append(LEFT_BRACKET).append(255).append(RIGHT_BRACKET);
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
        //是否自增，自增不能设置默认值
        if (propertyInfo.isAutoIncrement()) {
            propertySb.append(MYSQL_IDENTITY);
        } else {
            if (Objects.nonNull(propertyInfo.getDefaultValue())) {
                propertySb.append(StrUtil.format(DEFAULT, propertyInfo.getDefaultValue()));
            }
        }

        //列备注
        if (StrUtil.isNotBlank(propertyInfo.getColumnComment())) {
            propertySb.append(StrUtil.format(COMMENT, propertyInfo.getColumnComment()));
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
                if (length > 6 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 0);
                    length = 0;
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
            case DECIMAL:
            case NUMERIC:
            case DOUBLE:
            case FLOAT:
                if (length > 65 || length < 0) {
                    log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, type, length, 10);
                    length = 10;
                }
                if (decimalLength > 65 || decimalLength > length || decimalLength < 0) {
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
}
