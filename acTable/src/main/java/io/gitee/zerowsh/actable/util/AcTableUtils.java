package io.gitee.zerowsh.actable.util;

import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.constant.AcTableConstants;
import io.gitee.zerowsh.actable.service.DatabaseService;
import io.gitee.zerowsh.actable.service.impl.MysqlImpl;
import io.gitee.zerowsh.actable.service.impl.SqlServerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;
import java.util.Set;


/**
 * 所有数据库共用工具类
 *
 * @author zero
 */
@Slf4j
public class AcTableUtils {
    /**
     * 处理字符串长度
     *
     * @param length
     * @return
     */
    public static long handleStrLength(int length) {
        return length < 0 ? 255 : length;
    }

    /**
     * 处理时间长度
     *
     * @param length
     * @return
     */
    public static int handleDateLength(int length) {
        return length > 7 || length < 0 ? 0 : length;
    }

    public static DatabaseService getDatabaseService(String databaseType) {
        switch (databaseType) {
            case AcTableConstants.MYSQL:
                return new MysqlImpl();
            case AcTableConstants.SQL_SERVER:
                return new SqlServerImpl();
            default:
                throw new RuntimeException(StrUtil.format("数据库类型不支持 databaseType={}", databaseType));
        }
    }

    public static Set<BeanDefinition> scanPackageByAnnotation(String basePackage, Class<? extends Annotation> annotationClass) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // 添加包含过滤条件，只扫描带有特定注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(annotationClass));
        // 执行扫描并获取结果
        return scanner.findCandidateComponents(basePackage);
    }
}
