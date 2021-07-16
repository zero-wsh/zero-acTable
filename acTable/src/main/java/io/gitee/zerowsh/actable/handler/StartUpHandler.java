package io.gitee.zerowsh.actable.handler;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import io.gitee.zerowsh.actable.config.AcTableConfig;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.service.BaseDatabaseImpl;
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
    private BaseDatabaseImpl baseDatabase;

    @Resource
    private AcTableConfig acTableConfig;

    @PostConstruct
    public void startHandler() {
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
        List<TableInfo> tableInfoList = HandlerEntityUtils.getTableInfoByEntityPackage(acTableConfig);
        if (CollectionUtil.isEmpty(tableInfoList)) {
            log.warn("没有找到@Table或@TableName标记的类！！！ entityPackage={}", entityPackage);
            return;
        }
        baseDatabase.acTable(acTableConfig, tableInfoList);
    }
}
