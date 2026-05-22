package com.vtc.openplatform.gateway.filter;

import com.vtc.openplatform.gateway.PartnerCapability;
import com.vtc.openplatform.gateway.PartnerCapabilityMatcher;
import com.vtc.openplatform.gateway.PartnerGatewayConstants;
import com.vtc.openplatform.gateway.PartnerRateLimiter;
import com.vtc.openplatform.gateway.PartnerTokenResolver;
import com.vtc.openplatform.gateway.config.PartnerGatewayProperties;
import com.vtc.openplatform.gateway.dto.PartnerTokenContext;
import com.vtc.openplatform.gateway.support.BearerTokenExtractor;
import com.vtc.openplatform.gateway.support.OpenApiErrorWriter;
import com.vtc.openplatform.gateway.support.OpenApiPathNormalizer;
import com.vtc.openplatform.gateway.support.RequestIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Partner 鉴权全局过滤器（G1–G6；G7 限流见 {@link PartnerRateLimiter}）。
 */
@Component
public class PartnerAuthFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerAuthFilter.class);

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final PartnerGatewayProperties properties;

    private final PartnerTokenResolver partnerTokenResolver;

    private final PartnerCapabilityMatcher partnerCapabilityMatcher;

    private final PartnerRateLimiter partnerRateLimiter;

    public PartnerAuthFilter(PartnerGatewayProperties properties,
                             PartnerTokenResolver partnerTokenResolver,
                             PartnerCapabilityMatcher partnerCapabilityMatcher,
                             PartnerRateLimiter partnerRateLimiter) {
        this.properties = properties;
        this.partnerTokenResolver = partnerTokenResolver;
        this.partnerCapabilityMatcher = partnerCapabilityMatcher;
        this.partnerRateLimiter = partnerRateLimiter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        if (Boolean.TRUE.equals(exchange.getAttribute(PartnerGatewayConstants.EXCHANGE_ATTR_AUTHENTICATED))) {
            return chain.filter(exchange);
        }

        String rawPath = exchange.getRequest().getURI().getPath();
        String path = OpenApiPathNormalizer.normalize(rawPath);
        if (path.startsWith(PartnerGatewayConstants.PATH_INTERNAL_PREFIX)
                || rawPath.contains(PartnerGatewayConstants.PATH_INTERNAL_PREFIX + "/")) {
            return OpenApiErrorWriter.write(exchange, RequestIdResolver.resolve(exchange),
                    PartnerGatewayConstants.CODE_AUTH_FAILED,
                    PartnerGatewayConstants.MSG_INTERNAL_NOT_EXPOSED);
        }

        if (!path.startsWith(properties.getPathPrefix()) && !isTokenPath(path)) {
            return chain.filter(exchange);
        }
        if (isTokenPath(path)) {
            return chain.filter(exchange);
        }

        return authenticateOpenApi(exchange, chain, path);
    }

    private Mono<Void> authenticateOpenApi(ServerWebExchange exchange, GatewayFilterChain chain, String path) {
        String requestId = RequestIdResolver.resolve(exchange);
        String bearerToken = BearerTokenExtractor.extract(exchange.getRequest().getHeaders());
        Optional<PartnerTokenContext> contextOptional = partnerTokenResolver.resolve(bearerToken);
        if (!contextOptional.isPresent()) {
            return OpenApiErrorWriter.write(exchange, requestId, PartnerGatewayConstants.CODE_AUTH_FAILED,
                    PartnerGatewayConstants.MSG_TOKEN_INVALID);
        }

        PartnerTokenContext context = contextOptional.get();
        if (!context.isPartnerSubject()) {
            return OpenApiErrorWriter.write(exchange, requestId, PartnerGatewayConstants.CODE_AUTH_FAILED,
                    PartnerGatewayConstants.MSG_SUBJECT_TYPE_INVALID);
        }

        Optional<PartnerCapability> required = partnerCapabilityMatcher.requiredCapability(
                exchange.getRequest().getMethodValue(), path);
        if (required.isPresent() && !hasCapability(context, required.get())) {
            return OpenApiErrorWriter.write(exchange, requestId, PartnerGatewayConstants.CODE_CAPABILITY_DENIED,
                    PartnerGatewayConstants.MSG_CAPABILITY_DENIED_PREFIX + required.get().name());
        }

        if (!partnerRateLimiter.isAllowed(context.getPartnerId())) {
            return OpenApiErrorWriter.write(exchange, requestId, PartnerGatewayConstants.CODE_RATE_LIMITED,
                    PartnerGatewayConstants.MSG_RATE_LIMIT_EXCEEDED);
        }

        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header(PartnerGatewayConstants.HEADER_PARTNER_ID, context.getPartnerId())
                .header(PartnerGatewayConstants.HEADER_REQUEST_ID, requestId);
        if (properties.isStripAuthorization()) {
            requestBuilder.headers(headers -> headers.remove(PartnerGatewayConstants.HEADER_AUTHORIZATION));
        }

        exchange.getAttributes().put(PartnerGatewayConstants.EXCHANGE_ATTR_AUTHENTICATED, Boolean.TRUE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Partner gateway passed partnerId={}, path={}, requestId={}",
                    context.getPartnerId(), path, requestId);
        }
        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    private boolean hasCapability(PartnerTokenContext context, PartnerCapability required) {
        String requiredName = required.name();
        for (String granted : context.safeCapabilities()) {
            if (granted != null && requiredName.equalsIgnoreCase(granted.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isTokenPath(String path) {
        for (String tokenPath : properties.getTokenPaths()) {
            if (!StringUtils.hasText(tokenPath)) {
                continue;
            }
            String trimmed = tokenPath.trim();
            if (path.equals(trimmed) || PATH_MATCHER.match(trimmed, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return PartnerGatewayConstants.AUTH_FILTER_ORDER;
    }
}
