package com.vtc.openplatform.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Partner 网关启动入口（依赖栈对齐 morningglory，不继承 esmp-support）。
 * <p>Swagger 文档：{@code http://host:35770/doc.html}（仅 DEV 等 show-ui-envs）
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableDiscoveryClient
@EnableFeignClients("com.vtc.openplatform.gateway")
public class ApplicationStart {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStart.class);

    public static void main(String[] args) {
        SpringApplication.run(ApplicationStart.class, args);
        logger.info("partner-gateway started");
    }
}
