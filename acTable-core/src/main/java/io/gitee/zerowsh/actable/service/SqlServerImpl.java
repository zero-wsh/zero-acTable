package io.gitee.zerowsh.actable.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import io.gitee.zerowsh.actable.config.CreateTableConfig;
import io.gitee.zerowsh.actable.dao.SqlServerMapper;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.DatabaseTypeEnums;
import io.gitee.zerowsh.actable.util.SqlServerAcTableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * sql_server实现
 *
 * @author zero
 */
@Component
@Slf4j
public class SqlServerImpl {

    @Resource
    private SqlServerMapper sqlServerMapper;

    @Transactional(rollbackFor = Exception.class)
    public void acTable(CreateTableConfig createTableConfig, List<TableInfo> tableInfoList) {
        DatabaseTypeEnums databaseType = createTableConfig.getDatabaseType();
        log.info(StrUtil.format("执行 [{}] 自动建表。。。", databaseType));
        for (TableInfo tableInfo : tableInfoList) {
            String tableName = tableInfo.getName();
            if (sqlServerMapper.isExistTable(tableName) == 0) {
                //不存在--建表
                List<String> createTableSql = SqlServerAcTableUtils.getCreateTableSql(tableInfo);
                for (String sql : createTableSql) {
                    sqlServerMapper.executeSql(sql);
                    log.info(sql);
                }
            } else {
                /*
                 * 存在--改表
                 *
                 * 1.获取表结构
                 * 2.获取约束（唯一键、主键、索引）
                 * 3.获取默认值约束
                 * 4.修改表
                 * 5.顺序执行sql
                 */
                List<TableColumnInfo> tableColumnInfoList = sqlServerMapper.getTableStructure(tableName);
                List<ConstraintInfo> constraintInfoList = sqlServerMapper.getConstraintInfo(tableName);
                List<ConstraintInfo> defaultInfoList = sqlServerMapper.getDefaultInfo(tableName);
                List<String> updateTableSql = SqlServerAcTableUtils.getUpdateTableSql(tableInfo,
                        tableColumnInfoList,
                        constraintInfoList,
                        defaultInfoList,
                        createTableConfig.getModel());
                for (String sql : updateTableSql) {
                    sqlServerMapper.executeSql(sql);
                    log.info(sql);
                }
            }
        }
        log.info(StrUtil.format("执行 [{}] 自动建表完成！！！", databaseType));

        String script = createTableConfig.getScript();
        if (StrUtil.isBlank(script)) {
            log.info("没有初始化数据文件。。。");
            return;
        }
        log.info("执行 [{}] 初始化数据。。。", databaseType);

        // TODO 加载当前项目classpath下META-INF/folder及其子文件夹中的所有文件
        org.springframework.core.io.Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(ResourceUtils.CLASSPATH_URL_PREFIX + script);
        } catch (IOException e) {
            log.info("执行 [{}] 初始化数据失败！！！没找到 [{}] 文件", databaseType, script);
            return;
        }
        String fileUrl = null;
        try {
            for (org.springframework.core.io.Resource resource : resources) {
                fileUrl = resource.getURL().getFile();
                String sql = FileUtil.readUtf8String(fileUrl);
                if (StrUtil.isNotBlank(sql)) {
                    sql = sql.replaceAll("\t|\r|\n", "");
                    if (StrUtil.isNotBlank(sql)) {
                        String[] sqlArray = sql.split(StringPool.BACK_SLASH + StringPool.RIGHT_BRACKET + StringPool.SEMICOLON);
                        for (String s : sqlArray) {
                            log.info(s + StringPool.RIGHT_BRACKET);
                            sqlServerMapper.executeSql(s + StringPool.RIGHT_BRACKET);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.info("执行 [{}] 初始化数据失败！！！读取文件异常 [{}]", databaseType, fileUrl);
            return;
        }
        log.info("执行 [{}] 初始化数据完成！！！", databaseType);
    }
}
