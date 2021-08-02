package io.gitee.zerowsh.actable.annotation;

import java.lang.annotation.*;


/**
 * 忽略表的注解
 *
 * @author zero
 */
//表示注解加在接口、类、枚举等
@Target(ElementType.TYPE)
//VM将在运行期也保留注释，因此可以通过反射机制读取注解的信息
@Retention(RetentionPolicy.RUNTIME)
//将此注解包含在javadoc中
@Documented
public @interface IgnoreTable {
}
