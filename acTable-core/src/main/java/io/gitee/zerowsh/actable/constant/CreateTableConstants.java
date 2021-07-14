package io.gitee.zerowsh.actable.constant;

/**
 * 定义常量
 *
 * @author zero
 */
public interface CreateTableConstants {
    /**
     * 当等于该值时，默认值为null
     */
    String DEFAULT_VALUE = "default_value";

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
    String TRANSIENT = "transient";
    String STATIC = "static";
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
    String IDENTITY = " identity(1,1)";
    String NULL = " NULL";
    String NOT_NULL = " NOT NULL";
    String DEFAULT = " DEFAULT ";
    String COMMENT = " COMMENT '{}'";
    String PRIMARY_KEY = " PRIMARY KEY (`{}`) ";
    String UNIQUE_KEY = " UNIQUE KEY `{}` (`{}`) ";
    String INDEX_KEY = " KEY `{}` (`{}`) ";
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
    String ADD_COLUMN = "ALTER TABLE [{}] ADD [{}] {}";
    String UPDATE_COLUMN = "ALTER TABLE [{}] ALTER COLUMN [{}] {}";
    String ADD_COLUMN_COMMENT = "EXEC sp_addextendedproperty 'MS_Description', N'{}','SCHEMA', N'dbo','TABLE', N'{}','COLUMN', N'{}'";
    String UPDATE_COLUMN_COMMENT = "EXEC sp_updateextendedproperty 'MS_Description', N'{}','SCHEMA', N'dbo','TABLE', N'{}','COLUMN', N'{}'";
    String DROP_COLUMN_COMMENT = "EXEC sys.sp_dropextendedproperty N'MS_Description',N'SCHEMA', N'dbo', N'TABLE', N'{}', N'COLUMN', N'{}'";
    String CREATE_INDEX = "CREATE NONCLUSTERED INDEX [{}] ON [{}] ({})";
    String CREATE_UNIQUE = "ALTER TABLE [{}] add constraint [{}] unique ({})";
    String DROP_COLUMN = "ALTER TABLE [{}] DROP COLUMN [{}]";
    String DROP_CONSTRAINT = "ALTER TABLE [{}] DROP CONSTRAINT [{}]";
    String DROP_INDEX = "DROP INDEX [{}] ON [{}]";
    String CREATE_PRIMARY_KEY = "ALTER TABLE [{}] ADD CONSTRAINT [{}] PRIMARY KEY CLUSTERED ({})";
    String ADD_DEFAULT = "ALTER TABLE [{}] ADD DEFAULT {} FOR [{}]";
    /**
     * 关键字处理
     */
    String SQL_SERVER_KEYWORD_HANDLE = "[{}]";
    String MYSQL_KEYWORD_HANDLE = "`{}`";
    String MYSQL_IDENTITY = " AUTO_INCREMENT";
    String MYSQL_COMMENT = " COMMENT='{}'";
}
