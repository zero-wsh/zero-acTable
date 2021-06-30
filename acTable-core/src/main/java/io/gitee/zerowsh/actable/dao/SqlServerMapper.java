package io.gitee.zerowsh.actable.dao;

import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.provider.SqlServerProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;

/**
 * sql_server接口定义
 *
 * @author zero
 */
public interface SqlServerMapper {
    /**
     * 判断表是否存在
     *
     * @param tableName
     * @return
     */
    @SelectProvider(type = SqlServerProvider.class, method = "isExistTable")
    int isExistTable(@Param("tableName") String tableName);

    /**
     * 执行sql
     *
     * @param sql
     */
    @SelectProvider(type = SqlServerProvider.class, method = "executeSql")
    void executeSql(@Param("sql") String sql);

    /**
     * 获取表结构
     * https://blog.csdn.net/huang714/article/details/105063751?utm_medium=distribute.pc_relevant.none-task-blog-baidujs_title-0&spm=1001.2101.3001.4242
     *
     * @param tableName
     * @return
     */
    @SelectProvider(type = SqlServerProvider.class, method = "getTableStructure")
    List<TableColumnInfo> getTableStructure(@Param("tableName") String tableName);

    /**
     * 获取表约束信息（主键、唯一键、索引）
     * https://www.cnblogs.com/yangdunqin/articles/ys.html
     *
     * @param tableName
     * @return
     */
    @SelectProvider(type = SqlServerProvider.class, method = "getConstraintInfo")
    List<ConstraintInfo> getConstraintInfo(@Param("tableName") String tableName);

    /**
     * 获取表约束信息（默认值约束）
     * https://blog.csdn.net/my98800/article/details/69664327
     *
     * @param tableName
     * @return
     */
    @SelectProvider(type = SqlServerProvider.class, method = "getDefaultInfo")
    List<ConstraintInfo> getDefaultInfo(@Param("tableName") String tableName);
}
