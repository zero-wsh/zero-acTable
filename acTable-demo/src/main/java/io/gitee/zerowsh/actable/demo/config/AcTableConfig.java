package io.gitee.zerowsh.actable.demo.config;

import io.gitee.zerowsh.actable.properties.AcTableProperties;
import io.gitee.zerowsh.actable.service.AcTableService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class AcTableConfig {
    @Bean
    public AcTableService acTableService(DataSource dataSource, AcTableProperties acTableProperties) {
        return new AcTableService(dataSource, acTableProperties);
    }

}
