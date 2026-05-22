package com.vtc.openplatform.gateway.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.vtc.openplatform.gateway.PartnerGatewayConstants;
import com.vtc.openplatform.gateway.dto.PartnerJwtClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Partner JWT 校验（与 spore {@code JWTUtils} / open-api-service 签发算法、密钥一致，不引入 spore-starter-core）。
 */
public final class PartnerJwtSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerJwtSupport.class);

    /**
     * 与 {@code com.botany.spore.core.utils.JWTUtils} 保持一致。
     */
    private static final String SECURITY_KEY = "fd2wfd!g021nk90fd1h3kfd902*)Y!*&c3-(dmg)1==";

    private static final int JWT_PART_COUNT = 3;

    private static final String JWT_CLAIM_SUB = "sub";

    private static final String JWT_CLAIM_TYP = "typ";

    private static final String JWT_CLAIM_CLIENT_ID = "clientId";

    private static final String JWT_CLAIM_CAPABILITIES = "capabilities";

    private PartnerJwtSupport() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 校验 JWT 签名/结构并解析 Partner claims。
     */
    public static Optional<PartnerJwtClaims> parseClaims(String accessToken) {
        if (!StringUtils.hasText(accessToken) || !isValidate(accessToken)) {
            return Optional.empty();
        }
        String claimsJson = extractSubjectJson(accessToken);
        if (!StringUtils.hasText(claimsJson)) {
            return Optional.empty();
        }
        try {
            JSONObject json = JSON.parseObject(claimsJson);
            String typ = json.getString(JWT_CLAIM_TYP);
            if (!PartnerGatewayConstants.JWT_TYP_PARTNER.equalsIgnoreCase(typ)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Partner JWT typ invalid: {}", typ);
                }
                return Optional.empty();
            }
            String partnerId = json.getString(JWT_CLAIM_SUB);
            if (!StringUtils.hasText(partnerId)) {
                return Optional.empty();
            }
            return Optional.of(PartnerJwtClaims.builder()
                    .partnerId(partnerId)
                    .typ(typ)
                    .clientId(json.getString(JWT_CLAIM_CLIENT_ID))
                    .capabilities(json.getJSONArray(JWT_CLAIM_CAPABILITIES) == null
                            ? null
                            : json.getJSONArray(JWT_CLAIM_CAPABILITIES).toJavaList(String.class))
                    .build());
        } catch (JSONException ex) {
            LOGGER.warn("Partner JWT claims parse failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 对齐 {@code JWTUtils.isValidate}：签名非法拒绝；过期 Token 仍视为结构合法（由 Redis TTL 兜底）。
     */
    public static boolean isValidate(String token) {
        if (!StringUtils.hasText(token) || token.split("\\.").length != JWT_PART_COUNT) {
            return false;
        }
        try {
            parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            return true;
        } catch (Exception ex) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Partner JWT validate failed: {}", ex.getMessage());
            }
            return false;
        }
    }

    private static Claims parseClaimsJws(String token) {
        return Jwts.parser().setSigningKey(SECURITY_KEY).parseClaimsJws(token).getBody();
    }

    /**
     * 对齐 {@code JWTUtils.getSubject}：JWT payload 的 sub 字段为 open-api-service 写入的 claims JSON 字符串。
     */
    private static String extractSubjectJson(String token) {
        try {
            return parseClaimsJws(token).getSubject();
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            return ex.getClaims().getSubject();
        } catch (Exception ex) {
            return null;
        }
    }
}
