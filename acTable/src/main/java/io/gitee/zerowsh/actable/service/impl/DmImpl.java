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
 * 达梦数据库实现
 *
 * @author zero
 */
@Slf4j
public class DmImpl extends DatabaseService {
    public DmImpl() {
        super("\"", "\"");
    }

    @Override
    public Set<String> ignoreLengthAndDecimalLength() {
        AcTableProperties acTableProperties = SpringUtil.getBean(AcTableProperties.class);
        HashSet<String> resultSet = new HashSet<String>() {{
            add(BIT);
            add(INT);
            add(BIGINT);
            add(TEXT);
            add(DATE);
            add(TINYINT);
            add(SMALLINT);
        }};
        String ignoreLength = acTableProperties.getIgnoreLengthAndDecimalLength();
        if (StrUtil.isNotBlank(ignoreLength)) {
            resultSet.addAll(StrUtil.split(ignoreLength, StrPool.COMMA));
        }
        return resultSet;
    }

    public void checkTableInfo(TableInfo tableInfo) {
        List<String> keyList = tableInfo.getKeyList();
        List<TableInfo.UniqueInfo> uniqueInfoList = tableInfo.getUniqueInfoList();
        for (TableInfo.UniqueInfo uniqueInfo : uniqueInfoList) {
            if (new HashSet<>(keyList).equals(new HashSet<>(uniqueInfo.getColumns().stream()
                    .map(TableInfo.Index::getColumn)
                    .collect(Collectors.toSet())))) {
                //完全相等，不允许
                throw new RuntimeException(StrUtil.format("表[{}]唯一键和主键字段不能完全相同[{}]", tableInfo.getName(), CollectionUtil.join(keyList, StrPool.COMMA)));
            }
        }

        //自增字段集合
        List<String> autoIncrementList = new ArrayList<>();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            boolean autoIncrement = propertyInfo.isAutoIncrement();
            if (autoIncrement) {
                autoIncrementList.add(propertyInfo.getColumnName());
            }
        }
        if (autoIncrementList.size() > 1) {
            throw new RuntimeException(StrUtil.format("表[{}]只能存在一个自增列，目前有[{}]", tableInfo.getName(), CollectionUtil.join(autoIncrementList, StrPool.COMMA)));
        }
    }

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
        this.checkTableInfo(tableInfo);
        //存储需要执行的表相关sql
        List<String> resultList = new ArrayList<>();
        //存储需要执行的字段备注sql
        List<String> addColumnCommentSqlList = new ArrayList<>();
        String autoIncrementSql = null;
        String tableName = tableInfo.getName();
        String comment = tableInfo.getComment();
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        StringBuilder propertySb = new StringBuilder();
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            String columnName = propertyInfo.getColumnName();
            String columnComment = propertyInfo.getColumnComment();
            if (propertyInfo.isAutoIncrement()) {
                autoIncrementSql = StrUtil.format("alter table {} add column {} identity(1, 1)", this.addKeywordHandle(tableName), this.addKeywordHandle(columnName));
            }
            propertySb.append(StrPool.CRLF)
                    .append(this.getAlterSentence(propertyInfo, true))
                    .append(StrPool.COMMA);
            if (StrUtil.isNotBlank(columnComment)) {
                addColumnCommentSqlList.add(StrUtil.format("comment on column {}.{} is '{}'",
                        this.addKeywordHandle(tableName), this.addKeywordHandle(columnName), columnComment));
            }
        }
        //可显示插入自增主键
        if (CollectionUtil.isNotEmpty(tableInfo.getKeyList())) {
            propertySb.append(StrPool.CRLF)
                    .append(StrUtil.format("primary key ({}),", CollectionUtil.join(tableInfo.getKeyList(), StrPool.COMMA, this::addKeywordHandle)));
        }
        //存储建表sql
        resultList.add(this.addTableSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1).toString(), ""));
        //处理自增作为单独语句，方便新增和修改的getAlterSentence方法统一
        if (StrUtil.isNotBlank(autoIncrementSql)) {
            resultList.add(autoIncrementSql);
        }

        if (StrUtil.isNotBlank(comment)) {
            //存储表备注sql
            resultList.add(StrUtil.format("comment on table {} is '{}'", this.addKeywordHandle(tableName), comment));
        }
        //存储字段备注sql
        resultList.addAll(addColumnCommentSqlList);
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
            resultList.add(StrUtil.format("alter table {} add constraint {} primary key ({})",
                    this.addKeywordHandle(tableName), this.addKeywordHandle(PK_ + tableName + IdUtil.getSnowflakeNextId()),
                    CollectionUtil.join(keyList, StrPool.COMMA, this::addKeywordHandle)));
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
                resultList.add(StrUtil.format("create index {} on {} ({})",
                        this.addKeywordHandle(indexInfo.getValue() + IdUtil.getSnowflakeNextId()), this.addKeywordHandle(tableName),
                        CollectionUtil.join(indexInfo.getColumns(), StrPool.COMMA, (index) ->
                                this.addKeywordHandle(index.getColumn()) + (index.isAsc() ? ASC : DESC))));
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
                resultList.add(StrUtil.format("create unique index {} on {}({})",
                        this.addKeywordHandle(uniqueIndexInfo.getValue() + IdUtil.getSnowflakeNextId()), this.addKeywordHandle(tableName),
                        CollectionUtil.join(uniqueIndexInfo.getColumns(), StrPool.COMMA, (index) ->
                                this.addKeywordHandle(index.getColumn()) + (index.isAsc() ? ASC : DESC))));
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
                resultList.add(StrUtil.format("alter table {} add constraint {} unique ({})",
                        this.addKeywordHandle(tableName), this.addKeywordHandle(uniqueInfo.getValue() + IdUtil.getSnowflakeNextId()),
                        CollectionUtil.join(uniqueInfo.getColumns(), StrPool.COMMA, (index) -> this.addKeywordHandle(index.getColumn()))));
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
                                          Map<String, TableInfo.PropertyInfo> tableColumnInfoMap,
                                          List<ConstraintInfo> constraintInfoList,
                                          List<ConstraintInfo> defaultInfoList,
                                          ModelEnums modelEnums) {
        this.checkTableInfo(tableInfo);
        List<TableInfo.PropertyInfo> propertyInfoList = tableInfo.getPropertyInfoList();
        /*
         * 将语句排序
         * 删除自增约束，新增字段 > 修改字段名 > 索引语句 > 修改字段其他属性
         */
        List<String> addList = new ArrayList<>();
        List<String> updateList = new ArrayList<>();
        List<String> indexList = new ArrayList<>();
        List<String> updateOtherList = new ArrayList<>();
        List<String> otherList = new ArrayList<>();
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
                addList.add(StrUtil.format("alter table {} add column({})",
                        this.addKeywordHandle(tableName), this.getAlterSentence(propertyInfo, true)));
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
                    updateList.add(StrUtil.format("alter table {} alter column {} rename to {}",
                            this.addKeywordHandle(tableName), this.addKeywordHandle(oldColumnName), this.addKeywordHandle(columnName)));
                    tableColumnInfo = oldTableColumnInfo;
                    tableColumnInfo.setColumnName(columnName);
                }
            }

            //先比较默认值
            if (!StrUtil.equals(tableColumnInfo.getDefaultValue(), propertyInfo.getDefaultValue())) {
                if (Objects.isNull(propertyInfo.getDefaultValue())) {
                    updateOtherList.add(StrUtil.format("alter table {} alter column {} drop default", this.addKeywordHandle(tableName), this.addKeywordHandle(columnName)));
                }
            }

            String alterSentence1 = this.getAlterSentence(propertyInfo, true);
            String alterSentence2 = this.getAlterSentence(tableColumnInfo, false);
            /*
             * 在比较其他值，getAlterSentence方法没有处理自增
             *  自增（删除、新增）都是单独的语句，所以需要单独处理
             */
            if (!Objects.equals(alterSentence1, alterSentence2)) {
                this.updateHandle(propertyInfo, tableColumnInfo, addList, updateOtherList, true);
            } else if (propertyInfo.isAutoIncrement() != tableColumnInfo.isAutoIncrement()) {
                this.updateHandle(propertyInfo, tableColumnInfo, addList, updateOtherList, false);
            }

            if (!StrUtil.equalsIgnoreCase(tableColumnInfo.getColumnComment(), propertyInfo.getColumnComment())) {
                //修改字段备注
                otherList.add(StrUtil.format("comment on column {}.{} is '{}'",
                        this.addKeywordHandle(tableName), this.addKeywordHandle(columnName), propertyInfo.getColumnComment()));
            }
        }

        //处理表备注
        if (!Objects.equals(tableComment, dbTableComment)) {
            otherList.add(StrUtil.format("comment on table {} is '{}'", this.addKeywordHandle(tableName), tableComment));
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
            if (Objects.equals(constraintInfo.getConstraintFlag(), PK)) {
                //判断是否完全相等
                // 一个表只会查出来一个主键名称，一个主键名称对应多个字段
                if (!new HashSet<>(list).equals(new HashSet<>(keyList))) {
                    //修改
                    indexList.add(StrUtil.format("alter table {} modify constraint {} to primary key ({})", this.addKeywordHandle(tableName),
                            this.addKeywordHandle(constraintInfo.getConstraintName()), CollectionUtil.join(keyList, StrPool.COMMA, this::addKeywordHandle)));
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

                indexList.add(StrUtil.format("alter table {} drop constraint {}", this.addKeywordHandle(tableName), this.addKeywordHandle(constraintInfo.getConstraintName())));
            } else {
                //普通索引+唯一索引
                indexList.add(StrUtil.format("drop index {}", this.addKeywordHandle(constraintInfo.getConstraintName())));
            }
        }
        if (CollectionUtil.isNotEmpty(keyList)) {
            //新增主键
            this.createPk(keyList, tableName, indexList);
        }
        if (CollectionUtil.isNotEmpty(uniqueInfoList)) {
            //新增唯一键
            this.createUk(uniqueInfoList, tableName, indexList);
        }
        if (CollectionUtil.isNotEmpty(indexInfoList)) {
            //新增普通索引
            this.createIdx(indexInfoList, tableName, indexList);
        }
        if (CollectionUtil.isNotEmpty(uniqueIndexInfoList)) {
            //新增唯一索引
            this.createUkIdx(uniqueIndexInfoList, tableName, indexList);
        }

        if (Objects.equals(modelEnums, ModelEnums.ADD_OR_UPDATE_OR_DEL)) {
            //如果数据库有但是实体类没有，进行删除
            if (CollectionUtil.isNotEmpty(tableColumnInfoMap)) {
                for (Map.Entry<String, TableInfo.PropertyInfo> map : tableColumnInfoMap.entrySet()) {
                    TableInfo.PropertyInfo value = map.getValue();
                    indexList.add(StrUtil.format("alter table {} drop column {}",
                            this.addKeywordHandle(value.getTableName()), this.addKeywordHandle(value.getColumnName())));
                }
            }
        }
        addList.addAll(updateList);
        addList.addAll(indexList);
        addList.addAll(updateOtherList);
        addList.addAll(otherList);
        return addList;
    }

    /**
     * @param propertyInfo
     * @param tableColumnInfo
     * @param addList
     * @param updateOtherList
     * @param flag            true字段其他信息也修改了，false只改了自增
     */

    public void updateHandle(TableInfo.PropertyInfo propertyInfo, TableInfo.PropertyInfo tableColumnInfo, List<String> addList, List<String> updateOtherList, boolean flag) {
        String tableName = propertyInfo.getTableName();
        String columnName = propertyInfo.getColumnName();
        String alterSentence = this.getAlterSentence(propertyInfo, true);
        //数据库是自增，实体类不是自增，删掉自增
        if (tableColumnInfo.isAutoIncrement() && !propertyInfo.isAutoIncrement()) {
            //alter table TEST."t_zero" drop identity;
            addList.add(0, StrUtil.format("alter table {} drop identity", this.addKeywordHandle(tableName)));
        }
        if (flag) {
            updateOtherList.add(StrUtil.format("alter table {} modify {}",
                    this.addKeywordHandle(tableName), alterSentence));
        }
        if (propertyInfo.isAutoIncrement()) {
            //alter table TEST."t_zero" add column "zero" identity(1, 1);
            updateOtherList.add(StrUtil.format("alter table {} add column {} identity(1, 1)", this.addKeywordHandle(tableName), this.addKeywordHandle(columnName)));
        }
    }


    @Override
    public String javaTypeTurnColumnType(String fieldType, String columnType) {
        return Objects.equals(columnType, ColumnTypeConstants.DEFAULT_VALUE)
                ? JavaTypeTurnColumnTypeEnums.getDmByValue(fieldType) : columnType;
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
        long length = propertyInfo.getLength();
        long decimalLength = propertyInfo.getDecimalLength();
        boolean typeLimit = propertyInfo.isTypeLimit();
        if (typeLimit) {
            //类型限制，在已有的集合中找
            Pair<String, Long> pair = dmContains(typeStr);
            if (Objects.nonNull(pair.getValue())) {
                //没有定义返回的是默认的
                typeStr = pair.getKey();
                length = pair.getValue();
            }
        }
        StringBuilder sb = new StringBuilder(this.addKeywordHandle(propertyInfo.getColumnName()));
        sb.append(" ").append(typeStr);
        //处理length和decimalLength
        if (!this.ignoreLengthAndDecimalLength().contains(typeStr)) {
            //直接通过实体类映射或者注解指定的，需要先处理length和decimalLength
            String columnName = propertyInfo.getColumnName();
            String tableName = propertyInfo.getTableName();
            switch (typeStr) {
                case ColumnTypeConstants.DATETIME:
                    if (flag) {
                        //实体类
                        if (length > 9 || length < 0) {
                            log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, typeStr, length, 0);
                            length = 0;
                        }
                    } else {
                        //数据库查询
                        length = decimalLength;
                    }
                    decimalLength = AcTableConstants.NUMBER_UNDEFINED;
                    break;
                case ColumnTypeConstants.VARCHAR:
                case ColumnTypeConstants.CHAR:
                    if (length < 0) {
                        log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, typeStr, length, 255);
                        length = 255;
                    }
                    decimalLength = AcTableConstants.NUMBER_UNDEFINED;
                    break;
                case ColumnTypeConstants.FLOAT:
                case ColumnTypeConstants.DOUBLE:
                    if (length > 126 || length < 0) {
                        log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, typeStr, length, 53);
                        length = 53;
                    }
                    decimalLength = AcTableConstants.NUMBER_UNDEFINED;
                    break;
                case ColumnTypeConstants.DECIMAL:
                case ColumnTypeConstants.NUMERIC:
                    if (length > 38 || length < 0) {
                        log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, typeStr, length, 22);
                        length = 22;
                    }
                    if (decimalLength > 38 || decimalLength > length || decimalLength < 0) {
                        log.warn(COLUMN_DECIMAL_LENGTH_VALID_STR, tableName, columnName, typeStr, decimalLength, length, 2);
                        decimalLength = 2;
                    }
                    break;
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
        } else {
            sb.append(NULL);
        }
        //默认值处理
        String defaultValue = propertyInfo.getDefaultValue();
        if (Objects.nonNull(defaultValue)) {
            if (defaultValue.isEmpty()) {//空字符串
                sb.append(StrUtil.format(DEFAULT, "''"));
            } else {
                sb.append(StrUtil.format(DEFAULT, defaultValue));
            }
        }
        return sb.toString();
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
        return StrUtil.format("SELECT t.TABLE_NAME tableName, " +
                "                   (select COMMENTS from all_tab_comments A1   " +
                "                  WHERE t.TABLE_NAME=A1.TABLE_NAME and t.OWNER=A1.OWNER) tableComment,  " +
                "                       t.COLUMN_NAME columnName,  " +
                "                       (select COMMENTS from all_COL_comments A2   " +
                "                       WHERE  t.TABLE_NAME=A2.TABLE_NAME   " +
                "                       AND t.OWNER=A2.SCHEMA_NAME AND t.COLUMN_NAME=A2.COLUMN_NAME ) columnComment,  " +
                "                       t.CHARACTER_SET_NAME columnCharacterSetName,  " +
                "                       CASE WHEN t.NULLABLE = 'Y' THEN 1 ELSE 0 END isNull,  " +
                "   CASE WHEN t.DATA_TYPE = 'NUMBER' THEN  " +
                "   (CASE WHEN t.DATA_PRECISION IS NULL THEN t.DATA_TYPE  " +
                "         WHEN NVL(t.DATA_SCALE, 0) > 0 THEN t.DATA_TYPE || '(' || t.DATA_PRECISION || ',' || t.DATA_SCALE || ')'  " +
                " ELSE t.DATA_TYPE || '(' || t.DATA_PRECISION || ')' END)  " +
                "       ELSE t.DATA_TYPE END typeStr,  " +
                "   CASE WHEN (SELECT count(1) FROM all_CONS_COLUMNS T4, all_CONSTRAINTS T5  " +
                "              WHERE T4.CONSTRAINT_NAME = T5.CONSTRAINT_NAME AND T5.CONSTRAINT_TYPE = 'P' and T5.OWNER = t.OWNER " +
                "                 AND t.TABLE_NAME = T5.TABLE_NAME(+) AND t.COLUMN_NAME = T4.COLUMN_NAME(+)) > 0 THEN 1  " +
                "            ELSE 0 END isKey,  " +
                "       t.DATA_DEFAULT defaultValue,  " +
                "       case when t.DATA_PRECISION is null then t.DATA_LENGTH else t.DATA_PRECISION end length,  " +
                "       t.DATA_SCALE decimalLength, " +
                "       CASE WHEN (SELECT count(1) " +
                "  FROM SYSOBJECTS TAB " +
                "      ,SYSCOLUMNS COL " +
                " WHERE COL.ID = TAB.ID " +
                "   AND TAB.TYPE$ = 'SCHOBJ' " +
                "   AND TAB.SUBTYPE$ IN ('UTAB') " +
                "   AND COL.INFO2 & 0x01 = 1 " +
                "   AND TAB.SCHID = CURRENT_SCHID and TAB.NAME = t.TABLE_NAME and COL.NAME=t.COLUMN_NAME ) > 0 THEN 1 ELSE 0 END  isAutoIncrement  " +
                " FROM all_TAB_COLUMNS t WHERE t.TABLE_NAME = 't_zero' and t.OWNER=SF_GET_SCHEMA_NAME_BY_ID(CURRENT_SCHID)", tableName);
    }

    @Override
    public String getConstraintInfoSql(String tableName) {
        return StrUtil.format("SELECT IFNULL(C.CONSTRAINT_NAME,IC.INDEX_NAME) constraintName," +
                "                    LISTAGG(IC.COLUMN_NAME, ',') WITHIN GROUP (ORDER BY IC.COLUMN_POSITION) AS constraintColumnName," +
                " LISTAGG(IC.descend, ',') WITHIN GROUP (ORDER BY IC.COLUMN_POSITION) AS indexSortStr," +
                " case when C.CONSTRAINT_TYPE='P' then 1" +
                " when C.CONSTRAINT_TYPE='U' then 2 " +
                " when I.UNIQUENESS='NONUNIQUE' then 3 " +
                "     else 4 end constraintFlag " +
                " FROM  " +
                "    ALL_INDEXES I " +
                "    LEFT JOIN  ALL_IND_COLUMNS IC ON I.INDEX_NAME = IC.INDEX_NAME " +
                "    LEFT JOIN  ALL_CONSTRAINTS C ON I.TABLE_NAME = C.TABLE_NAME AND I.INDEX_NAME = C.INDEX_NAME " +
                " WHERE  " +
                "    I.TABLE_NAME = '{}' and I.TABLE_OWNER=SF_GET_SCHEMA_NAME_BY_ID(CURRENT_SCHID) and IC.COLUMN_NAME is not null " +
                " group by IC.INDEX_NAME, C.CONSTRAINT_TYPE,C.CONSTRAINT_NAME,I.UNIQUENESS", tableName);
    }


}
