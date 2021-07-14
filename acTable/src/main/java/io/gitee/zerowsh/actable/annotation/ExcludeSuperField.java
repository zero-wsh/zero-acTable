package io.gitee.zerowsh.actable.annotation;

import java.lang.annotation.*;


/**
 * 用于排除父类的字段，只会排除父类的字段，当前类字段排除使用@Column注解
 * 继承com.baomidou.mybatisplus.extension.activerecord.Model时，多serialVersionUID和entityClass两个字段
 * 不确定是否还有其他情况，提供一个注解来维护
 *
 * @author zero
 */
//表示注解加在接口、类、枚举等
@Target(ElementType.TYPE)
//VM将在运行期也保留注释，因此可以通过反射机制读取注解的信息
@Retention(RetentionPolicy.RUNTIME)
//将此注解包含在javadoc中
@Documented
public @interface ExcludeSuperField {
    String[] value() default {"serialVersionUID", "entityClass"};
}
