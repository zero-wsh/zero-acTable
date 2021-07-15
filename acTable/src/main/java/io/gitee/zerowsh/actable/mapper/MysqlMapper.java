package io.gitee.zerowsh.actable.mapper;

import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * mysql接口定义
 *
 * @author zero
 */
public interface MysqlMapper extends BaseDatabaseMapper {
    /**
     * 判断表是否存在
     *
     * @param tableName
     * @return
     */
    @Select("select count(1) from information_schema.tables where table_name =#{tableName}")
    int isExistTable(@Param("tableName") String tableName);


    /**
     * 获取表结构
     *
     * @param tableName
     * @return
     */
    @Select("SELECT t.table_name tableName,t.table_comment tableComment,\n" +
            "\t\t\tcase when c.IS_NULLABLE='YES' then 1 else 0 end isNull,\n" +
            "\t\t\tc.column_name columnName,c.column_comment columnComment,c.DATA_TYPE typeStr,c.COLUMN_DEFAULT defaultValue,\n" +
            "\t\t\tcase when c.NUMERIC_PRECISION !='' and  c.NUMERIC_PRECISION is not null then c.NUMERIC_PRECISION else  c.CHARACTER_MAXIMUM_LENGTH end length,\n" +
            "\t\t\tcase when c.NUMERIC_SCALE!='' and c.NUMERIC_SCALE is not null then c.NUMERIC_SCALE else c.DATETIME_PRECISION end decimalLength,case when c.column_key='PRI' then 1 else 0 end isKey,\n" +
            "\t\t\tcase when c.EXTRA='auto_increment' then 1 else 0 end isAutoIncrement \n" +
            "\t\t\tFROM information_schema.columns c,information_schema.tables t WHERE c.table_name = t.table_name and c.table_name=#{tableName}")
    List<TableColumnInfo> getTableStructure(@Param("tableName") String tableName);

    /**
     * 获取表约束信息（主键、唯一键、索引）
     *
     *
     * @param tableName
     * @return
     */
    @Select("select index_name constraintName ,GROUP_CONCAT(column_name order by column_name) constraintColumnName,\n" +
            "\t\t\t\tcase when non_unique=0 then case when index_name='PRIMARY' then 1 else 2 end else 3 end constraintFlag\n" +
            "from information_schema.statistics where table_name = #{tableName} \n" +
            "GROUP BY constraintName,constraintFlag")
    List<ConstraintInfo> getConstraintInfo(@Param("tableName") String tableName);
}
