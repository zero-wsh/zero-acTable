package io.gitee.zerowsh.actable.demo.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

/**
 * 数据库初始化
 *
 * @author zero
 */
@Component
public class DatabaseInitializer implements BeanPostProcessor {
    private static final String MYSQL_CREATE_DATABASE = "create database if not exists `%s` default character set utf8mb4";
    private static final String SQL_SERVER_CREATE_DATABASE = "IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = '%s')\n" +
            "BEGIN\n" +
            "    CREATE DATABASE [%s];\n" +
            "END";
    //达梦空间
    private static final String DM_CREATE_TABLESPACES = "BEGIN\n" +
            "    IF NOT EXISTS (SELECT 1 FROM DBA_TABLESPACES WHERE TABLESPACE_NAME = '%s') THEN\n" +
            "        EXECUTE IMMEDIATE 'CREATE TABLESPACE %s DATAFILE ''/dmdata/DAMENG/%s.dbf'' SIZE 1024';\n" +
            "    END IF;\n" +
            "END;";
    //达梦用户
    private static final String DM_CREATE_USER = "BEGIN\n" +
            "    IF NOT EXISTS (SELECT 1 FROM DBA_USERS WHERE USERNAME = '%s') THEN\n" +
            "        EXECUTE IMMEDIATE 'CREATE USER %s IDENTIFIED BY \"%s\" DEFAULT TABLESPACE %s';\n" +
            "    END IF;\n" +
            "END;";
    //用户授权
    private static final String DM_AUTHORIZE = "BEGIN\n" +
            "    IF EXISTS (SELECT 1 FROM DBA_USERS WHERE USERNAME = '%s') THEN\n" +
            "        EXECUTE IMMEDIATE 'GRANT \"PUBLIC\",\"RESOURCE\",\"SOI\" TO  %s';\n" +
            "    END IF;\n" +
            "END;";

    private static final String QUESTION_MARK = "?";
    private static final String SLASH = "/";

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
        String active = String.valueOf(environment.getProperty("spring.profiles.active"));
        if (active.equalsIgnoreCase("mysql")) {
            mysql();
        } else if (active.equalsIgnoreCase("dm")) {
            dm();
        } else if (active.equalsIgnoreCase("sql_server")) {
            sqlServer();
        }

    }

    private void mysql() {
        String url = String.valueOf(environment.getProperty("spring.datasource.url"));
        String username = String.valueOf(environment.getProperty("spring.datasource.username"));
        String password = String.valueOf(environment.getProperty("spring.datasource.password"));
        if (StrUtil.isNotBlank(url) && StrUtil.isNotBlank(username) && StrUtil.isNotBlank(password)) {
            String url02;
            String databaseName;
            if (url.contains(QUESTION_MARK)) {
                String url01 = url.substring(0, url.indexOf(QUESTION_MARK));
                url02 = url01.substring(0, url01.lastIndexOf(SLASH)) + url.substring(url.indexOf(QUESTION_MARK));
                databaseName = url01.substring(url01.lastIndexOf(SLASH) + 1);
            } else {
                url02 = url.substring(0, url.lastIndexOf(SLASH));
                databaseName = url.substring(url.lastIndexOf(SLASH) + 1);
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

    private void sqlServer() {
        String url = String.valueOf(environment.getProperty("spring.datasource.url"));
        String username = String.valueOf(environment.getProperty("spring.datasource.username"));
        String password = String.valueOf(environment.getProperty("spring.datasource.password"));
        if (StrUtil.isNotBlank(url) && StrUtil.isNotBlank(username) && StrUtil.isNotBlank(password)) {
            List<String> split = StrUtil.split(url, ';', 2);
            String url02 = split.get(0);
            String databaseName = split.get(1).replaceAll("databaseName=", "");
            try (
                    Connection connection = DriverManager.getConnection(url02, username, password);
                    Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format(SQL_SERVER_CREATE_DATABASE, databaseName, databaseName));
            } catch (Exception e) {
                throw new RuntimeException("创建SQL_SERVER数据库失败", e);
            }
        }
    }

    private void dm() {
        String url = String.valueOf(environment.getProperty("spring.datasource.url"));
        String username = String.valueOf(environment.getProperty("spring.datasource.username"));
        String password = String.valueOf(environment.getProperty("spring.datasource.password"));
        if (StrUtil.isNotBlank(url) && StrUtil.isNotBlank(username) && StrUtil.isNotBlank(password)) {
            String url02 = url.substring(0, url.lastIndexOf(SLASH));
            String databaseName = url.substring(url.lastIndexOf(SLASH) + 1);
            try (
                    Connection connection = DriverManager.getConnection(url02, username, password);
                    Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format(DM_CREATE_TABLESPACES, databaseName, databaseName, databaseName));
                statement.executeUpdate(String.format(DM_CREATE_USER, databaseName, databaseName, databaseName + "ZERO001", databaseName));
                statement.executeUpdate(String.format(DM_AUTHORIZE, databaseName, databaseName));
            } catch (Exception e) {
                throw new RuntimeException("创建达梦数据库失败", e);
            }
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}