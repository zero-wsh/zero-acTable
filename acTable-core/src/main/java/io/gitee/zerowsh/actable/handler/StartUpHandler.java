package io.gitee.zerowsh.actable.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

/**
 * 启动时进行处理的实现类--只支持sqlserver
 *
 * @author zero
 */
@Component
@Slf4j
public class StartUpHandler {
    @Autowired
    private DatabaseHandler databaseHandler;

    @PostConstruct
    @Transactional(rollbackFor = Exception.class)
    public void startHandler() {
        databaseHandler.createTable();
    }
}
