package io.gitee.zerowsh.actable.constant;

import io.gitee.zerowsh.actable.emnus.SqlTypeEnums;
import io.gitee.zerowsh.actable.util.AcTableThreadLocalUtils;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.MYSQL;
import static io.gitee.zerowsh.actable.constant.AcTableConstants.SQL_SERVER;

/**
 * 定义常量
 *
 * @author zero
 */
public interface SqlConstants {
    /**
     * mysql相关语句
     */
    String MYSQL_EXIST_SQL = "select count(1) from information_schema.tables where table_name ='{}' and table_schema = (select database())";
    String MYSQL_TABLE_STRUCTURE = "SELECT t.table_name tableName,t.table_comment tableComment," +
            " case when c.IS_NULLABLE='YES' then 1 else 0 end isNull," +
            " c.column_name columnName,c.column_comment columnComment,c.DATA_TYPE typeStr,c.COLUMN_DEFAULT defaultValue," +
            " case when c.NUMERIC_PRECISION !='' and  c.NUMERIC_PRECISION is not null then c.NUMERIC_PRECISION else  c.CHARACTER_MAXIMUM_LENGTH end length," +
            " case when c.NUMERIC_SCALE!='' and c.NUMERIC_SCALE is not null then c.NUMERIC_SCALE else c.DATETIME_PRECISION end decimalLength," +
            " case when c.column_key='PRI' then 1 else 0 end isKey,case when c.EXTRA='auto_increment' then 1 else 0 end isAutoIncrement" +
            " FROM information_schema.columns c,information_schema.tables t WHERE c.table_name = t.table_name and c.table_name='{}'" +
            " and c.table_schema = (select database()) AND t.table_schema = (SELECT DATABASE ()) ";

    String MYSQL_CONSTRAINT_INFO = "select index_name constraintName ,GROUP_CONCAT(column_name order by column_name) constraintColumnName," +
            " case when non_unique=0 then case when index_name='PRIMARY' then 1 else 2 end else 3 end constraintFlag" +
            " from information_schema.statistics where table_name = '{}' and table_schema = (select database())" +
            " GROUP BY constraintName,constraintFlag";

    /**
     * sql_server相关语句
     */
    String SQL_SERVER_EXIST_SQL = "SELECT count(1) FROM sys.all_objects WHERE object_id = OBJECT_ID('{}') AND type IN ('U')";
    String SQL_SERVER_TABLE_STRUCTURE = "SELECT d.name tableName,convert(nvarchar(255), f.value) tableComment,a.name columnName," +
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
            " where d.name='{}'";
    String SQL_SERVER_CONSTRAINT_INFO = "WITH MO_Cook AS (SELECT  IDX.NAME AS constraintName, IDX.TYPE_DESC AS constraintType,COL.NAME AS constraintColumnName,case when IDX.IS_PRIMARY_KEY = 1 then 1 else case when        IDX.IS_UNIQUE_CONSTRAINT = 1 then 2 else 3 end end constraintFlag FROM  SYS.INDEXES IDX JOIN " +
            " SYS.INDEX_COLUMNS IDXCOL ON (IDX.OBJECT_ID = IDXCOL.OBJECT_ID AND IDX.INDEX_ID = IDXCOL.INDEX_ID) JOIN " +
            " SYS.TABLES TAB ON (IDX.OBJECT_ID = TAB.OBJECT_ID) JOIN " +
            " SYS.COLUMNS COL ON (IDX.OBJECT_ID = COL.OBJECT_ID AND IDXCOL.COLUMN_ID = COL.COLUMN_ID) " +
            " where  TAB.NAME='{}') " +
            " select constraintName,constraintType,constraintFlag,stuff((select ','+constraintColumnName from  MO_Cook   " +
            "            where c.constraintName=constraintName and c.constraintType=constraintType and c.constraintFlag=constraintFlag order by constraintColumnName " +
            "            for xml path('')),1,1,'') as constraintColumnName  from MO_Cook c    " +
            "      group by c.constraintName,c.constraintType,c.constraintFlag";
    String SQL_SERVER_DEFAULT_INFO = "select t.name constraintName,syscolumns.name constraintColumnName from (SELECT sysobjects.name,sysobjects.id FROM sysobjects  " +
            "where sysobjects.id IN ( SELECT syscolumns.cdefault FROM sysobjects INNER JOIN syscolumns ON sysobjects.Id= syscolumns.Id WHERE sysobjects.name= '{}' ))t  " +
            "LEFT JOIN syscolumns ON t.Id= syscolumns.cdefault";

    static String getExecuteSql(SqlTypeEnums sqlTypeEnums) {
        String databaseType = AcTableThreadLocalUtils.getDatabaseType();
        String sql = null;
        switch (databaseType) {
            case MYSQL:
                switch (sqlTypeEnums) {
                    case EXIST_TABLE:
                        sql = MYSQL_EXIST_SQL;
                        break;
                    case TABLE_STRUCTURE:
                        sql = MYSQL_TABLE_STRUCTURE;
                        break;
                    case CONSTRAINT_INFO:
                        sql = MYSQL_CONSTRAINT_INFO;
                        break;
                    default:
                }
                break;
            case SQL_SERVER:
                switch (sqlTypeEnums) {
                    case EXIST_TABLE:
                        sql = SQL_SERVER_EXIST_SQL;
                        break;
                    case TABLE_STRUCTURE:
                        sql = SQL_SERVER_TABLE_STRUCTURE;
                        break;
                    case CONSTRAINT_INFO:
                        sql = SQL_SERVER_CONSTRAINT_INFO;
                        break;
                    case DEFAULT_INFO:
                        sql = SQL_SERVER_DEFAULT_INFO;
                        break;
                    default:
                }
                break;
            default:
        }
        return sql;
    }
}
