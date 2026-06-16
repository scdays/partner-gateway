package com.vtc.openplatform.gateway.config;

import com.vtc.openplatform.gateway.PartnerGatewayConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Partner 网关配置（前缀 {@code partner.gateway}，默认关闭鉴权）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "partner.gateway")
public class PartnerGatewayProperties {

    /**
     * 是否启用 Partner 鉴权过滤器；默认 false，生产在 Nacos 显式开启。
     */
    private boolean enabled = false;

    private String pathPrefix = PartnerGatewayConstants.PATH_PREFIX;

    private boolean stripAuthorization = true;

    private List<String> tokenPaths = Arrays.asList(
            "/oauth/token",
            "/api/open/v1/oauth/token"
    );

    /**
     * {@link PartnerGatewayConstants#INTROSPECT_MODE_REDIS} 或 {@link PartnerGatewayConstants#INTROSPECT_MODE_FEIGN}。
     * <p>
     * redis 模式：先 {@code JWTUtils} 校验 JWT，再读 Redis {@code partner:token:{sha256}}（与 open-api-service 一致）。
     */
    private String introspectMode = PartnerGatewayConstants.INTROSPECT_MODE_REDIS;

    /**
     * 是否校验 JWT（须与 open-api-service 相同密钥）；redis / feign 模式均建议开启。
     */
    private boolean jwtValidateEnabled = true;

    private RateLimit rateLimit = new RateLimit();

    private Cors cors = new Cors();

    @Data
    public static class Cors {

        /** 浏览器联调跨域；第三方服务端调用可关闭 */
        private boolean enabled = true;
    }

    @Data
    public static class RateLimit {

        private boolean enabled = false;

        private int defaultQps = 100;

        private int windowSeconds = 1;
    }
}
