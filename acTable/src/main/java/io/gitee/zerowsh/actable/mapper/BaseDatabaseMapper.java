package io.gitee.zerowsh.actable.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 数据库接口超类
 *
 * @author zero
 */
public interface BaseDatabaseMapper {

    /**
     * 执行sql
     *
     * @param sql
     */
    @Select("${sql}")
    void executeSql(@Param("sql") String sql);
}
