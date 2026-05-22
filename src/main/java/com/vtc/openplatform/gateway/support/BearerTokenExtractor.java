package com.vtc.openplatform.gateway.support;

import com.vtc.openplatform.gateway.PartnerGatewayConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/**
 * 从 Authorization 头解析 Bearer Token。
 */
public final class BearerTokenExtractor {

    private BearerTokenExtractor() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    public static String extract(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String authorization = headers.getFirst(PartnerGatewayConstants.HEADER_AUTHORIZATION);
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.regionMatches(true, 0, PartnerGatewayConstants.BEARER_PREFIX, 0,
                PartnerGatewayConstants.BEARER_PREFIX.length())) {
            return authorization.substring(PartnerGatewayConstants.BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
