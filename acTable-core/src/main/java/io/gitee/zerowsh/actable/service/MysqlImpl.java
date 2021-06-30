package io.gitee.zerowsh.actable.service;

import io.gitee.zerowsh.actable.config.CreateTableConfig;
import io.gitee.zerowsh.actable.dao.MysqlMapper;
import io.gitee.zerowsh.actable.dto.TableInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * mysql实现
 *
 * @author zero
 */
@Component
@Slf4j
public class MysqlImpl {

    @Resource
    private MysqlMapper mysqlMapper;

    @Transactional(rollbackFor = Exception.class)
    public void acTable(CreateTableConfig createTableConfig, List<TableInfo> tableInfoList) {
        log.info("暂不支持Mysql！！！");
    }
}
