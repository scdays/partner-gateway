package com.vtc.openplatform.gateway.support;

import com.vtc.openplatform.gateway.PartnerGatewayConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;

/**
 * 解析或生成全链路 requestId。
 */
public final class RequestIdResolver {

    private RequestIdResolver() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    public static String resolve(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String requestId = headers.getFirst(PartnerGatewayConstants.HEADER_REQUEST_ID);
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        return PartnerGatewayConstants.REQUEST_ID_PREFIX
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, PartnerGatewayConstants.REQUEST_ID_RANDOM_LENGTH);
    }
}
