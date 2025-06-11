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
            add(BIGINT);
            add(SMALLINT);
            add(DATE);
            add(TEXT);
            add(TINYINT);
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
                resultList.add(StrUtil.format("ALTER TABLE [dbo].{} ADD CONSTRAINT {} UNIQUE NONCLUSTERED ({})", this.addKeywordHandle(tableName),
                        this.addKeywordHandle(uniqueInfo.getValue() + IdUtil.getSnowflakeNextId()),
                        CollectionUtil.join(uniqueInfo.getColumns(), StrPool.COMMA, (index) -> this.addKeywordHandle(index.getColumn()))));
            }
        }
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
        List<String> addList = new ArrayList<>();
        List<String> updateList = new ArrayList<>();
        List<String> indexList = new ArrayList<>();
        List<String> otherList = new ArrayList<>();
        String tableComment = tableInfo.getComment();

        //数据库表中自增字段
        String tableAutoIncrementColumnName = null;
        if (CollectionUtil.isNotEmpty(constraintInfoList)) {
            for (Map.Entry<String, TableInfo.PropertyInfo> propertyInfoEntry : tableColumnInfoMap.entrySet()) {
                TableInfo.PropertyInfo value = propertyInfoEntry.getValue();
                boolean autoIncrement = value.isAutoIncrement();
                if (autoIncrement) {
                    tableAutoIncrementColumnName = value.getColumnName();
                    break;
                }
            }
        }
        //实体类自增字段
        String entityAutoIncrementColumnName = null;
        for (TableInfo.PropertyInfo propertyInfo : propertyInfoList) {
            boolean autoIncrement = propertyInfo.isAutoIncrement();
            if (autoIncrement) {
                entityAutoIncrementColumnName = propertyInfo.getColumnName();
                break;
            }
        }
        if (Objects.nonNull(tableAutoIncrementColumnName) && !Objects.equals(tableAutoIncrementColumnName, entityAutoIncrementColumnName)) {
            throw new RuntimeException(StrUtil.format("表[{}]自增主键字段[{}]不允许改变", tableInfo.getName(), tableAutoIncrementColumnName));
        }
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
                    tableColumnInfo = oldTableColumnInfo;
                    tableColumnInfo.setColumnName(columnName);
                }
            }
            String defaultValue = propertyInfo.getDefaultValue();
            if (Objects.nonNull(defaultValue)) {//这里的判断只需要判断是否为null,空字符串是允许的
                //特殊默认值处理
                switch (tableColumnInfo.getTypeStr()) {
                    case BIT:
                        defaultValue = StrUtil.format("(({}))", defaultValue);
                        break;

                    case DATE:
                    case DATETIME:
                    case DATETIME2:
                        if (Objects.equals(defaultValue, "CURRENT_TIMESTAMP")) {
                            defaultValue = StrUtil.format("({})", "getdate()");
                        } else {
                            defaultValue = StrUtil.format("({})", defaultValue);
                        }
                        break;
                    default:
                        defaultValue = StrUtil.format("({})", defaultValue);
                        break;
                }
            }
            //先比较默认值
            if (!StrUtil.equals(tableColumnInfo.getDefaultValue(), defaultValue)) {
                for (ConstraintInfo constraintInfo : defaultInfoList) {
                    String constraintColumnName = constraintInfo.getConstraintColumnName();
                    if (Objects.equals(constraintColumnName, tableColumnInfo.getColumnName())) {
                        //删除
                        //ALTER TABLE [dbo].[t_zero] DROP CONSTRAINT [DF__t_zero__test11__6EF57B66]
                        updateList.add(StrUtil.format("ALTER TABLE [dbo].{} DROP CONSTRAINT {}",
                                tableName, constraintInfo.getConstraintName()));
                        break;
                    }
                }
                if (Objects.nonNull(defaultValue)) {
                    updateList.add(StrUtil.format("ALTER TABLE [dbo].{} ADD CONSTRAINT {} DEFAULT {} FOR {}",
                            this.addKeywordHandle(tableName), this.addKeywordHandle(StrUtil.format("DF__{}__{}__", tableName, columnName) + IdUtil.getSnowflakeNextId())
                            , propertyInfo.getDefaultValue(), this.addKeywordHandle(columnName)));
                }
            }
            String alterSentence1 = this.getAlterSentence(propertyInfo, true);
            String alterSentence2 = this.getAlterSentence(tableColumnInfo, false);
            //在比较其他值
            if (!Objects.equals(alterSentence1, alterSentence2)) {
                for (ConstraintInfo constraintInfo : constraintInfoList) {
                    Integer constraintFlag = constraintInfo.getConstraintFlag();
                    if (Objects.equals(constraintFlag, UK)) {
                        List<String> list = Arrays.asList(constraintInfo.getConstraintColumnName().split(StrPool.COMMA));
                        if (list.contains(tableColumnInfo.getColumnName())) {
                            //先删除
                            //ALTER TABLE [dbo].[t_zero] DROP CONSTRAINT [UQ__t_zero__FF33573DF6A1794C]
                            updateList.add(StrUtil.format("ALTER TABLE [dbo].{} DROP CONSTRAINT {}", this.addKeywordHandle(tableName),
                                    this.addKeywordHandle(constraintInfo.getConstraintName())));
                            constraintInfoList.remove(constraintInfo);
                            break;
                        }
                    }
                }

                //默认值处理过了，这里直接不拼接
                updateList.add(StrUtil.format("ALTER TABLE [dbo].{} ALTER COLUMN {}", this.addKeywordHandle(tableName),
                        StrUtil.format(this.getAlterSentence(propertyInfo, true), "")));
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
        addList.addAll(otherList);
        return addList;
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
        return StrUtil.format("SELECT d.name tableName, " +
                "convert(nvarchar(255), f.value) tableComment, " +
                "a.name columnName, " +
                "case when COLUMNPROPERTY( a.id,a.name,'IsIdentity')=1 then  1 else 0 end isAutoIncrement, " +
                "case when exists(SELECT 1 FROM sysobjects where xtype='PK' and parent_obj=a.id and name in ( " +
                "SELECT name FROM sysindexes WHERE indid in( SELECT indid FROM sysindexkeys WHERE id = a.id AND colid=a.colid))) then 1 else 0 end isKey, " +
                "b.name typeStr,  " +
                "CASE  WHEN b.name IN ('datetime2', 'datetimeoffset', 'time') THEN  " +
                "COLUMNPROPERTY(a.id, a.name, 'Scale')  ELSE COLUMNPROPERTY(a.id, a.name, 'PRECISION') END as length, " +
                "isnull(COLUMNPROPERTY(a.id,a.name,'Scale'),0) decimalLength, " +
                "case when a.isnullable=1 then 1 else 0 end isNull,convert(nvarchar(255), e.text) defaultValue,convert(nvarchar(255), g.value) columnComment " +
                "FROM syscolumns a " +
                "left join systypes b on a.xusertype=b.xusertype " +
                "inner join sysobjects d on a.id=d.id  and d.xtype='U' and  d.name<>'dtproperties' " +
                "left join syscomments e on a.cdefault=e.id " +
                "left join sys.extended_properties g on a.id=G.major_id and a.colid=g.minor_id " +
                "left join sys.extended_properties f on d.id=f.major_id and f.minor_id=0 where d.name='{}'", tableName);
    }

    @Override
    public String getConstraintInfoSql(String tableName) {
        return StrUtil.format("WITH MO_Cook AS( SELECT IDX.NAME AS constraintName, COL.NAME AS constraintColumnName," +
                " CASE WHEN IDX.IS_PRIMARY_KEY = 1 THEN 1 WHEN IDX.IS_UNIQUE_CONSTRAINT = 1 AND IDX.IS_UNIQUE = 1 THEN 2 WHEN IDX.IS_UNIQUE = 1 THEN 4 ELSE 3 END AS constraintFlag," +
                " IDXCOL.is_descending_key AS isDescending, IDXCOL.key_ordinal AS columnOrder FROM SYS.INDEXES IDX " +
                " JOIN SYS.INDEX_COLUMNS IDXCOL ON (IDX.OBJECT_ID = IDXCOL.OBJECT_ID AND IDX.INDEX_ID = IDXCOL.INDEX_ID) " +
                " JOIN SYS.TABLES TAB ON (IDX.OBJECT_ID = TAB.OBJECT_ID) " +
                " JOIN SYS.COLUMNS COL ON (IDX.OBJECT_ID = COL.OBJECT_ID AND IDXCOL.COLUMN_ID = COL.COLUMN_ID) WHERE TAB.NAME = '{}')" +
                " SELECT constraintName, constraintFlag, STUFF(( SELECT ',' + constraintColumnName FROM MO_Cook m WHERE m.constraintName = c.constraintName AND m.constraintFlag = c.constraintFlag " +
                " ORDER BY m.columnOrder FOR XML PATH('') ), 1, 1, '') AS constraintColumnName, " +
                " STUFF(( SELECT ',' + CASE WHEN isDescending = 1 THEN 'DESC' ELSE 'ASC' END FROM MO_Cook m WHERE m.constraintName = c.constraintName AND m.constraintFlag = c.constraintFlag" +
                " ORDER BY m.columnOrder FOR XML PATH('') ), 1, 1, '') AS indexSortStr FROM MO_Cook c GROUP BY c.constraintName, c.constraintFlag", tableName);
    }


    @Override
    public String getDefaultInfoSql(String tableName) {
        return StrUtil.format("select t.name constraintName,syscolumns.name constraintColumnName,5 constraintFlag,'' indexSortStr from (SELECT sysobjects.name,sysobjects.id FROM sysobjects  " +
                "where sysobjects.id IN ( SELECT syscolumns.cdefault FROM sysobjects INNER JOIN syscolumns ON sysobjects.Id= syscolumns.Id WHERE sysobjects.name= '{}' ))t  " +
                "LEFT JOIN syscolumns ON t.Id= syscolumns.cdefault", tableName);
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
