package io.gitee.zerowsh.actable.constant;

import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 定义常量
 *
 * @author zero
 */
public interface CreateTableConstants {
    /**
     * 当等于改值时，默认值为null
     */
    String DEFAULT_VALUE = "default_value";
    /**
     * 数据库类型转java类型
     */
    Map<String, ColumnTypeEnums> JAVA_TURN_DATABASE_MAP = new HashMap<String, ColumnTypeEnums>() {{
        put("java.lang.String", ColumnTypeEnums.NVARCHAR);
        put("java.lang.Long", ColumnTypeEnums.BIGINT);
        put("long", ColumnTypeEnums.BIGINT);
        put("java.lang.Integer", ColumnTypeEnums.INT);
        put("int", ColumnTypeEnums.INT);
        put("java.lang.Boolean", ColumnTypeEnums.BIT);
        put("java.lang.boolean", ColumnTypeEnums.BIT);
        put("java.util.Date", ColumnTypeEnums.DATETIME2);
        put("java.sql.Timestamp", ColumnTypeEnums.DATETIME2);
        put("java.math.BigDecimal", ColumnTypeEnums.NUMERIC);
        put("java.lang.Double", ColumnTypeEnums.NUMERIC);
        put("double", ColumnTypeEnums.NUMERIC);
        put("java.lang.Float", ColumnTypeEnums.FLOAT);
        put("float", ColumnTypeEnums.FLOAT);
        put("char", ColumnTypeEnums.NCHAR);
    }};

    static ColumnTypeEnums getJavaTurnDatabaseValue(String key) {
        ColumnTypeEnums columnTypeEnums = JAVA_TURN_DATABASE_MAP.get(key);
        return Objects.isNull(columnTypeEnums) ? ColumnTypeEnums.NVARCHAR : columnTypeEnums;
    }

    /**
     * 定义字段默认值，当实体属性没标记@Column注解时有用
     */
    boolean COLUMN_IS_NULL_DEF = true;
    int COLUMN_LENGTH_DEF = 255;
    int COLUMN_DECIMAL_LENGTH_DEF = 0;
    int PK = 1;
    int UK = 2;
    int INDEX = 3;

    /**
     * 删除相关约束sql（主键 唯一键 索引 默认值）
     */
    String DEL_PK_C_SQL = "del_pk_sql";
    String DEL_UK_C_SQL = "del_uk_sql";
    String DEL_INDEX_C_SQL = "del_index_sql";
    String DEL_DF_C_SQL = "del_df_sql";
    /**
     * 唯一键前缀
     */
    String UK_ = "uk_";
    /**
     * 索引前缀
     */
    String IDX_ = "idx_";
    /**
     * 主键前缀
     */
    String PK_ = "pk_";
    /**
     * 自增
     */
    String IDENTITY = "identity(1,1)";
    String NULL = "NULL";
    String NOT_NULL = "NOT NULL";
    String DEFAULT = "DEFAULT";
    /**
     * 验证字符串
     */
    String COLUMN_LENGTH_VALID_STR = "表 [{}] 字段 [{}] {}类型长度 [{}] 存在问题，使用默认值 [{}]";
    String COLUMN_DUPLICATE_VALID_STR = "[{}] 字段名或@Column name重复";

    /**
     * 数据库操作部分sql
     */
    String CREATE_TABLE = "CREATE TABLE {} ({})";
    String ADD_TABLE_COMMENT = "EXEC sp_addextendedproperty 'MS_Description', N'{}','SCHEMA', N'dbo','TABLE', N'{}'";
    String UPDATE_TABLE_COMMENT = "EXEC sp_updateextendedproperty 'MS_Description', N'{}','SCHEMA', N'dbo','TABLE', N'{}'";
    String DROP_TABLE_COMMENT = "EXEC sys.sp_dropextendedproperty 'MS_Description',N'SCHEMA', N'dbo', N'TABLE', N'{}'";
    //ALTER TABLE [dbo].[t_zero] ADD [dd] varchar(255) DEFAULT 11 NULL
    String ADD_COLUMN = "ALTER TABLE [{}] ADD [{}] {}";
    String UPDATE_COLUMN = "ALTER TABLE [{}] ALTER COLUMN [{}] {}";
    String ADD_COLUMN_COMMENT = "EXEC sp_addextendedproperty 'MS_Description', N'{}','SCHEMA', N'dbo','TABLE', N'{}','COLUMN', N'{}'";
    String UPDATE_COLUMN_COMMENT = "EXEC sp_updateextendedproperty 'MS_Description', N'{}','SCHEMA', N'dbo','TABLE', N'{}','COLUMN', N'{}'";
    String DROP_COLUMN_COMMENT = "EXEC sys.sp_dropextendedproperty N'MS_Description',N'SCHEMA', N'dbo', N'TABLE', N'{}', N'COLUMN', N'{}'";
    String CREATE_INDEX = "CREATE NONCLUSTERED INDEX [{}] ON [{}] ({})";
    String CREATE_UNIQUE = "ALTER TABLE [{}] add constraint [{}] unique ({})";
    String DROP_COLUMN = "ALTER TABLE [{}] DROP COLUMN [{}]";
    //删除约束（包含唯一键和主键约束）
    String DROP_CONSTRAINT = "ALTER TABLE [{}] DROP CONSTRAINT [{}]";
    String DROP_INDEX = "DROP INDEX [{}] ON [{}]";
    String CREATE_PRIMARY_KEY = "ALTER TABLE [{}] ADD CONSTRAINT [{}] PRIMARY KEY CLUSTERED ({})";
    String ADD_DEFAULT = "ALTER TABLE [{}] ADD DEFAULT {} FOR [{}]";
}
