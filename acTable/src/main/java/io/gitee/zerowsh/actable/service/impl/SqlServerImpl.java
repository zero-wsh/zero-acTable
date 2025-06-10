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
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.JavaTypeTurnColumnTypeEnums;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.properties.AcTableProperties;
import io.gitee.zerowsh.actable.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.*;
import static io.gitee.zerowsh.actable.constant.ColumnTypeConstants.*;
import static io.gitee.zerowsh.actable.constant.StringConstants.LEFT_BRACKET;
import static io.gitee.zerowsh.actable.constant.StringConstants.RIGHT_BRACKET;

/**
 * sqlserver数据库实现
 *
 * @author zero
 */
@Slf4j
public class SqlServerImpl extends DatabaseService {
    public SqlServerImpl() {
        super("[", "]");
    }


    @Override
    public Set<String> ignoreLengthAndDecimalLength() {
        AcTableProperties acTableProperties = SpringUtil.getBean(AcTableProperties.class);
        HashSet<String> resultSet = new HashSet<String>() {{
            add(BIT);
            add(INT);
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
    @Override
    public List<String> getCreateTableSql(TableInfo tableInfo) {
        this.checkTableInfo(tableInfo);
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
            propertySb.append(StrPool.CRLF)
                    .append(this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue()))
                    .append(StrPool.COMMA);
            if (StrUtil.isNotBlank(columnComment)) {
                addColumnCommentSqlList.add(StrUtil.format("EXEC sp_addextendedproperty 'MS_Description', N'{}','SCHEMA', N'dbo','TABLE', N'{}','COLUMN', N'{}'", columnComment,
                        tableName, columnName));
            }
        }
        //可显示插入自增主键
        if (CollectionUtil.isNotEmpty(tableInfo.getKeyList())) {
            propertySb.append(StrPool.CRLF)
                    .append(StrUtil.format("primary key ({}),", CollectionUtil.join(tableInfo.getKeyList(), StrPool.COMMA, this::addKeywordHandle)));
        }
        if (CollectionUtil.isNotEmpty(tableInfo.getUniqueInfoList())) {
            for (TableInfo.UniqueInfo uniqueInfo : tableInfo.getUniqueInfoList()) {
                propertySb.append(StrPool.CRLF)
                        .append(StrUtil.format("UNIQUE NONCLUSTERED ({}),",
                                CollectionUtil.join(uniqueInfo.getColumns(), StrPool.COMMA, (index) -> this.addKeywordHandle(index.getColumn()))));
            }
        }
        //存储建表sql
        resultList.add(this.addTableSql(tableName, propertySb.deleteCharAt(propertySb.length() - 1).toString(), ""));

        if (StrUtil.isNotBlank(comment)) {
            //存储表备注sql
            resultList.add(StrUtil.format("EXEC sp_addextendedproperty 'MS_Description', N'{}','SCHEMA', N'dbo','TABLE', N'{}'", comment, tableName));
        }
        //存储字段备注sql
        resultList.addAll(addColumnCommentSqlList);
        //创建索引
        this.createIdx(tableInfo.getIndexInfoList(), tableName, resultList);
        //创建唯一索引
        this.createUkIdx(tableInfo.getUniqueIndexInfoList(), tableName, resultList);
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
            resultList.add(StrUtil.format("ALTER TABLE {} ADD CONSTRAINT {} PRIMARY KEY CLUSTERED ({})",
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
                resultList.add(StrUtil.format("CREATE NONCLUSTERED INDEX {} on {} ({})",
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
                resultList.add(StrUtil.format("CREATE UNIQUE NONCLUSTERED INDEX {} on {}({})",
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
                resultList.add(StrUtil.format("CREATE UNIQUE NONCLUSTERED INDEX {} on {}({})",
                        this.addKeywordHandle(uniqueInfo.getValue() + IdUtil.getSnowflakeNextId()), this.addKeywordHandle(tableName),
                        CollectionUtil.join(uniqueInfo.getColumns(), StrPool.COMMA, (index) -> this.addKeywordHandle(index.getColumn()))));
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
    private static void handleColumnComment(TableInfo.PropertyInfo tableColumnInfo, TableInfo.PropertyInfo propertyInfo, List<String> resultList, String tableName) {
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
     * @param tableColumnInfoMap
     * @param constraintInfoList
     * @param defaultInfoList
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
                addList.add(StrUtil.format("ALTER TABLE [dbo].{} ADD {}",
                        this.addKeywordHandle(tableName), this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue())));
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
                    updateList.add(StrUtil.format("EXEC sp_rename '[dbo].{}.{}', '{}', 'COLUMN'",
                            this.addKeywordHandle(tableName), this.addKeywordHandle(oldColumnName), columnName));
                    updateOtherList.add(StrUtil.format("ALTER TABLE [dbo].{} ALTER COLUMN {}",
                            this.addKeywordHandle(tableName), this.jointDefault(this.getAlterSentence(propertyInfo, true), propertyInfo.getDefaultValue())));
                    continue;
                }
            }


            //先比较默认值
            if (!StrUtil.equals(tableColumnInfo.getDefaultValue(), propertyInfo.getDefaultValue())) {
                this.updateHandle(propertyInfo, tableColumnInfo, addList, updateOtherList, true);
            } else {
                String alterSentence1 = this.getAlterSentence(propertyInfo, true);
                String alterSentence2 = this.getAlterSentence(tableColumnInfo, false);
                //在比较其他值
                if (!Objects.equals(alterSentence1, alterSentence2)) {
                    this.updateHandle(propertyInfo, tableColumnInfo, addList, updateOtherList, true);
                } else if (propertyInfo.isAutoIncrement() != tableColumnInfo.isAutoIncrement()) {
                    this.updateHandle(propertyInfo, tableColumnInfo, addList, updateOtherList, false);
                }
            }
            if (!StrUtil.equalsIgnoreCase(tableColumnInfo.getColumnComment(), propertyInfo.getColumnComment())) {
                //修改字段备注
                otherList.add(StrUtil.format("EXEC sp_updateextendedproperty 'MS_Description', N'{}','SCHEMA', N'dbo','TABLE', N'{}','COLUMN', N'{}'", propertyInfo.getColumnComment(),
                        tableName, columnName));
            }
        }

        //处理表备注
        if (!Objects.equals(tableComment, dbTableComment)) {
            otherList.add(StrUtil.format("EXEC sp_updateextendedproperty 'MS_Description', N'{}','SCHEMA', N'dbo','TABLE', N'{}'", tableComment, tableName));
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
                    indexList.add(StrUtil.format("ALTER TABLE [dbo].{} DROP CONSTRAINT {}", this.addKeywordHandle(tableName),
                            this.addKeywordHandle(constraintInfo.getConstraintName())));
                    indexList.add(StrUtil.format("ALTER TABLE [dbo].{} ADD CONSTRAINT {} PRIMARY KEY  ({})", this.addKeywordHandle(tableName),
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

                indexList.add(StrUtil.format("ALTER TABLE [dbo].{} DROP CONSTRAINT {}", this.addKeywordHandle(tableName), this.addKeywordHandle(constraintInfo.getConstraintName())));
            } else {
                //普通索引+唯一索引
                indexList.add(StrUtil.format("DROP INDEX {} ON [dbo].{}", this.addKeywordHandle(constraintInfo.getConstraintName()), this.addKeywordHandle(tableName)));
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
                    this.addKeywordHandle(tableName), this.jointDefault(alterSentence, propertyInfo.getDefaultValue())));
        }
        if (propertyInfo.isAutoIncrement()) {
            //alter table TEST."t_zero" add column "zero" identity(1, 1);
            updateOtherList.add(StrUtil.format("alter table {} add column {} identity(1, 1)", this.addKeywordHandle(tableName), this.addKeywordHandle(columnName)));
        }
    }

    @Override
    public String javaTypeTurnColumnType(String fieldType, String columnType) {
        return Objects.equals(columnType, ColumnTypeConstants.DEFAULT_VALUE)
                ? JavaTypeTurnColumnTypeEnums.getSqlServerByValue(fieldType) : columnType;
    }

    @Override
    public String getAllTableSql() {
        return "select name from sys.tables";
    }

    @Override
    public String existTableSql(String tableName) {
        return StrUtil.format("SELECT count(1) FROM sys.all_objects WHERE object_id = OBJECT_ID('{}') AND type IN ('U')", tableName);
    }

    @Override
    public String getTableStructureSql(String tableName) {
        return StrUtil.format("SELECT d.name tableName,convert(nvarchar(255), f.value) tableComment,a.name columnName," +
                " case when COLUMNPROPERTY( a.id,a.name,'IsIdentity')=1 then  1 else 0 end isAutoIncrement," +
                " case when exists(SELECT 1 FROM sysobjects where xtype='PK' and parent_obj=a.id and name in (" +
                " SELECT name FROM sysindexes WHERE indid in( SELECT indid FROM sysindexkeys WHERE id = a.id AND colid=a.colid))) then 1 else 0 end isKey," +
                " b.name typeStr, COLUMNPROPERTY(a.id,a.name,'PRECISION') length," +
                " isnull(COLUMNPROPERTY(a.id,a.name,'Scale'),0) decimalLength," +
                " case when a.isnullable=1 then 1 else 0 end isNull,convert(nvarchar(255), e.text) defaultValue,convert(nvarchar(255), g.value) columnComment" +
                " FROM syscolumns a" +
                " left join systypes b on a.xusertype=b.xusertype" +
                " inner join sysobjects d on a.id=d.id  and d.xtype='U' and  d.name<>'dtproperties'" +
                " left join syscomments e on a.cdefault=e.id" +
                " left join sys.extended_properties g on a.id=G.major_id and a.colid=g.minor_id" +
                " left join sys.extended_properties f on d.id=f.major_id and f.minor_id=0 where d.name='{}'", tableName);
    }

    @Override
    public String getConstraintInfoSql(String tableName) {
        return StrUtil.format("WITH MO_Cook AS (SELECT  IDX.NAME AS constraintName, IDX.TYPE_DESC AS constraintType,COL.NAME AS constraintColumnName,case when IDX.IS_PRIMARY_KEY = 1 then 1 else case when        IDX.IS_UNIQUE_CONSTRAINT = 1 then 2 else 3 end end constraintFlag FROM  SYS.INDEXES IDX JOIN " +
                " SYS.INDEX_COLUMNS IDXCOL ON (IDX.OBJECT_ID = IDXCOL.OBJECT_ID AND IDX.INDEX_ID = IDXCOL.INDEX_ID) JOIN " +
                " SYS.TABLES TAB ON (IDX.OBJECT_ID = TAB.OBJECT_ID) JOIN " +
                " SYS.COLUMNS COL ON (IDX.OBJECT_ID = COL.OBJECT_ID AND IDXCOL.COLUMN_ID = COL.COLUMN_ID) " +
                " where  TAB.NAME='{}') " +
                " select constraintName,constraintType,constraintFlag,stuff((select ','+constraintColumnName from  MO_Cook   " +
                " where c.constraintName=constraintName and c.constraintType=constraintType and c.constraintFlag=constraintFlag order by constraintColumnName " +
                " for xml path('')),1,1,'') as constraintColumnName  from MO_Cook c" +
                " group by c.constraintName,c.constraintType,c.constraintFlag", tableName);
    }


    @Override
    public String getDefaultInfoSql(String tableName) {
        return StrUtil.format("select t.name constraintName,syscolumns.name constraintColumnName,4 constraintFlag from (SELECT sysobjects.name,sysobjects.id FROM sysobjects  " +
                "where sysobjects.id IN ( SELECT syscolumns.cdefault FROM sysobjects INNER JOIN syscolumns ON sysobjects.Id= syscolumns.Id WHERE sysobjects.name= '{}' ))t  " +
                "LEFT JOIN syscolumns ON t.Id= syscolumns.cdefault", tableName);
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
//            String[] columns = uniqueInfo.getColumns();
//            if (ArrayUtil.isNotEmpty(columns)) {
//                Arrays.sort(columns);
//                set.add(StrUtil.join(StrUtil.COMMA, columns));
//            }
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
//            String[] columns = indexInfo.getColumns();
//            if (ArrayUtil.isNotEmpty(columns)) {
//                Arrays.sort(columns);
//                set.add(StrUtil.join(StrUtil.COMMA, columns));
//            }
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
//            String[] columns = uniqueInfo.getColumns();
//            for (String column : columns) {
//                if (Objects.equals(column, propertyInfo.getColumnName())) {
//                    return true;
//                }
//            }
        }
        return false;
    }


    private static boolean handleIdxConstraint(TableInfo tableInfo, TableInfo.PropertyInfo propertyInfo) {
        List<TableInfo.IndexInfo> indexInfoList = tableInfo.getIndexInfoList();
        for (TableInfo.IndexInfo indexInfo : indexInfoList) {
//            String[] columns = indexInfo.getColumns();
//            for (String column : columns) {
//                if (Objects.equals(column, propertyInfo.getColumnName())) {
//                    return true;
//                }
//            }
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
        propertySb.append(StrUtil.COMMA);
    }

    /**
     * 拼接列类型
     *
     * @param propertySb
     * @param propertyInfo
     * @param tableName
     */
    private static void splicingColumnType(StringBuilder propertySb, TableInfo.PropertyInfo propertyInfo, String tableName) {
        String type = propertyInfo.getTypeStr();
        long length = propertyInfo.getLength();
        long decimalLength = propertyInfo.getDecimalLength();
        String columnName = propertyInfo.getColumnName();
        ColumnTypeEnums typeEnum = ColumnTypeEnums.getSqlServerByValue(type);
        propertySb.append(StrPool.CRLF);
        switch (typeEnum) {
            case VARCHAR:
            case NVARCHAR:
            case DATETIME2:
            case NCHAR:
            case CHAR:
                propertySb.append(type).append(LEFT_BRACKET);
                if (Objects.equals(type, ColumnTypeEnums.DATETIME2.getSqlServer())) {
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
                propertySb.append(RIGHT_BRACKET);
                break;

            case DECIMAL:
            case NUMERIC:
                propertySb.append(type).append(LEFT_BRACKET);

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
                    propertySb.append(StrUtil.COMMA).append(2);
                } else {
                    propertySb.append(StrUtil.COMMA).append(decimalLength);
                }
                propertySb.append(RIGHT_BRACKET);
                break;
            default:
                propertySb.append(type);
        }
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
            Pair<String, Long> pair = sqlServerContains(typeStr);
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
                case ColumnTypeConstants.VARCHAR:
                case ColumnTypeConstants.NVARCHAR:
                case ColumnTypeConstants.NCHAR:
                case ColumnTypeConstants.CHAR:
                    if (length < 0) {
                        log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, typeStr, length, 255);
                        length = 255;
                    }
                    decimalLength = AcTableConstants.NUMBER_UNDEFINED;
                    break;
                case ColumnTypeConstants.DATETIME2:
                    //对类型特殊处理
                    if (length > 7 || length < 0) {
                        log.warn(COLUMN_LENGTH_VALID_STR, tableName, columnName, typeStr, length, 0);
                        length = 7;
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
            if (propertyInfo.isAutoIncrement()) {
                //加上自增的逻辑
                sb.append(IDENTITY);
            }
        } else {
            sb.append(NULL);
        }
        //默认值占位符
        sb.append("{}");
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
