package io.gitee.zerowsh.actable.demo.config;//package io.gitee.zerowsh.common.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 实现创建数据库
 *
 * @author zero
 */
public class CreateDatabaseProcessor implements EnvironmentPostProcessor {
    private static final String MYSQL_CREATE_DATABASE = "create database if not exists `%s` default character set utf8mb4";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (PropertySource<?> ps : environment.getPropertySources()) {

            if (ps instanceof OriginTrackedMapPropertySource) {
                OriginTrackedMapPropertySource source = (OriginTrackedMapPropertySource) ps;
                String url = String.valueOf(source.getProperty("spring.datasource.url"));
                String username = String.valueOf(source.getProperty("spring.datasource.username"));
                String password = String.valueOf(source.getProperty("spring.datasource.password"));
                if (StrUtil.isNotBlank(url) && StrUtil.isNotBlank(username) && StrUtil.isNotBlank(password)) {
                    createDatabase(url, username, password);
                    break;
                }
            }
        }
    }

    private void createDatabase(String url, String username, String password) {
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
