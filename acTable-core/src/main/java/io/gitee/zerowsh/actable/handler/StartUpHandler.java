package io.gitee.zerowsh.actable.handler;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.config.CreateTableConfig;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.service.MysqlImpl;
import io.gitee.zerowsh.actable.service.SqlServerImpl;
import io.gitee.zerowsh.actable.util.HandlerEntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 启动时进行处理的实现类
 *
 * @author zero
 */
@Component
@Slf4j
public class StartUpHandler {
    @Resource
    private SqlServerImpl sqlServerImpl;
    @Resource
    private MysqlImpl mysqlImpl;

    @Resource
    private CreateTableConfig createTableConfig;

    @PostConstruct
    public void startHandler() {
        if (Objects.isNull(createTableConfig.getModel())
                || Objects.equals(createTableConfig.getModel(), ModelEnums.NONE)) {
            log.info("自动建表不做任何操作！！！");
            return;
        }
        String entityPackage = createTableConfig.getEntityPackage();
        if (StrUtil.isBlank(entityPackage)) {
            log.warn("请设置实体类包路径！！！");
            return;
        }
        List<TableInfo> tableInfoList = HandlerEntityUtils.getTableInfoByEntityPackage(createTableConfig);
        if (CollectionUtil.isEmpty(tableInfoList)) {
            log.warn("没有找到@Table或@TableName标记的类！！！ entityPackage={}", entityPackage);
            return;
        }
        switch (createTableConfig.getDatabaseType()) {
            case MYSQL:
                mysqlImpl.acTable(createTableConfig, tableInfoList);
                break;
            case SQL_SERVER:
                sqlServerImpl.acTable(createTableConfig, tableInfoList);
                break;
            default:
                log.info(StrUtil.format("暂不支持{}！！！", createTableConfig.getDatabaseType()));
        }
    }
}
