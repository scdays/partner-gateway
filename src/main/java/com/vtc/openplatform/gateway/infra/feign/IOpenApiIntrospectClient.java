package com.vtc.openplatform.gateway.infra.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 降级 introspect：partner-gateway → open-api-service。
 */
@FeignClient(name = "open-api-service", contextId = "openApiIntrospectClient")
public interface IOpenApiIntrospectClient {

    /**
     * 校验 Partner Token 并返回上下文。
     *
     * @param request 请求体，键 {@code token}
     * @return 开放平台统一响应 Map（含 code、data）
     */
    @PostMapping("/internal/token/introspect")
    Map<String, Object> introspect(@RequestBody Map<String, String> request);
}
