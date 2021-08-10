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
//        //循环参数，如果没有就不走这里
//        for (int i = 1; i <= obj.length; i++) {
//            //注意：数组下标从0开始，预处理参数设置从1开始
//            ps.setObject(i, obj[i - 1]);
//            log.info("param key=[{}] value=[{}]", i, obj[i - 1]);
//        }
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

    /**
     * 查询返回List集合
     *
     * @param conn
     * @param sql
     * @param obj
     * @return
     */
    public static String getAcTableInfo(Connection conn, String sql, Object... obj) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = handlePrepareStatement(conn, sql, obj);
            rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getString("table_info_md5");
            }
        } finally {
            IoUtil.close(ps, rs);
        }
        return null;
    }


//    /**
//     * 查询返回List集合
//     *
//     * @param conn
//     * @param cls
//     * @param sql
//     * @param obj
//     * @param <T>
//     * @return
//     */
//    public static <T> List<T> getList(Connection conn, Class<T> cls, String sql, Object... obj) throws SQLException, IllegalAccessException, InstantiationException {
//        //创建一个list集合对象来存储查询数据
//        List<T> list = new ArrayList<>();
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//            //获取预处理对象
//            ps = handlePrepareStatement(conn, sql, obj);
//            rs = ps.executeQuery();
//            /*
//             * 4.遍历结果集
//             * 遍历之前准备：因为封装不知道未来会查询多少列，所以我们需要指定有多少列
//             * 获取ResultSet对象的列编号、类型和属性
//             */
//            ResultSetMetaData date = rs.getMetaData();
//            //获取列数
//            int column = date.getColumnCount();
//            //获取本类所有的属性
//            Field[] fields = cls.getDeclaredFields();
//            //开始遍历结果集
//            while (rs.next()) {
//                //创建类类型实例
//                T t = cls.newInstance();
//                for (int i = 1; i <= column; i++) {
//                    //每一列的值
//                    Object value = rs.getObject(i);
//                    /*
//                     *String columnName = date.getColumnName(i);//获取每一列名称
//                     * 关于获取每一列名称，如果列取了别名的话，则不能用上面的方法取列的名称
//                     * 用下面的方法获取每一列名称（别名）
//                     */
//                    String columnName = date.getColumnLabel(i);
//                    //遍历所有属性对象
//                    for (Field field : fields) {
//                        //获取属性名
//                        String name = field.getName();
//                        if (name.equals(columnName)) {
//                            setFieldValueByFieldName(t, name, value);
//                            break;//增加效率，避免不必要的循环
//                        }
//                    }
//                }
//                list.add(t);
//            }
//            return list;
//        } finally {
//            IoUtil.close(ps, rs);
//        }
//    }
//    /**
//     * 递归获取字段信息
//     *
//     * @param c
//     * @param fields
//     * @param fieldName
//     * @return
//     */
//    private static Field getFieldInfo(Class<?> c, Field[] fields, Object fieldName) {
//        for (Field field : fields) {
//            if (Objects.equals(fieldName, field.getName())) {
//                return field;
//            }
//        }
//        Class<?> superclass = c.getSuperclass();
//        if (Objects.isNull(superclass)) {
//            return null;
//        }
//        Field[] declaredFields = superclass.getDeclaredFields();
//        return getFieldInfo(superclass, declaredFields, fieldName);
//    }
//
//    /**
//     * 根据属性名设置属性值
//     *
//     * @param fieldName
//     * @param object
//     */
//    public static void setFieldValueByFieldName(Object object, String fieldName, Object value) {
//        try {
//            // 获取obj类的字节文件对象
//            Class<?> c = object.getClass();
//            Field field = getFieldInfo(c, c.getDeclaredFields(), fieldName);
//            if (!Objects.isNull(field)) {
//                // 取消语言访问检查
//                field.setAccessible(true);
//                // 给变量赋值
//                field.set(object, value);
//            }
//        } catch (Exception e) {
//            log.warn("根据属性名设置属性值 {} 失败", fieldName);
//        }
//    }

}