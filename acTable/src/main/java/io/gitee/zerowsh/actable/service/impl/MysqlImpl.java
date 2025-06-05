package io.gitee.zerowsh.actable.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import io.gitee.zerowsh.actable.constant.AcTableConstants;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.JavaTypeTurnColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.properties.AcTableProperties;
import io.gitee.zerowsh.actable.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.*;
import static io.gitee.zerowsh.actable.constant.ColumnTypeConstants.*;

/**
 * mysql数据库实现
 *
 * @author zero
 */
@Slf4j
public class MysqlImpl extends DatabaseService {
    public MysqlImpl() {
        super("`", "`");
    }


    @Override
    public boolean autoincrementIsPk() {
        return true;
    }


    @Override
    public Set<String> ignoreLengthAndDecimalLength() {
        AcTableProperties acTableProperties = SpringUtil.getBean(AcTableProperties.class);
        HashSet<String> resultSet = new HashSet<String>() {{
            add(BIGINT);
            add(INT);
            add(LONGTEXT);
            add(TEXT);
            add(DATE);
            add(JSON);
            add(TINYINT);
            add(SMALLINT);
        }};
        String ignoreLength = acTableProperties.getIgnoreLengthAndDecimalLength();
        if (StrUtil.isNotBlank(ignoreLength)) {
            resultSet.addAll(StrUtil.split(ignoreLength, StrPool.COMMA));
        }
        return resultSet;
    }

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
            propertySb.append(StrPool.CRLF).append(this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue()))
                    .append(StrPool.COMMA);
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
        resultList.add(this.addTableSql(tableName,
                propertySb.deleteCharAt(propertySb.length() - 1).toString(),
                StrUtil.format(COMMENT_EQ, tableInfo.getComment())));
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
                                          Map<String, TableInfo.PropertyInfo> tableColumnInfoMap,
                                          List<ConstraintInfo> constraintInfoList,
                                          List<ConstraintInfo> defaultInfoList,
                                          ModelEnums modelEnums) {
        //数据库表中主键
        List<ConstraintInfo> tablePkList = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(constraintInfoList)) {
            tablePkList = constraintInfoList.stream()
                    .filter(constraintInfo -> Objects.equals(constraintInfo.getConstraintFlag(), PK))
                    .collect(Collectors.toList());
        }
        List<String> resultList = new ArrayList<>();
        String addPrimary = null;
        List<String> columnList = new ArrayList<>();
        String tableComment = tableInfo.getComment();
        //数据库查询出来表注释
        String tableComment1 = null;
        String tableName = tableInfo.getName();
        //主键
        List<String> keyList = tableInfo.getKeyList();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            String columnName = propertyInfo.getColumnName();
            String oldColumnName = propertyInfo.getOldColumnName();
            TableInfo.PropertyInfo tableColumnInfo = tableColumnInfoMap.get(columnName);
            TableInfo.PropertyInfo oldTableColumnInfo = tableColumnInfoMap.get(oldColumnName);
            if (StrUtil.isBlank(tableComment1)) {
                tableComment1 = propertyInfo.getTableComment();
            }

            //新旧字段一起删除，代表处理过
            tableColumnInfoMap.remove(oldColumnName);
            tableColumnInfoMap.remove(columnName);
            if (Objects.isNull(tableColumnInfo) && Objects.isNull(oldTableColumnInfo)) {
                //如果columnName、oldColumnName在数据库中都没有，新增columnName
                StringBuilder propertySb = new StringBuilder(this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue()));
                propertySb.append(StrPool.COMMA);
                //mysql如果是自增字段就必须是主键，并且添加主键的时候必须在第一个
                if (propertyInfo.isAutoIncrement()) {
                    if (CollectionUtil.isEmpty(keyList)) {
                        keyList = Collections.singletonList(propertyInfo.getColumnName());
                    } else {
                        keyList.remove(columnName);
                        keyList.add(0, columnName);
                    }
                    //判断以前表中是否有主键，如果有先删除，在添加。注意这里必须写成一条语句
                    if (CollectionUtil.isNotEmpty(tablePkList)) {
                        //需要判断删除的主键中是否有自增字段，如果有需要先删除字段的自增
                        for (ConstraintInfo constraintInfo : tablePkList) {
                            this.removeAutoIncrement(tableColumnInfoMap, constraintInfo.getConstraintColumnName(), resultList, tableName);
                        }
                        //这里的多字段可能不在表中
                        propertySb.append(StrUtil.format(" DROP PRIMARY KEY, ADD PRIMARY KEY ({}),",
                                CollectionUtil.join(keyList, StrPool.COMMA, this::addKeywordHandle)));
                    } else {
                        propertySb.append(StrUtil.format(" ADD PRIMARY KEY ({}),",
                                CollectionUtil.join(keyList, StrPool.COMMA, this::addKeywordHandle)));
                    }
                    //代表处理过主键
                    keyList = null;
                    addPrimary = this.getAddColumnSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1));
                } else {
                    resultList.add(this.getAddColumnSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1)));
                }
                continue;
            }

            if (!Objects.equals(columnName, oldColumnName)) {
                if (Objects.nonNull(tableColumnInfo) && Objects.nonNull(oldTableColumnInfo)) {
                    //如果columnName、oldColumnName在数据库中都有，存在问题
                    throw new RuntimeException(StrUtil.format("无法将表【{}】，字段【{}】修改成【{}】，两个字段都在表中存在！",
                            tableName, oldColumnName, columnName));
                }
                if (Objects.nonNull(oldTableColumnInfo)) {
                    //将旧的字段修改成新字段
                    resultList.add(this.getUpdateColumnNameSql(tableName, oldColumnName, columnName, this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue())));
                    continue;
                }
            }

            //去掉defaultValue前后的单引号，因为数据库查询出来不会带单引号
            String defaultValue = this.defaultValue(propertyInfo.getDefaultValue());
            //判断是否存在字段修改
            if (!StrUtil.equals(tableColumnInfo.getDefaultValue(), defaultValue)) {
                columnList.add(this.getUpdateColumnSql(tableName, this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue())));
            } else {
                String alterSentence1 = this.getAlterSentence(propertyInfo, true);
                String alterSentence2 = this.getAlterSentence(tableColumnInfo, false);
                if (!Objects.equals(alterSentence1, alterSentence2)) {
                    columnList.add(this.getUpdateColumnSql(tableName, this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue())));
                }
            }
        }
        //处理表备注
        if (!Objects.equals(tableComment, tableComment1)) {
            columnList.add(this.getUpdateTableCommentSql(tableName, tableComment));
        }
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
                //判断是否在前面处理过
                if (Objects.nonNull(keyList)) {
                    //判断是否完全相等
                    // 一个表只会查出来一个主键名称，一个主键名称对应多个字段
                    if (!new HashSet<>(list).equals(new HashSet<>(keyList))) {
                        this.removeAutoIncrement(tableColumnInfoMap, constraintInfo.getConstraintColumnName(), resultList, tableName);
                        //修改
                        resultList.add(this.getUpdatePkSql(tableName, constraintInfo.getConstraintName(), keyList, CollectionUtil.isNotEmpty(tablePkList)));
                    }
                }
                //代表处理过主键
                keyList = null;
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
        if (StrUtil.isNotBlank(addPrimary)) {
            resultList.add(addPrimary);
        }

        if (Objects.nonNull(keyList)) {
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
                for (Map.Entry<String, TableInfo.PropertyInfo> map : tableColumnInfoMap.entrySet()) {
                    TableInfo.PropertyInfo value = map.getValue();
                    resultList.add(this.getDelColumnSql(value.getTableName(), value.getColumnName()));
                }
            }
        }
        return resultList;
    }

    public void removeAutoIncrement(Map<String, TableInfo.PropertyInfo> tableColumnInfoMap,
                                    String columnName,
                                    List<String> resultList,
                                    String tableName) {
        TableInfo.PropertyInfo tablePkColumnInfo = tableColumnInfoMap.get(columnName);
        if (Objects.nonNull(tablePkColumnInfo) && tablePkColumnInfo.isAutoIncrement()) {
            TableInfo.PropertyInfo propertyPkInfo = new TableInfo.PropertyInfo();
            BeanUtil.copyProperties(tablePkColumnInfo, propertyPkInfo);
            //存在修改
            propertyPkInfo.setKey(false);
            propertyPkInfo.setAutoIncrement(false);
            propertyPkInfo.setTypeStr(tablePkColumnInfo.getTypeStr());
            resultList.add(this.getUpdateColumnSql(tableName, this.jointDefault(this.getAlterSentence(propertyPkInfo, true), propertyPkInfo.getDefaultValue())));
        }
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
    public String javaTypeTurnColumnType(String fieldType, String columnType) {
        return Objects.equals(columnType, ColumnTypeConstants.DEFAULT_VALUE)
                ? JavaTypeTurnColumnTypeEnums.getMysqlByValue(fieldType) : columnType;
    }

    @Override
    public String getAllTableSql() {
        return "select table_name as name from information_schema.tables   " +
                " where table_schema = (select database())";
    }

    @Override
    public String existTableSql(String tableName) {
        return StrUtil.format("select count(1) from information_schema.tables   " +
                " where table_name ='{}' and table_schema = (select database())", tableName);
    }

    @Override
    public String getTableStructureSql(String tableName) {
        return StrUtil.format("SELECT t.table_name tableName,  " +
                "t.table_comment tableComment,  " +
                "case when c.IS_NULLABLE='YES' then 1 else 0 end isNull,  " +
                "c.column_name columnName,  " +
                "c.column_comment columnComment,  " +
                "c.DATA_TYPE typeStr,  " +
                "c.COLUMN_DEFAULT defaultValue,  " +
                "ifnull(case when c.NUMERIC_PRECISION !='' and  c.NUMERIC_PRECISION is not null then c.NUMERIC_PRECISION else  c.CHARACTER_MAXIMUM_LENGTH end,-1) length,  " +
                "ifnull(case when c.NUMERIC_SCALE!='' and c.NUMERIC_SCALE is not null then c.NUMERIC_SCALE else c.DATETIME_PRECISION end,-1) decimalLength,  " +
                "case when c.column_key='PRI' then 1 else 0 end isKey,case when c.EXTRA='auto_increment' then 1 else 0 end isAutoIncrement   " +
                "FROM information_schema.columns c,information_schema.tables t   " +
                "WHERE c.table_name = t.table_name and c.table_name='{}' and c.table_schema = (select database()) AND t.table_schema = (SELECT DATABASE ())", tableName);
    }


    @Override
    public String getConstraintInfoSql(String tableName) {
        return StrUtil.format("select index_name constraintName ,  " +
                "GROUP_CONCAT(column_name order by column_name) constraintColumnName,   " +
                "GROUP_CONCAT(case WHEN collation='D' then 'DESC' else 'ASC' end order by collation) indexSortStr,   " +
                "case when non_unique=0 then case when index_name='PRIMARY' then 1 else 2 end else 3 end constraintFlag   " +
                "from information_schema.statistics where table_name = '{}' and table_schema = (select database())   " +
                "GROUP BY constraintName,constraintFlag", tableName);
    }


    @Override
    public String getUpdatePkSql(String tableName, String constraintName, List<String> columnList, boolean tableExistPk) {
        //ALTER TABLE `test`.`t_zero`  DROP PRIMARY KEY, ADD PRIMARY KEY (`id`, `cc`)
        String column = CollectionUtil.join(columnList, StrPool.COMMA, this::addKeywordHandle);
        if (tableExistPk) {
            return StrUtil.format("ALTER TABLE {} DROP PRIMARY KEY, ADD PRIMARY KEY ({})",
                    this.addKeywordHandle(tableName), column);
        } else {
            return StrUtil.format("ALTER TABLE {} ADD PRIMARY KEY ({})",
                    this.addKeywordHandle(tableName), column);
        }
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
    public String getUpdateColumnSql(String tableName, String columnNameDetails) {
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
     * 获取alter语句
     *
     * @param propertyInfo 字段信息
     * @param flag         是否实体类映射
     * @return
     */
    public String getAlterSentence(TableInfo.PropertyInfo propertyInfo, boolean flag) {
        String typeStr = propertyInfo.getTypeStr();
        StringBuilder sb = new StringBuilder(this.addKeywordHandle(propertyInfo.getColumnName()));
        sb.append(" ").append(typeStr.toLowerCase());
        long length = propertyInfo.getLength();
        long decimalLength = propertyInfo.getDecimalLength();


        //处理length和decimalLength
        if (!this.ignoreLengthAndDecimalLength().contains(typeStr)) {
            if (flag) {
                //直接通过实体类映射或者注解指定的，需要先处理length和decimalLength
                String columnName = propertyInfo.getColumnName();
                String tableName = propertyInfo.getTableName();
                switch (typeStr) {
                    case ColumnTypeConstants.DATETIME:
                        if (length > 6 || length < 0) {
                            log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, typeStr, decimalLength, 0);
                            length = 0;
                        }
                        break;
                    case ColumnTypeConstants.VARCHAR:
                    case ColumnTypeConstants.CHAR:
                        if (length < 0) {
                            log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, typeStr, length, 255);
                            length = 255;
                        }
                        break;
                    case DECIMAL:
                    case NUMERIC:
                    case DOUBLE:
                    case FLOAT:
                        if (length > 65 || length < 0) {
                            log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, typeStr, length, 10);
                            length = 10;
                        }
                        if (decimalLength > 65 || decimalLength > length || decimalLength < 0) {
                            log.warn(COLUMN_DECIMAL_LENGTH_VALID_STR, tableName, columnName, typeStr, decimalLength, length, 2);
                            decimalLength = 2;
                        }
                        break;
                    case BIT:
                        if (length > 64 || length < 0) {
                            log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, typeStr, length, 1);
                            length = 1;
                        }
                        break;
                }
            }

            if (!Objects.equals(length, AcTableConstants.NUMBER_UNDEFINED)
                    && !Objects.equals(decimalLength, AcTableConstants.NUMBER_UNDEFINED)) {
                //length和decimalLength都有值
                sb.append(StrUtil.format(LENGTH_DECIMAL, length, decimalLength));
            } else if (!Objects.equals(length, AcTableConstants.NUMBER_UNDEFINED)) {
                //length有值
                sb.append(StrUtil.format(LENGTH, length));
            }
        }
        /*
         * 如果是自增可能不是必填
         * 如果是主键肯定是必填
         * 如果isNull是false必填
         */
        if (propertyInfo.isAutoIncrement()) {
            if (this.autoincrementIsPk()) {
                sb.append(NOT_NULL);
            } else {
                if (propertyInfo.isKey()) {
                    sb.append(NOT_NULL);
                } else {
                    if (propertyInfo.isNull()) {
                        sb.append(NULL);
                    } else {
                        sb.append(NOT_NULL);
                    }
                }
            }
            //加上自增的逻辑
            sb.append(MYSQL_IDENTITY);
        } else {
            if (propertyInfo.isKey()) {
                sb.append(NOT_NULL);
            } else {
                if (propertyInfo.isNull()) {
                    sb.append(NULL);
                } else {
                    sb.append(NOT_NULL);
                }
            }
        }
        //默认值占位符
        sb.append("{}");
        String columnComment = propertyInfo.getColumnComment();
        if (Objects.nonNull(columnComment)) {
            sb.append(StrUtil.format(COMMENT, columnComment));
        }
        return sb.toString();
    }

    public String jointDefault(String alterSentence, String defaultValue) {
        if (Objects.nonNull(defaultValue)) {
            if (defaultValue.isEmpty()) {//空字符串
                return StrUtil.format(alterSentence, StrUtil.format(DEFAULT, "''"));
            } else {
                return StrUtil.format(alterSentence, StrUtil.format(DEFAULT, defaultValue));
            }
        }
        return StrUtil.format(alterSentence, "");
    }
}
