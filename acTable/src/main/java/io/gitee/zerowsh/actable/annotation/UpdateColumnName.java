package io.gitee.zerowsh.actable.annotation;

import java.lang.annotation.*;


/**
 * 修改字段名称
 * 设置当前类中需要调整字段名称的字段
 * 比如oldName改成name,设置属性value={"oldName->name"}
 * <p>
 * <p>
 * 如果对应的属性注解AcColumn设置了oldName,集合中对应的字段就不会生效
 * 比如：@UpdateColumnName("oldName->name"),@AcColumn(oldName="name",value="name")
 * 这时候就不会执行将oldName字段改为name字段，以AcColumn注解优先
 *
 * @author zero
 */
//表示注解加在接口、类、枚举等
@Target(ElementType.TYPE)
//VM将在运行期也保留注释，因此可以通过反射机制读取注解的信息
@Retention(RetentionPolicy.RUNTIME)
//将此注解包含在javadoc中
@Documented
public @interface UpdateColumnName {
    String[] value();
}
