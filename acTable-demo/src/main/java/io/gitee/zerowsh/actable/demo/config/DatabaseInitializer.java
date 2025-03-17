package io.gitee.zerowsh.actable.demo.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 数据库初始化
 *
 * @author zero
 */
@Component
public class DatabaseInitializer implements BeanPostProcessor {
    private static final String MYSQL_CREATE_DATABASE = "create database if not exists `%s` default character set utf8mb4";

    @Resource
    private Environment environment;


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            // 在 DataSource 初始化之前执行
            initializeDatabase();
        }
        return bean;
    }

    private void initializeDatabase() {
        String url = String.valueOf(environment.getProperty("spring.datasource.url"));
        String username = String.valueOf(environment.getProperty("spring.datasource.username"));
        String password = String.valueOf(environment.getProperty("spring.datasource.password"));
        if (StrUtil.isNotBlank(url) && StrUtil.isNotBlank(username) && StrUtil.isNotBlank(password)) {
            String url02;
            String databaseName;
            if (url.contains(StringPool.QUESTION_MARK)) {
                String url01 = url.substring(0, url.indexOf(StringPool.QUESTION_MARK));
                url02 = url01.substring(0, url01.lastIndexOf(StringPool.SLASH)) + url.substring(url.indexOf(StringPool.QUESTION_MARK));
                databaseName = url01.substring(url01.lastIndexOf(StringPool.SLASH) + 1);
            } else {
                url02 = url.substring(0, url.lastIndexOf(StringPool.SLASH));
                databaseName = url.substring(url.lastIndexOf(StringPool.SLASH) + 1);
            }
            try (
                    Connection connection = DriverManager.getConnection(url02, username, password);
                    Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format(MYSQL_CREATE_DATABASE, databaseName));
            } catch (Exception e) {
                throw new RuntimeException("创建MYSQL数据库失败", e);
            }
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}