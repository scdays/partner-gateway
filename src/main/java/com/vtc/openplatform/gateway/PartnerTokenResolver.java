package com.vtc.openplatform.gateway;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.vtc.openplatform.gateway.config.PartnerGatewayProperties;
import com.vtc.openplatform.gateway.dto.PartnerJwtClaims;
import com.vtc.openplatform.gateway.dto.PartnerTokenContext;
import com.vtc.openplatform.gateway.infra.feign.IOpenApiIntrospectClient;
import com.vtc.openplatform.gateway.support.PartnerJwtSupport;
import com.vtc.openplatform.gateway.support.Sha256Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Partner Token 解析：JWT（{@code JWTUtils.create}）+ Redis 上下文，对齐 open-api-service。
 */
@Component
public class PartnerTokenResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerTokenResolver.class);

    private final PartnerGatewayProperties properties;

    private final StringRedisTemplate stringRedisTemplate;

    private final IOpenApiIntrospectClient openApiIntrospectClient;

    public PartnerTokenResolver(PartnerGatewayProperties properties,
                                ObjectProvider<StringRedisTemplate> stringRedisTemplate,
                                ObjectProvider<IOpenApiIntrospectClient> openApiIntrospectClient) {
        this.properties = properties;
        this.stringRedisTemplate = stringRedisTemplate.getIfAvailable();
        this.openApiIntrospectClient = openApiIntrospectClient.getIfAvailable();
    }

    public Optional<PartnerTokenContext> resolve(String bearerToken) {
        if (!StringUtils.hasText(bearerToken)) {
            return Optional.empty();
        }
        Optional<PartnerTokenContext> contextOptional;
        if (PartnerGatewayConstants.INTROSPECT_MODE_FEIGN.equalsIgnoreCase(properties.getIntrospectMode())
                && openApiIntrospectClient != null) {
            contextOptional = resolveByFeign(bearerToken);
        } else {
            contextOptional = resolveByJwtAndRedis(bearerToken);
        }
        return contextOptional.filter(this::isValidContext);
    }

    private boolean isValidContext(PartnerTokenContext context) {
        if (context == null || !StringUtils.hasText(context.getPartnerId())) {
            return false;
        }
        if (!context.isPartnerSubject()) {
            return false;
        }
        Long expiresAt = context.getExpiresAt();
        if (expiresAt != null && expiresAt > 0
                && expiresAt < System.currentTimeMillis() / PartnerGatewayConstants.MILLIS_PER_SECOND) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Partner token expired, partnerId={}", context.getPartnerId());
            }
            return false;
        }
        return true;
    }

    /**
     * 与 open-api-service 一致：JWT 校验 + Redis {@code partner:token:{sha256(accessToken)}}。
     */
    private Optional<PartnerTokenContext> resolveByJwtAndRedis(String bearerToken) {
        if (stringRedisTemplate == null) {
            LOGGER.warn("StringRedisTemplate not available; configure Redis or introspect-mode=feign");
            return Optional.empty();
        }

        Optional<PartnerJwtClaims> jwtClaims = Optional.empty();
        if (properties.isJwtValidateEnabled()) {
            jwtClaims = PartnerJwtSupport.parseClaims(bearerToken);
            if (!jwtClaims.isPresent()) {
                return Optional.empty();
            }
        }

        Optional<PartnerTokenContext> redisContext = loadRedisContext(bearerToken);
        if (!redisContext.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Partner token not in Redis or revoked");
            }
            return Optional.empty();
        }

        if (jwtClaims.isPresent() && !jwtClaims.get().getPartnerId().equals(redisContext.get().getPartnerId())) {
            LOGGER.warn("JWT partnerId mismatch Redis context, jwt={}, redis={}",
                    jwtClaims.get().getPartnerId(), redisContext.get().getPartnerId());
            return Optional.empty();
        }

        return redisContext;
    }

    private Optional<PartnerTokenContext> loadRedisContext(String bearerToken) {
        String cacheKey = PartnerGatewayConstants.REDIS_KEY_PREFIX + Sha256Utils.hex(bearerToken);
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }
        try {
            return Optional.of(JSON.parseObject(json, PartnerTokenContext.class));
        } catch (JSONException ex) {
            LOGGER.warn("Partner token Redis context parse failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<PartnerTokenContext> resolveByFeign(String bearerToken) {
        if (properties.isJwtValidateEnabled() && !PartnerJwtSupport.parseClaims(bearerToken).isPresent()) {
            return Optional.empty();
        }
        Map<String, String> request = new HashMap<>(1);
        request.put(PartnerGatewayConstants.JSON_FIELD_TOKEN, bearerToken);
        try {
            Map<String, Object> response = openApiIntrospectClient.introspect(request);
            if (response == null || response.isEmpty()) {
                return Optional.empty();
            }
            Object codeObj = response.get(PartnerGatewayConstants.JSON_FIELD_CODE);
            if (codeObj instanceof Number
                    && ((Number) codeObj).intValue() != PartnerGatewayConstants.API_RESPONSE_CODE_OK) {
                return Optional.empty();
            }
            Object data = response.get(PartnerGatewayConstants.JSON_FIELD_DATA);
            if (data == null) {
                return Optional.empty();
            }
            return Optional.of(JSON.parseObject(JSON.toJSONString(data), PartnerTokenContext.class));
        } catch (JSONException ex) {
            LOGGER.warn("open-api-service introspect parse failed: {}", ex.getMessage());
            return Optional.empty();
        } catch (RuntimeException ex) {
            LOGGER.warn("open-api-service introspect failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
