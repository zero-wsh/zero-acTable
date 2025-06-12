package io.gitee.zerowsh.actable.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Pair;
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
        this.checkTableInfo(tableInfo);
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
            propertySb.append(StrPool.CRLF);
            this.splicingIdx(uniqueInfo.getColumns(), propertySb, uniqueInfo.getValue(), UNIQUE_KEY);
        }
        //唯一索引
        for (TableInfo.UniqueIndexInfo uniqueIndexInfo : tableInfo.getUniqueIndexInfoList()) {
            propertySb.append(StrPool.CRLF);
            this.splicingIdx(uniqueIndexInfo.getColumns(), propertySb, uniqueIndexInfo.getValue(), UNIQUE_KEY);

        }
        //普通索引
        for (TableInfo.IndexInfo indexInfo : tableInfo.getIndexInfoList()) {
            propertySb.append(StrPool.CRLF);
            this.splicingIdx(indexInfo.getColumns(), propertySb, indexInfo.getValue(), INDEX_KEY);
        }
        resultList.add(this.addTableSql(tableName,
                propertySb.deleteCharAt(propertySb.length() - 1).toString(),
                StrUtil.format(" COMMENT='{}'", tableInfo.getComment())));
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
        propertySb.append(StrUtil.format(fillingStr, this.addKeywordHandle(value + IdUtil.getSnowflakeNextId()), column));
    }

    /**
     * 保证实体类不会存在建表冲突的情况
     * 1、一个表只能有一个自增列
     * 2、自增列必须是索引的一部分（不一定是主键，但通常是主键）
     *
     * @param tableInfo
     * @return 自增是什么索引
     */
    public void checkTableInfo(TableInfo tableInfo) {
        //自增字段集合
        List<String> autoIncrementList = new ArrayList<>();
        //不是主键的自增字段
        String notKeyAutoIncrementColumnName = null;
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            boolean autoIncrement = propertyInfo.isAutoIncrement();
            boolean key = propertyInfo.isKey();
            if (autoIncrement) {
                autoIncrementList.add(propertyInfo.getColumnName());
                if (!key) {
                    notKeyAutoIncrementColumnName = propertyInfo.getColumnName();
                }
            }
        }
        if (autoIncrementList.size() > 1) {
            throw new RuntimeException(StrUtil.format("表[{}]只能存在一个自增列，目前有[{}]", tableInfo.getName(), CollectionUtil.join(autoIncrementList, StrPool.COMMA)));
        }
        if (StrUtil.isNotBlank(notKeyAutoIncrementColumnName)) {
            List<TableInfo.IndexInfo> indexInfoList = tableInfo.getIndexInfoList();
            for (TableInfo.IndexInfo indexInfo : indexInfoList) {
                List<TableInfo.Index> columns = indexInfo.getColumns();
                for (TableInfo.Index column : columns) {
                    if (Objects.equals(column.getColumn(), notKeyAutoIncrementColumnName)) {
                        return;
                    }
                }
            }

            List<TableInfo.UniqueInfo> uniqueInfoList = tableInfo.getUniqueInfoList();
            for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
                List<TableInfo.Index> columns = uniqueInfo.getColumns();
                for (TableInfo.Index column : columns) {
                    if (Objects.equals(column.getColumn(), notKeyAutoIncrementColumnName)) {
                        return;
                    }
                }
            }

            List<TableInfo.UniqueIndexInfo> uniqueIndexInfoList = tableInfo.getUniqueIndexInfoList();
            for (TableInfo.UniqueIndexInfo uniqueIndexInfo : uniqueIndexInfoList) {
                List<TableInfo.Index> columns = uniqueIndexInfo.getColumns();
                for (TableInfo.Index column : columns) {
                    if (Objects.equals(column.getColumn(), notKeyAutoIncrementColumnName)) {
                        return;
                    }
                }
            }
            throw new RuntimeException(StrUtil.format("表[{}]自增列[{}]必须是索引的一部分", tableInfo.getName(), notKeyAutoIncrementColumnName));
        }
    }

    @Override
    public List<String> getUpdateTableSql(TableInfo tableInfo,
                                          Map<String, TableInfo.PropertyInfo> tableColumnInfoMap,
                                          List<ConstraintInfo> constraintInfoList,
                                          List<ConstraintInfo> defaultInfoList,
                                          ModelEnums modelEnums) {
        this.checkTableInfo(tableInfo);
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        //数据库表中主键
        List<ConstraintInfo> tablePkList = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(constraintInfoList)) {
            tablePkList = constraintInfoList.stream()
                    .filter(constraintInfo -> Objects.equals(constraintInfo.getConstraintFlag(), PK))
                    .collect(Collectors.toList());
        }
        //需要执行的所有sql
        List<String> resultList = new ArrayList<>();
        String tableComment = tableInfo.getComment();
        //数据库查询出来表注释
        String dbTableComment = null;
        String tableName = tableInfo.getName();
        //主键
        List<String> keyList = tableInfo.getKeyList();
        //唯一键
        List<TableInfo.UniqueInfo> uniqueInfoList = tableInfo.getUniqueInfoList();
        //唯一索引
        List<TableInfo.UniqueIndexInfo> uniqueIndexInfoList = tableInfo.getUniqueIndexInfoList();
        //普通索引
        List<TableInfo.IndexInfo> indexInfoList = tableInfo.getIndexInfoList();
        StringBuilder columnSql = new StringBuilder();
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            String columnName = propertyInfo.getColumnName();
            String oldColumnName = propertyInfo.getOldColumnName();
            TableInfo.PropertyInfo tableColumnInfo = tableColumnInfoMap.get(columnName);
            TableInfo.PropertyInfo oldTableColumnInfo = tableColumnInfoMap.get(oldColumnName);
            if (StrUtil.isBlank(dbTableComment) && Objects.nonNull(tableColumnInfo)) {
                dbTableComment = tableColumnInfo.getTableComment();
            }
            //新旧字段一起删除，代表处理过
            tableColumnInfoMap.remove(oldColumnName);
            tableColumnInfoMap.remove(columnName);
            if (Objects.isNull(tableColumnInfo) && Objects.isNull(oldTableColumnInfo)) {
                //如果columnName、oldColumnName在数据库中都没有，新增columnName
                columnSql.append(StrPool.CRLF).append(StrUtil.format("ADD COLUMN {},", this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue())));
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
                    columnSql.append(StrPool.CRLF).append(StrUtil.format("CHANGE COLUMN {} {},", this.addKeywordHandle(oldColumnName), this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue())));
                    continue;
                }
            }

            //去掉实体类defaultValue前后的单引号，因为数据库查询出来不会带单引号
            String defaultValue = this.defaultValue(propertyInfo.getDefaultValue());
            //特殊默认值处理
            if (Objects.equals(tableColumnInfo.getTypeStr(), BIT) && StrUtil.isNotBlank(defaultValue)) {
                defaultValue = StrUtil.format("b'{}'", defaultValue);
            }
            //先比较默认值
            if (!StrUtil.equals(tableColumnInfo.getDefaultValue(), defaultValue)) {
                //ALTER TABLE `test`.`t_zero` MODIFY COLUMN `test222` char(11) NULL DEFAULT b'1' AFTER `ddd`;
                columnSql.append(StrPool.CRLF).append(StrUtil.format("MODIFY COLUMN {},", this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue())));
            } else {
                String alterSentence1 = this.getAlterSentence(propertyInfo, true);
                String alterSentence2 = this.getAlterSentence(tableColumnInfo, false);
                //在比较其他值
                if (!Objects.equals(alterSentence1, alterSentence2)) {
                    columnSql.append(StrPool.CRLF).append(StrUtil.format("MODIFY COLUMN {},", this.jointDefault(alterSentence1, propertyInfo.getDefaultValue())));
                }
            }
        }


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
            if (Objects.equals(constraintInfo.getConstraintFlag(), UK)) {
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
            if (!Objects.equals(constraintInfo.getConstraintFlag(), PK)) {
                //ALTER TABLE `test`.`t_zero` DROP INDEX `uk_realD1764853286143889410`;
                columnSql.append(StrPool.CRLF).append(StrUtil.format("DROP INDEX {},", this.addKeywordHandle(constraintInfo.getConstraintName())));
            }
        }
        //主键处理
        if (Objects.nonNull(keyList)) {
            String column = CollectionUtil.join(keyList, StrPool.COMMA, this::addKeywordHandle);
            if (CollectionUtil.isNotEmpty(tablePkList)) {
                Optional<ConstraintInfo> result = constraintInfoList.stream()
                        .filter(constraintInfo -> Objects.equals(constraintInfo.getConstraintFlag(), PK))
                        .findFirst(); // 返回第一个匹配的元素（Optional）
                if (result.isPresent()) {
                    List<String> list = Arrays.asList(result.get().getConstraintColumnName().split(StrPool.COMMA));
                    if (!new HashSet<>(list).equals(new HashSet<>(keyList))) {
                        columnSql.append(StrPool.CRLF).append(StrUtil.format("DROP PRIMARY KEY, ADD PRIMARY KEY ({}),", column));
                    }
                }
            } else {
                columnSql.append(StrPool.CRLF).append(StrUtil.format("ADD PRIMARY KEY ({}),", column));
            }
        } else {
            if (CollectionUtil.isNotEmpty(tablePkList)) {
                columnSql.append(StrPool.CRLF).append("DROP PRIMARY KEY,");
            }
        }
        //唯一约束
        for (TableInfo.UniqueInfo uniqueInfo : tableInfo.getUniqueInfoList()) {
            columnSql.append(StrPool.CRLF).append("ADD");
            this.splicingIdx(uniqueInfo.getColumns(), columnSql, uniqueInfo.getValue(), UNIQUE_KEY);
        }
        //唯一索引
        for (TableInfo.UniqueIndexInfo uniqueIndexInfo : tableInfo.getUniqueIndexInfoList()) {
            columnSql.append(StrPool.CRLF).append("ADD");
            this.splicingIdx(uniqueIndexInfo.getColumns(), columnSql, uniqueIndexInfo.getValue(), UNIQUE_KEY);

        }
        //普通索引
        for (TableInfo.IndexInfo indexInfo : tableInfo.getIndexInfoList()) {
            columnSql.append(StrPool.CRLF).append("ADD");
            this.splicingIdx(indexInfo.getColumns(), columnSql, indexInfo.getValue(), INDEX_KEY);
        }

        if (Objects.equals(modelEnums, ModelEnums.ADD_OR_UPDATE_OR_DEL)) {
            //如果数据库有但是实体类没有，进行删除
            if (CollectionUtil.isNotEmpty(tableColumnInfoMap)) {
                for (Map.Entry<String, TableInfo.PropertyInfo> map : tableColumnInfoMap.entrySet()) {
                    TableInfo.PropertyInfo value = map.getValue();
                    //ALTER TABLE `test`.`t_zero` DROP COLUMN `zero`;
                    columnSql.append(StrPool.CRLF).append(StrUtil.format("DROP COLUMN {},", this.addKeywordHandle(value.getColumnName())));
                }
            }
        }

        //处理表备注
        if (!Objects.equals(tableComment, dbTableComment)) {
            //ALTER TABLE `test`.`t_zero` COMMENT = '测试11';
            columnSql.append(StrPool.CRLF).append(StrUtil.format(" COMMENT = '{}',", tableComment));
        }
        if (columnSql.length() > 0) {
            String alterTable = StrUtil.format("ALTER TABLE {}", this.addKeywordHandle(tableName));
            columnSql.insert(0, alterTable);
            resultList.add(columnSql.deleteCharAt(columnSql.length() - 1).toString());
        }
        return resultList;
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


    /**
     * 获取alter语句
     *
     * @param propertyInfo 字段信息
     * @param flag         是否实体类映射
     * @return
     */
    public String getAlterSentence(TableInfo.PropertyInfo propertyInfo, boolean flag) {
        String typeStr = propertyInfo.getTypeStr().toLowerCase();
        StringBuilder sb = new StringBuilder(this.addKeywordHandle(propertyInfo.getColumnName()));
        sb.append(" ").append(typeStr);
        long length = propertyInfo.getLength();
        long decimalLength = propertyInfo.getDecimalLength();


        //处理length和decimalLength
        if (!this.ignoreLengthAndDecimalLength().contains(typeStr)) {
            if (flag) {
                boolean typeLimit = propertyInfo.isTypeLimit();
                if (typeLimit) {
                    //类型限制，在已有的集合中找
                    Pair<String, Long> pair = mysqlContains(typeStr);
                    if (Objects.nonNull(pair.getValue())) {
                        //没有定义返回的是默认的
                        typeStr = pair.getKey();
                        length = pair.getValue();
                    }
                }
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
         * 自增、主键 必填
         * 如果isNull是false 必填
         */
        if (propertyInfo.isAutoIncrement() || propertyInfo.isKey() || !propertyInfo.isNull()) {
            sb.append(NOT_NULL);
            if (propertyInfo.isAutoIncrement()) {
                //加上自增的逻辑
                sb.append(MYSQL_IDENTITY);
            }
        } else {
            sb.append(NULL);
        }
        //默认值占位符
        sb.append("{}");
        String columnComment = propertyInfo.getColumnComment();
        if (Objects.nonNull(columnComment)) {
            sb.append(StrUtil.format(" COMMENT '{}'", columnComment));
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
