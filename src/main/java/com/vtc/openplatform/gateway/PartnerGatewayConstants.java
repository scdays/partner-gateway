package com.vtc.openplatform.gateway;

/**
 * Partner 网关常量（避免魔法值，符合阿里巴巴 Java 开发手册）。
 *
 * @see svmp/docs/internal/组件职责与接口映射.md §4
 */
public final class PartnerGatewayConstants {

    private PartnerGatewayConstants() {
        throw new UnsupportedOperationException("常量类禁止实例化");
    }

    public static final String PATH_PREFIX = "/api/open/v1";

    public static final String PATH_INTERNAL_PREFIX = "/internal";

    public static final String HEADER_PARTNER_ID = "X-Partner-Id";

    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String BEARER_PREFIX = "Bearer ";

    public static final String SUBJECT_TYPE_PARTNER = "PARTNER";

    public static final String REDIS_KEY_PREFIX = "partner:token:";

    public static final String REDIS_RATE_KEY_PREFIX = "partner:rate:";

    /** Swagger2 默认文档后缀（聚合路由：/{serviceId}/v2/api-docs） */
    public static final String SWAGGER2_URL_SUFFIX = "/v2/api-docs";

    public static final String EXCHANGE_ATTR_AUTHENTICATED = "partner_gateway_authed";

    public static final int AUTH_FILTER_ORDER = -100;

    public static final int CODE_AUTH_FAILED = 40101;

    public static final int CODE_CAPABILITY_DENIED = 40301;

    public static final int CODE_RATE_LIMITED = 42901;

    public static final int API_RESPONSE_CODE_OK = 0;

    public static final String INTROSPECT_MODE_FEIGN = "feign";

    public static final String INTROSPECT_MODE_REDIS = "redis";

    /** JWT claims 字段 typ，与 open-api-service 签发一致 */
    public static final String JWT_TYP_PARTNER = "partner";

    public static final String JSON_FIELD_CODE = "code";

    public static final String JSON_FIELD_DATA = "data";

    public static final String JSON_FIELD_TOKEN = "token";

    public static final String REQUEST_ID_PREFIX = "req-";

    public static final int REQUEST_ID_RANDOM_LENGTH = 16;

    public static final long MILLIS_PER_SECOND = 1000L;

    public static final long RATE_LIMIT_FIRST_HIT = 1L;

    public static final int RATE_LIMIT_MIN_WINDOW_SECONDS = 1;

    public static final String MSG_TOKEN_INVALID = "Partner token invalid or expired";

    public static final String MSG_SUBJECT_TYPE_INVALID = "Token subjectType must be PARTNER";

    public static final String MSG_INTERNAL_NOT_EXPOSED = "Internal API not exposed on partner gateway";

    public static final String MSG_RATE_LIMIT_EXCEEDED = "Partner rate limit exceeded";

    public static final String MSG_CAPABILITY_DENIED_PREFIX = "Capability not granted: ";

    /** @deprecated 使用 {@link #SWAGGER2_URL_SUFFIX} */
    @Deprecated
    public static final String SWAGGER2URL = SWAGGER2_URL_SUFFIX;
}
