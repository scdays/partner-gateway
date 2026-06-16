package com.vtc.openplatform.gateway.filter;

import com.vtc.openplatform.gateway.PartnerGatewayConstants;
import com.vtc.openplatform.gateway.config.PartnerGatewayProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * �����������E2E ����̨������Ԥ�죺ֱ�� partner-gateway ʱ OPTIONS �� 200��
 * ��������������˵����� CORS���ɰ���ر� {@code partner.gateway.cors.enabled}��
 */
@Component
public class PartnerGatewayCorsFilter implements WebFilter, Ordered {

    private final PartnerGatewayProperties properties;

    public PartnerGatewayCorsFilter(PartnerGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.getCors().isEnabled()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        if (!CorsUtils.isCorsRequest(request)) {
            return chain.filter(exchange);
        }
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, CorsUtils.isPreFlightRequest(request)
                ? request.getHeaders().getOrigin() : "*");
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                "Authorization, Content-Type, X-Partner-Id, X-Request-Id, Idempotency-Key");
        headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        if (request.getMethod() == HttpMethod.OPTIONS) {
            response.setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return PartnerGatewayConstants.AUTH_FILTER_ORDER - 10;
    }
}
