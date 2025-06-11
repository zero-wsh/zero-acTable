package io.gitee.zerowsh.actable.constant;

/**
 * 定义常量
 *
 * @author zero
 */
public interface AcTableConstants {
    /**
     * 当等于该值时，默认值为null
     */
    String DEFAULT_VALUE = "ZERO_AC_TABLE_DEFAULT_VALUE";

    String SINGLE_QUOTE = "'";
    String DOUBLE_QUOTES = "\"";
    long DEFAULT_STR = 255L;
    String CONVERT_STR = "->";
    /**
     * 当实体属性没标记@Column注解时有用，字段可以为空，默认长度255、默认浮点数0
     */
    boolean COLUMN_IS_NULL_DEF = true;
    long NUMBER_UNDEFINED = -1L;
    String DESC = " DESC";
    String ASC = " ASC";
    /**
     * 1 主键
     * 2 唯一键
     * 3 索引
     * 4 唯一索引
     * 5 默认值约束
     */
    int PK = 1;
    int UK = 2;
    int INDEX = 3;
    int UK_IDX = 4;
    int DEF = 5;

    String TRANSIENT = "transient";
    String STATIC = "static";
    /**
     * 主键前缀
     */
    String PK_ = "pk_";
    /**
     * 自增
     */
    String IDENTITY = " identity(1,1)";
    String MYSQL_IDENTITY = " AUTO_INCREMENT";
    String NULL = " NULL";
    String NOT_NULL = " NOT NULL";
    String DEFAULT = " DEFAULT {}";
    String LENGTH_DECIMAL = "({},{})";
    String LENGTH = "({})";
    String PRIMARY_KEY = " PRIMARY KEY ({}),";
    String UNIQUE_KEY = " UNIQUE KEY {} ({}),";
    String INDEX_KEY = " KEY {} ({}),";
    /**
     * 验证字符串
     */
    String COLUMN_LENGTH_VALID_STR = "表【{}】字段【{}】，【{}】类型长度【{}】存在问题，使用默认值【{}】！";
    String COLUMN_DECIMAL_LENGTH_VALID_STR = "表【{}】字段【{}】，【{}】精度长度【{}】 类型长度【{}】存在问题，使用默认值【{}】！";
    String COLUMN_DUPLICATE_VALID_STR = "表【{}】，【{}】字段名或@AcColumn注解属性值【name或value】重复！";

    /**
     * 历史表处理
     */
    String GET_HISTORY = "select file_md5 from tb_ac_history where file_name='{}' and exec_script='{}'";
    String INSERT_HISTORY = "insert into tb_ac_history(file_name,file_md5,exec_script,create_time) values('{}','{}','{}','{}')";
    String UPDATE_HISTORY = "update tb_ac_history set file_md5='{}',update_time='{}' where file_name='{}' and exec_script='{}'";
}
