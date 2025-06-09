package io.gitee.zerowsh.actable.annotation;

import io.gitee.zerowsh.actable.constant.AcTableConstants;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;

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
public @interface AcColumn {

    /**
     * 建表时是否排除
     */
    boolean exclude() default false;

    /**
     * 建表时字段顺序，只有建表时才会生效，越小排到越前面
     */
    int order() default 0;

    /**
     * 字段名
     */
    String value() default "";


    /**
     * 以前字段名
     * 结合value属性使用，当设置属性value和oldName不同时，代表value是现在要改成的字段名，oldName是以前的字段名
     */
    String oldName() default "";


    /**
     * 字段类型：不填默认使用属性的数据类型进行转换
     */
    String type() default ColumnTypeConstants.DEFAULT_VALUE;


    /**
     * 是否限制字段类型为支持的类型，如果不限制可自定义类型值;
     * 自定义类型值length和decimalLength将不会生效，将直接使用type的值作为数据库字段类型
     * 开启后将不会检验type属性的有效性
     * 例如：
     * datetime(5)
     * decimal(10,2)
     */
    boolean typeLimit() default true;


    /**
     * 字段长度
     */
    long length() default AcTableConstants.NUMBER_UNDEFINED;

    /**
     * 小数位数
     */
    long decimalLength() default AcTableConstants.NUMBER_UNDEFINED;

    /**
     * 是否为可以为null，true是可以，false是不可以，默认为true
     */
    boolean isNull() default AcTableConstants.COLUMN_IS_NULL_DEF;

    /**
     * 是否是主键
     */
    boolean isKey() default false;

    /**
     * 是否自动递增
     * mysql只有主键才能设置自增，两个是绑定关系
     */
    boolean isAutoIncrement() default false;

    /**
     * 默认值
     * 请自行判定默认值是否是字符串
     */
    String defaultValue() default AcTableConstants.DEFAULT_VALUE;

    /**
     * 字段备注
     */
    String comment() default AcTableConstants.DEFAULT_VALUE;
}
