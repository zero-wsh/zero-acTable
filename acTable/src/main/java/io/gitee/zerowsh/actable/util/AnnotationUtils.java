package io.gitee.zerowsh.actable.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.Supplier;

public class AnnotationUtils {

    /**
     * 是否存在
     */

    public static <T extends Annotation> boolean isAnnotationPresent(Supplier<Class<T>> annotationSupplier) {
        try {
            annotationSupplier.get();
            return true;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    /**
     * 安全获取注解（仅当注解类存在时才返回）
     */
    public static <T extends Annotation> T getAnnotationSafe(Field field, Supplier<Class<T>> annotationSupplier) {
        try {
            Class<T> annotationClass = annotationSupplier.get();
            return field.getAnnotation(annotationClass);
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }

    /**
     * 安全获取注解（仅当注解类存在时才返回）
     */
    public static <T extends Annotation> T getAnnotationClassSafe(Class<?> cls, Supplier<Class<T>> annotationSupplier) {
        try {
            Class<T> annotationClass = annotationSupplier.get();
            return cls.getAnnotation(annotationClass);
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }
}