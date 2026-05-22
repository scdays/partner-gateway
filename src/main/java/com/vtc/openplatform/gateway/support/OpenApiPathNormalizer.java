package com.vtc.openplatform.gateway.support;

import com.vtc.openplatform.gateway.PartnerGatewayConstants;
import org.springframework.util.StringUtils;

/**
 * 将 Knife4j 聚合路径（如 {@code /open-api-service/api/open/v1/tasks}）
 * 归一化为开放平台契约路径（{@code /api/open/v1/tasks}）。
 */
public final class OpenApiPathNormalizer {

    private static final String OAUTH_TOKEN_PATH = "/oauth/token";

    private OpenApiPathNormalizer() {
    }

    public static String normalize(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return rawPath;
        }
        int openApiIdx = rawPath.indexOf(PartnerGatewayConstants.PATH_PREFIX);
        if (openApiIdx >= 0) {
            return rawPath.substring(openApiIdx);
        }
        int oauthIdx = rawPath.indexOf(OAUTH_TOKEN_PATH);
        if (oauthIdx >= 0) {
            return rawPath.substring(oauthIdx);
        }
        return rawPath;
    }
}
