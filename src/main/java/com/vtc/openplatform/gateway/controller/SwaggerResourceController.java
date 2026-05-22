package com.vtc.openplatform.gateway.controller;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.vtc.openplatform.gateway.component.CustomSwaggerResourceProvider;
import com.vtc.openplatform.gateway.support.Knife4jEnvSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger.web.SecurityConfigurationBuilder;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;

import java.util.List;

/**
 * Swagger 聚合接口（Knife4j / swagger-ui 依赖），对齐 morningglory。
 */
@RestController
@RequestMapping("/swagger-resources")
public class SwaggerResourceController {

    private final CustomSwaggerResourceProvider swaggerResourceProvider;

    private final NacosDiscoveryProperties discoveryProperties;

    @Value("#{'${knife4j.show-ui-envs:DEV}'.split(',')}")
    private List<String> showSwaggerUiEnvs;

    public SwaggerResourceController(CustomSwaggerResourceProvider swaggerResourceProvider,
                                       NacosDiscoveryProperties discoveryProperties) {
        this.swaggerResourceProvider = swaggerResourceProvider;
        this.discoveryProperties = discoveryProperties;
    }

    @GetMapping("/configuration/security")
    public ResponseEntity<SecurityConfiguration> securityConfiguration() {
        if (!isSwaggerEnv()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(SecurityConfigurationBuilder.builder().build());
    }

    @GetMapping("/configuration/ui")
    public ResponseEntity<UiConfiguration> uiConfiguration() {
        if (!isSwaggerEnv()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UiConfigurationBuilder.builder().build());
    }

    @GetMapping
    public ResponseEntity<List<SwaggerResource>> swaggerResources() {
        if (!isSwaggerEnv()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(swaggerResourceProvider.get());
    }

    private boolean isSwaggerEnv() {
        return Knife4jEnvSupport.isSwaggerEnv(discoveryProperties.getGroup(), showSwaggerUiEnvs);
    }
}
