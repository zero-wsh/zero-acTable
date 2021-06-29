package io.gitee.zerowsh.actable.annotation;

import io.gitee.zerowsh.actable.constant.CreateTableConstants;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;

import java.lang.annotation.*;

/**
 * 标记实体类字段的注解
 *
 * @author zero
 */
// 该注解用于方法声明
@Target(ElementType.FIELD)
// VM将在运行期也保留注释，因此可以通过反射机制读取注解的信息
@Retention(RetentionPolicy.RUNTIME)
// 将此注解包含在javadoc中
@Documented
public @interface Column {

    /**
     * 建表时是否排除,默认否
     *
     * @return boolean
     */
    boolean exclude() default false;

    /**
     * 字段名
     * 1.当@Column name和mybatis plus @TableField同时存在时，优先使用@TableField的value
     * 2.两个注解都不存在或者设置的值都无效时，使用字段配合turn进行转换
     */
    String name() default "";


    /**
     * 字段类型：不填默认使用属性的数据类型进行转换，转换失败的字段不会添加
     * 仅支持zero.mybatisplus.dynamictable.emnus.ColumnTypeEnums中的枚举数据类型
     * 不填默认转换类：zero.mybatisplus.dynamictable.util.JavaToDatabaseColumnType
     *
     * @return 字段类型
     */
    ColumnTypeEnums type() default ColumnTypeEnums.DEFAULT;

    /**
     * 字段长度，默认是255
     *
     * @return 默认字段长度，默认是255
     */
    int length() default CreateTableConstants.COLUMN_LENGTH_DEF;

    /**
     * 小数点长度，默认是0
     *
     * @return 小数点长度，默认是0
     */
    int decimalLength() default CreateTableConstants.COLUMN_DECIMAL_LENGTH_DEF;

    /**
     * 是否为可以为null，true是可以，false是不可以，默认为true
     *
     * @return 是否为可以为null，true是可以，false是不可以，默认为true
     */
    boolean isNull() default CreateTableConstants.COLUMN_IS_NULL_DEF;

    /**
     * 是否是主键，默认false
     *
     * @return 是否是主键，默认false
     */
    boolean isKey() default false;

    /**
     * 是否自动递增，默认false
     *
     * @return 是否自动递增，默认false 只有主键才能使用
     */
    boolean isAutoIncrement() default false;

    /**
     * 默认值，默认为null
     *
     * @return 默认值
     */
    String defaultValue() default CreateTableConstants.DEFAULT_VALUE;

    /**
     * 字段备注
     *
     * @return 默认值，默认为空
     */
    String comment() default CreateTableConstants.DEFAULT_VALUE;
}
