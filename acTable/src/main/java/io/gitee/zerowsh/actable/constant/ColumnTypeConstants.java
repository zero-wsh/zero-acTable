package io.gitee.zerowsh.actable.constant;

import cn.hutool.core.lang.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 定义常量
 *
 * @author zero
 */
public interface ColumnTypeConstants {

    String DEFAULT_VALUE = AcTableConstants.DEFAULT_VALUE;
    String VARCHAR = "varchar";
    String TEXT = "text";
    String BIGINT = "bigint";
    String INT = "int";
    String BIT = "bit";
    String DATETIME = "datetime";
    String DATE = "date";
    String CHAR = "char";
    String FLOAT = "float";
    String DOUBLE = "double";
    String DECIMAL = "decimal";
    String TINYINT = "tinyint";
    String NUMERIC = "numeric";
    String SMALLINT = "smallint";
    /**
     * mysql才有的字段
     */
    String TIME = "time";
    String LONGBLOB = "longblob";
    String JSON = "json";
    String LONGTEXT = "longtext";
    String YEAR = "year";
    String INTEGER = "integer";
    /**
     * sqlserver才有的字段
     */
    String NVARCHAR = "nvarchar";
    String NVARCHAR_MAX = "nvarchar(max)";
    String VARCHAR_MAX = "varchar(max)";
    String XML = "xml";
    String DATETIME2 = "datetime2";
    String MONEY = "money";
    String VARBINARY = "varbinary";
    String VARBINARY_MAX = "varbinary(max)";
    String NCHAR = "nchar";
    /**
     * 达梦数据库才有的字段
     */
    String DATETIME_WITH_TIME_ZONE = "DATETIME WITH TIME ZONE";
    List<String> COMMON_COLUMN_TYPE_LIST = new ArrayList<String>() {{
        this.add(VARCHAR);
        this.add(TEXT);
        this.add(BIGINT);
        this.add(INT);
        this.add(BIT);
        this.add(DATE);
        this.add(CHAR);
        this.add(FLOAT);
        this.add(DOUBLE);
        this.add(DECIMAL);
        this.add(TINYINT);
        this.add(NUMERIC);
        this.add(SMALLINT);
    }};

    /**
     * 达梦数据库字段是否包含这个列类型
     */
    static Pair<String, Long> mysqlContains(String columnType) {
        List<String> list = new ArrayList<String>(COMMON_COLUMN_TYPE_LIST) {{
            this.add(TIME);
            this.add(LONGBLOB);
            this.add(JSON);
            this.add(LONGTEXT);
            this.add(YEAR);
            this.add(INTEGER);
            this.add(DATETIME);
        }};

        if (list.contains(columnType)) {
            return Pair.of(columnType, null);
        }
        return Pair.of(ColumnTypeConstants.VARCHAR, AcTableConstants.DEFAULT_STR);
    }

    /**
     * 达梦数据库字段是否包含这个列类型
     *
     * @return
     */

    static Pair<String, Long> sqlServerContains(String columnType) {
        List<String> list = new ArrayList<String>(COMMON_COLUMN_TYPE_LIST) {{
            this.add(NVARCHAR);
            this.add(NVARCHAR_MAX);
            this.add(VARCHAR_MAX);
            this.add(XML);
            this.add(DATETIME2);
            this.add(MONEY);
            this.add(VARBINARY);
            this.add(VARBINARY_MAX);
            this.add(NCHAR);
        }};
        if (list.contains(columnType)) {
            return Pair.of(columnType, null);
        }
        return Pair.of(ColumnTypeConstants.NVARCHAR, AcTableConstants.DEFAULT_STR);
    }

    /**
     * 达梦数据库字段是否包含这个列类型，如果不包含返回默认的
     *
     * @return
     */

    static Pair<String, Long> dmContains(String columnType) {
        List<String> list = new ArrayList<String>(COMMON_COLUMN_TYPE_LIST) {{
            this.add(DATETIME_WITH_TIME_ZONE);
            this.add(DATETIME);
        }};
        if (list.contains(columnType)) {
            return Pair.of(columnType, null);
        }
        return Pair.of(ColumnTypeConstants.VARCHAR, AcTableConstants.DEFAULT_STR);
    }

}
