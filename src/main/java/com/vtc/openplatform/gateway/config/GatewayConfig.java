package com.vtc.openplatform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * WebFlux 静态资源（Knife4j / Swagger UI webjars）。
 */
@Configuration
public class GatewayConfig {

    private static final String WEBJARS_PATTERN = "/webjars/**";

    private static final String WEBJARS_LOCATION = "webjars/";

    @Bean
    public RouterFunction<ServerResponse> staticResourceRouter() {
        return RouterFunctions.resources(WEBJARS_PATTERN, new ClassPathResource(WEBJARS_LOCATION));
    }
}
