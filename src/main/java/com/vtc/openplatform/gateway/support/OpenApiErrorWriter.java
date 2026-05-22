package com.vtc.openplatform.gateway.support;

import com.alibaba.fastjson.JSONObject;
import com.vtc.openplatform.gateway.PartnerGatewayConstants;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 开放平台统一错误体（HTTP 200 + business code），与 external §9 一致。
 */
public final class OpenApiErrorWriter {

    private static final HttpStatus HTTP_STATUS_OK = HttpStatus.OK;

    private static final String JSON_FIELD_CODE = "code";

    private static final String JSON_FIELD_MESSAGE = "message";

    private static final String JSON_FIELD_DATA = "data";

    private static final String JSON_FIELD_REQUEST_ID = "requestId";

    private OpenApiErrorWriter() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    public static Mono<Void> write(ServerWebExchange exchange, String requestId, int code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HTTP_STATUS_OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(PartnerGatewayConstants.HEADER_REQUEST_ID, requestId);

        JSONObject body = new JSONObject(true);
        body.put(JSON_FIELD_CODE, code);
        body.put(JSON_FIELD_MESSAGE, message);
        body.put(JSON_FIELD_DATA, null);
        body.put(JSON_FIELD_REQUEST_ID, requestId);

        byte[] bytes = body.toJSONString().getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
