package com.vtc.openplatform.gateway;

import com.vtc.openplatform.gateway.config.PartnerGatewayProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * G7：按 partnerId 限流（Redis 固定窗口计数）。默认关闭，见 backlog。
 */
@Component
public class PartnerRateLimiter {

    private final PartnerGatewayProperties properties;

    private final StringRedisTemplate stringRedisTemplate;

    public PartnerRateLimiter(PartnerGatewayProperties properties,
                              ObjectProvider<StringRedisTemplate> stringRedisTemplate) {
        this.properties = properties;
        this.stringRedisTemplate = stringRedisTemplate.getIfAvailable();
    }

    public boolean isAllowed(String partnerId) {
        PartnerGatewayProperties.RateLimit rateLimit = properties.getRateLimit();
        if (!rateLimit.isEnabled() || !StringUtils.hasText(partnerId) || rateLimit.getDefaultQps() <= 0) {
            return true;
        }
        if (stringRedisTemplate == null) {
            return true;
        }
        int windowSeconds = Math.max(PartnerGatewayConstants.RATE_LIMIT_MIN_WINDOW_SECONDS,
                rateLimit.getWindowSeconds());
        long window = System.currentTimeMillis() / (windowSeconds * PartnerGatewayConstants.MILLIS_PER_SECOND);
        String key = PartnerGatewayConstants.REDIS_RATE_KEY_PREFIX + partnerId + ":" + window;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count.longValue() == PartnerGatewayConstants.RATE_LIMIT_FIRST_HIT) {
            stringRedisTemplate.expire(key, windowSeconds + 1L, TimeUnit.SECONDS);
        }
        return count != null && count <= rateLimit.getDefaultQps();
    }
}
