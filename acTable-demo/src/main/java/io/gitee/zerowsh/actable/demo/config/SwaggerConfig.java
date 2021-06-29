package io.gitee.zerowsh.actable.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Swagger 配置
 *
 * @author zero
 */
@Component
@EnableSwagger2
@Getter
@Setter
@ConfigurationProperties(prefix = "swagger")
public class SwaggerConfig {
    private String basePackage = "io.gitee.zerowsh.actable.demo.controller";
    private String title = "Spring Boot Jpa";
    private String description = "接口服务";
    private String version = "API V1.0.0";
    private String termsOfServiceUrl = "https://www.baidu.com/";
    private String emailAndName = "15397608105@163.com（zero）";

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .pathMapping("/")
                .select()
                .apis(RequestHandlerSelectors.any())
                .apis(RequestHandlerSelectors.basePackage(basePackage))
                .build().directModelSubstitute(Timestamp.class, Date.class);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(title)
                .description(description)
                .termsOfServiceUrl(termsOfServiceUrl)
                .version(version)
                .contact(new Contact(emailAndName, null, null)).build();
    }
}
