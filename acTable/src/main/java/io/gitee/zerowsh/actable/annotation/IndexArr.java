package io.gitee.zerowsh.actable.annotation;

import java.lang.annotation.*;


/**
 * @author zero
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IndexArr {
    Index[] value();
}

