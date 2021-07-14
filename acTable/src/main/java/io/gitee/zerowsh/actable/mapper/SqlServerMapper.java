package io.gitee.zerowsh.actable.mapper;

import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * sql_server接口定义
 *
 * @author zero
 */
public interface SqlServerMapper extends BaseDatabaseMapper {
    /**
     * 判断表是否存在
     *
     * @param tableName
     * @return
     */
    @Select("SELECT count(1) FROM sys.all_objects WHERE object_id = OBJECT_ID(#{tableName}) AND type IN ('U')")
    int isExistTable(@Param("tableName") String tableName);

    /**
     * 获取表结构
     * https://blog.csdn.net/huang714/article/details/105063751?utm_medium=distribute.pc_relevant.none-task-blog-baidujs_title-0&spm=1001.2101.3001.4242
     *
     * @param tableName
     * @return
     */
    @Select("SELECT d.name tableName,convert(nvarchar(255), f.value) tableComment,a.name columnName," +
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
            " left join sys.extended_properties f on d.id=f.major_id and f.minor_id=0" +
            " where d.name=#{tableName}")
    List<TableColumnInfo> getTableStructure(@Param("tableName") String tableName);

    /**
     * 获取表约束信息（主键、唯一键、索引）
     * https://www.cnblogs.com/yangdunqin/articles/ys.html
     *
     * @param tableName
     * @return
     */
    @Select("WITH MO_Cook AS\t(SELECT  IDX.NAME AS constraintName, IDX.TYPE_DESC AS constraintType,COL.NAME AS constraintColumnName,case when IDX.IS_PRIMARY_KEY = 1 then 1 else case when \t\t\t\t\t\t\tIDX.IS_UNIQUE_CONSTRAINT = 1 then 2 else 3 end end constraintFlag FROM  SYS.INDEXES IDX JOIN\n" +
            "                SYS.INDEX_COLUMNS IDXCOL ON (IDX.OBJECT_ID = IDXCOL.OBJECT_ID AND IDX.INDEX_ID = IDXCOL.INDEX_ID) JOIN\n" +
            "                SYS.TABLES TAB ON (IDX.OBJECT_ID = TAB.OBJECT_ID) JOIN\n" +
            "                SYS.COLUMNS COL ON (IDX.OBJECT_ID = COL.OBJECT_ID AND IDXCOL.COLUMN_ID = COL.COLUMN_ID)\n" +
            "                where  TAB.NAME=#{tableName})\n" +
            "\tselect constraintName,constraintType,constraintFlag,stuff((select ','+constraintColumnName from  MO_Cook  \n" +
            "            where c.constraintName=constraintName and c.constraintType=constraintType and c.constraintFlag=constraintFlag order by constraintColumnName\n" +
            "            for xml path('')),1,1,'') as constraintColumnName  from MO_Cook c   \n" +
            "\t\t\t\t\t\tgroup by c.constraintName,c.constraintType,c.constraintFlag")
    List<ConstraintInfo> getConstraintInfo(@Param("tableName") String tableName);

    /**
     * 获取表约束信息（默认值约束）
     * https://blog.csdn.net/my98800/article/details/69664327
     *
     * @param tableName
     * @return
     */
    @Select("select t.name constraintName,syscolumns.name constraintColumnName from (SELECT sysobjects.name,sysobjects.id FROM\tsysobjects \n" +
            "where\tsysobjects.id IN ( SELECT syscolumns.cdefault FROM sysobjects INNER JOIN syscolumns ON sysobjects.Id= syscolumns.Id WHERE sysobjects.name= #{tableName} ))t \n" +
            "LEFT JOIN syscolumns ON t.Id= syscolumns.cdefault")
    List<ConstraintInfo> getDefaultInfo(@Param("tableName") String tableName);
}
