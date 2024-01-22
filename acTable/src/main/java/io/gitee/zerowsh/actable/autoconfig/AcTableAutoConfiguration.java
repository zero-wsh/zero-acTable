package io.gitee.zerowsh.actable.autoconfig;

import io.gitee.zerowsh.actable.properties.AcTableProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author zero
 */
@Configuration
@EnableConfigurationProperties(AcTableProperties.class)
@ConditionalOnClass(DataSource.class)
public class AcTableAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AcTableService acTableService(DataSource dataSource, AcTableProperties acTableProperties) {
        return new AcTableService(dataSource, acTableProperties);
    }

}
