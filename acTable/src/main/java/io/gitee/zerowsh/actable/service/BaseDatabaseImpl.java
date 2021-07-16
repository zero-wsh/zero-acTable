package io.gitee.zerowsh.actable.service;

import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.config.AcTableConfig;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.DatabaseTypeEnums;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.mapper.BaseDatabaseMapper;
import io.gitee.zerowsh.actable.util.AcTableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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
    private AcTableUtils acTableUtils;

    @Transactional(rollbackFor = Exception.class)
    public void acTable(AcTableConfig acTableConfig, List<TableInfo> tableInfoList) {
        DatabaseTypeEnums databaseType = acTableConfig.getDatabaseType();
        ModelEnums modelEnums = acTableConfig.getModel();
        List<String> executeSqlList;
        switch (acTableConfig.getDatabaseType()) {
            case MYSQL:
                executeSqlList = mysql.acTable(modelEnums, tableInfoList);
                break;
            case SQL_SERVER:
                executeSqlList = sqlServer.acTable(modelEnums, tableInfoList);
                break;
            default:
                log.info(StrUtil.format("暂不支持{}！！！", acTableConfig.getDatabaseType()));
                return;
        }
        log.info(StrUtil.format("执行 [{}] 自动建表。。。", databaseType));
        for (String sql : executeSqlList) {
            log.info(sql);
            baseDatabaseMapper.executeSql(sql);
        }
        log.info(StrUtil.format("执行 [{}] 自动建表完成！！！", databaseType));
        acTableUtils.executeScript();
    }
}
