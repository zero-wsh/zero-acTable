package io.gitee.zerowsh.actable.util;

import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zero
 */
public class JdbcUtil {
    private static final Logger log = LoggerFactory.getLogger(JdbcUtil.class);

    public static void executeSql(Connection conn, String sql, Object... obj) throws SQLException {
        try (PreparedStatement ps = handlePrepareStatement(conn, sql, obj)) {
            ps.execute();
        }
    }

    /**
     * 是否存在数据
     *
     * @param conn
     * @param sql
     * @param obj
     * @return
     */
    public static boolean isExist(Connection conn, String sql, Object... obj) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = handlePrepareStatement(conn, sql, obj);
            rs = ps.executeQuery();
            //开始遍历结果集
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } finally {
            IoUtil.close(ps, rs);
        }
        return false;
    }

    private static PreparedStatement handlePrepareStatement(Connection conn, String sql, Object... obj) throws SQLException {
        String formatSql = StrUtil.format(sql, obj);
        PreparedStatement ps = conn.prepareStatement(formatSql);
        log.info(formatSql);
        return ps;
    }

    /**
     * 查询返回List集合
     *
     * @param conn
     * @param sql
     * @param obj
     * @return
     */
    public static List<TableColumnInfo> getTableColumnInfoList(Connection conn, String sql, Object... obj) throws SQLException {
        List<TableColumnInfo> list = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = handlePrepareStatement(conn, sql, obj);
            rs = ps.executeQuery();
            while (rs.next()) {
                TableColumnInfo tableColumnInfo = new TableColumnInfo();
                tableColumnInfo.setTableName(rs.getString("tableName"));
                tableColumnInfo.setTableComment(rs.getString("tableComment"));
                tableColumnInfo.setColumnName(rs.getString("columnName"));
                tableColumnInfo.setColumnComment(rs.getString("columnComment"));
                tableColumnInfo.setKey(rs.getBoolean("isKey"));
                tableColumnInfo.setTypeStr(rs.getString("typeStr"));
                tableColumnInfo.setLength(rs.getInt("length"));
                tableColumnInfo.setDecimalLength(rs.getInt("decimalLength"));
                tableColumnInfo.setNull(rs.getBoolean("isNull"));
                tableColumnInfo.setAutoIncrement(rs.getBoolean("isAutoIncrement"));
                tableColumnInfo.setDefaultValue(rs.getString("defaultValue"));
                list.add(tableColumnInfo);
            }
            return list;
        } finally {
            IoUtil.close(ps, rs);
        }
    }

    /**
     * 查询返回List集合
     *
     * @param conn
     * @param sql
     * @param obj
     * @return
     */
    public static List<ConstraintInfo> getConstraintInfoList(Connection conn, String sql, Object... obj) throws SQLException {
        //创建一个list集合对象来存储查询数据
        List<ConstraintInfo> list = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = handlePrepareStatement(conn, sql, obj);
            rs = ps.executeQuery();
            while (rs.next()) {
                ConstraintInfo constraintInfo = new ConstraintInfo();
                constraintInfo.setConstraintName(rs.getString("constraintName"));
                constraintInfo.setConstraintColumnName(rs.getString("constraintColumnName"));
                constraintInfo.setConstraintFlag(rs.getInt("constraintFlag"));
                list.add(constraintInfo);
            }
            return list;
        } finally {
            IoUtil.close(ps, rs);
        }
    }
}