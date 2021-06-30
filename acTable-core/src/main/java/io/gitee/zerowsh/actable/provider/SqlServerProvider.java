package io.gitee.zerowsh.actable.provider;

/**
 * sqlsever数据库相关操作
 *
 * @author zero
 */
@SuppressWarnings("all")
public class SqlServerProvider {
    public String isExistTable() {
        return "SELECT count(1) FROM sys.all_objects WHERE object_id = OBJECT_ID(N'${tableName}') AND type IN ('U')";
    }


    public String executeSql() {
        return "${sql}";
    }

    public String dropTable(String tableName) {
        return "DROP TABLE " + tableName;
    }

    public String getTableStructure() {
        return "SELECT d.name tableName,convert(nvarchar(255), f.value) tableComment,a.name columnName," +
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
                " where d.name='${tableName}'";
    }

    public String getConstraintInfo() {
        return "WITH MO_Cook AS\t(SELECT  IDX.NAME AS constraintName, IDX.TYPE_DESC AS constraintType,COL.NAME AS constraintColumnName,case when IDX.IS_PRIMARY_KEY = 1 then 1 else case when \t\t\t\t\t\t\tIDX.IS_UNIQUE_CONSTRAINT = 1 then 2 else 3 end end constraintFlag FROM  SYS.INDEXES IDX JOIN\n" +
                "                SYS.INDEX_COLUMNS IDXCOL ON (IDX.OBJECT_ID = IDXCOL.OBJECT_ID AND IDX.INDEX_ID = IDXCOL.INDEX_ID) JOIN\n" +
                "                SYS.TABLES TAB ON (IDX.OBJECT_ID = TAB.OBJECT_ID) JOIN\n" +
                "                SYS.COLUMNS COL ON (IDX.OBJECT_ID = COL.OBJECT_ID AND IDXCOL.COLUMN_ID = COL.COLUMN_ID)\n" +
                "                where  TAB.NAME='${tableName}')\n" +
                "\tselect constraintName,constraintType,constraintFlag,stuff((select ','+constraintColumnName from  MO_Cook  \n" +
                "            where c.constraintName=constraintName and c.constraintType=constraintType and c.constraintFlag=constraintFlag order by constraintColumnName\n" +
                "            for xml path('')),1,1,'') as constraintColumnName  from MO_Cook c   \n" +
                "\t\t\t\t\t\tgroup by c.constraintName,c.constraintType,c.constraintFlag";
    }

    public String getDefaultInfo() {
        return "select t.name constraintName,syscolumns.name constraintColumnName from (SELECT sysobjects.name,sysobjects.id FROM\tsysobjects \n" +
                "where\tsysobjects.id IN ( SELECT syscolumns.cdefault FROM sysobjects INNER JOIN syscolumns ON sysobjects.Id= syscolumns.Id WHERE sysobjects.name= N'${tableName}' ))t \n" +
                "LEFT JOIN syscolumns ON t.Id= syscolumns.cdefault";
    }
}
