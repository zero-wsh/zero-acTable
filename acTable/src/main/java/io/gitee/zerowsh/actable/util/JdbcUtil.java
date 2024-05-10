package io.gitee.zerowsh.actable.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.properties.AcTableProperties;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zero
 */
@Slf4j
public class JdbcUtil {

    public static void executeSql(Connection conn, String sql, Object... obj) throws SQLException {
        try (PreparedStatement ps = handlePrepareStatement(conn, sql, obj)) {
            ps.execute();
        }
    }

    /**
     * @param conn
     * @param sql
     * @param obj
     * @return 是否存在数据
     */
    public static boolean isExist(Connection conn, String sql, Object... obj) throws SQLException {
        try (PreparedStatement ps = handlePrepareStatement(conn, sql, obj);
             ResultSet rs = ps.executeQuery()) {
            //开始遍历结果集
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private static PreparedStatement handlePrepareStatement(Connection conn, String sql, Object... obj) throws SQLException {
        String formatSql = StrUtil.format(sql, obj);
        AcTableProperties acTableProperties = SpringUtil.getBean(AcTableProperties.class);
        if(acTableProperties.getPrint()){
            log.info(formatSql);
        }
        return conn.prepareStatement(formatSql);
    }

    /**
     * @param conn
     * @param sql
     * @param obj
     * @return 查询返回List集合
     */
    public static Map<String, TableColumnInfo> getTableColumnInfoMap(Connection conn, String sql, Object... obj) throws SQLException {
        Map<String, TableColumnInfo> resultMap = new HashMap<>();
        try (PreparedStatement ps = handlePrepareStatement(conn, sql, obj);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TableColumnInfo tableColumnInfo = new TableColumnInfo();
                tableColumnInfo.setTableName(rs.getString("tableName"));
                String tableComment = rs.getString("tableComment");
                tableColumnInfo.setTableComment(StrUtil.isBlank(tableComment) ? "" : tableComment);
                tableColumnInfo.setColumnName(rs.getString("columnName"));
                String columnComment = rs.getString("columnComment");
                tableColumnInfo.setColumnComment(StrUtil.isBlank(columnComment) ? "" : columnComment);
                tableColumnInfo.setKey(rs.getBoolean("isKey"));
                tableColumnInfo.setTypeStr(rs.getString("typeStr"));
                tableColumnInfo.setLength(rs.getLong("length"));
                tableColumnInfo.setDecimalLength(rs.getInt("decimalLength"));
                tableColumnInfo.setNull(rs.getBoolean("isNull"));
                tableColumnInfo.setAutoIncrement(rs.getBoolean("isAutoIncrement"));
                tableColumnInfo.setDefaultValue(rs.getString("defaultValue"));
                resultMap.put(tableColumnInfo.getColumnName(), tableColumnInfo);
            }
            return resultMap;
        }
    }

    /**
     * @param conn
     * @param sql
     * @param obj
     * @return 查询返回List集合
     */
    public static List<ConstraintInfo> getConstraintInfoList(Connection conn, String sql, Object... obj) throws SQLException {
        if (StrUtil.isBlank(sql)) {
            return null;
        }
        //创建一个list集合对象来存储查询数据
        List<ConstraintInfo> list = new ArrayList<>();
        try (PreparedStatement ps = handlePrepareStatement(conn, sql, obj);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ConstraintInfo constraintInfo = new ConstraintInfo();
                constraintInfo.setConstraintName(rs.getString("constraintName"));
                constraintInfo.setConstraintColumnName(rs.getString("constraintColumnName"));
                constraintInfo.setConstraintFlag(rs.getInt("constraintFlag"));
                constraintInfo.setIndexSortStr(rs.getString("indexSortStr"));
                list.add(constraintInfo);
            }
            return list;
        }
    }

    /**
     * @param conn
     * @param sql
     * @param obj
     * @return 获取数据库中所有表
     */
    public static List<String> getTableNameList(Connection conn, String sql, Object... obj) throws SQLException {
        //创建一个list集合对象来存储查询数据
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = handlePrepareStatement(conn, sql, obj);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("name"));
            }
            return list;
        }
    }
}
