package io.gitee.zerowsh.actable.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import io.gitee.zerowsh.actable.config.AcTableConfig;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.mapper.BaseDatabaseMapper;
import io.gitee.zerowsh.actable.util.AcTableThreadLocalUtils;
import io.gitee.zerowsh.actable.util.HandlerEntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.gitee.zerowsh.actable.constant.AcTableConstants.MYSQL;
import static io.gitee.zerowsh.actable.constant.AcTableConstants.SQL_SERVER;

/**
 * mysql实现
 *
 * @author zero
 */
@Component
@Slf4j
public class BaseDatabaseImpl {

    @Resource
    private BaseDatabaseMapper baseDatabaseMapper;

    @Resource
    private SqlServerImpl sqlServer;
    @Resource
    private MysqlImpl mysql;
    @Resource
    private AcTableConfig acTableConfig;
    @Resource
    private DataSource dataSource;

    @Transactional(rollbackFor = Exception.class)
    public void acTable() {
        if (Objects.isNull(acTableConfig.getModel())
                || Objects.equals(acTableConfig.getModel(), ModelEnums.NONE)) {
            log.info("自动建表不做任何操作！！！");
            return;
        }
        String entityPackage = acTableConfig.getEntityPackage();
        if (StrUtil.isBlank(entityPackage)) {
            log.warn("请设置实体类包路径！！！");
            return;
        }
        String databaseType = null;
        try {
            databaseType = dataSource.getConnection().getMetaData().getDatabaseProductName();
            AcTableThreadLocalUtils.setDatabaseType(databaseType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StrUtil.isBlank(databaseType)) {
            throw new RuntimeException("获取数据库类型失败！！！");
        }

        List<TableInfo> tableInfoList = HandlerEntityUtils.getTableInfoByEntityPackage(acTableConfig);
        if (CollectionUtil.isEmpty(tableInfoList)) {
            log.warn("没有找到@Table或@TableName标记的类！！！ entityPackage={}", entityPackage);
            return;
        }

        ModelEnums modelEnums = acTableConfig.getModel();
        List<String> executeSqlList;
        switch (databaseType) {
            case MYSQL:
                executeSqlList = mysql.acTable(modelEnums, tableInfoList);
                break;
            case SQL_SERVER:
                executeSqlList = sqlServer.acTable(modelEnums, tableInfoList);
                break;
            default:
                log.info(StrUtil.format("自动建表暂不支持 [{}]！！！", databaseType));
                return;
        }
        log.info(StrUtil.format("执行 [{}] 自动建表。。。", databaseType));
        for (String sql : executeSqlList) {
            log.info(sql);
            baseDatabaseMapper.executeSql(sql);
        }
        log.info(StrUtil.format("执行 [{}] 自动建表完成！！！", databaseType));
        this.executeScript(acTableConfig.getScript());
        AcTableThreadLocalUtils.remove();
    }

    /**
     * 执行脚本
     *
     * @param script
     */
    public void executeScript(String script) {
        String databaseType = AcTableThreadLocalUtils.getDatabaseType();
        if (StrUtil.isBlank(script)) {
            return;
        }
        log.info("执行 [{}] 初始化数据。。。", databaseType);
        List<org.springframework.core.io.Resource> list = new ArrayList<>();
        for (String s : script.split(StringPool.COMMA)) {
            try {
                list.addAll(Arrays.asList(new PathMatchingResourcePatternResolver().getResources(ResourceUtils.CLASSPATH_URL_PREFIX + s)));
            } catch (IOException e) {
                log.warn("[{}] 没找到初始化数据文件 [{}]！！！", databaseType, s);
                return;
            }
        }

        String fileUrl = null;
        try {
            for (org.springframework.core.io.Resource resource : list) {
                File file = resource.getFile();
                if (file.isFile()) {
                    fileUrl = file.getAbsolutePath();
                    String sql = FileUtil.readUtf8String(file);
                    if (StrUtil.isNotBlank(sql)) {
                        log.info(sql);
                        baseDatabaseMapper.executeSql(sql);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("执行 [{}] 初始化数据失败！！！读取文件异常 [{}]", databaseType, fileUrl);
            return;
        }
        log.info("执行 [{}] 初始化数据完成！！！", databaseType);
    }
}
