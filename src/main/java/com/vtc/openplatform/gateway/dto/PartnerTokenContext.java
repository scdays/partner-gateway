package com.vtc.openplatform.gateway.dto;

import com.vtc.openplatform.gateway.PartnerGatewayConstants;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Partner Token 解析结果（来自 Redis 或 open-api-service introspect）。
 */
@Data
public class PartnerTokenContext {

    private String subjectType;

    private String partnerId;

    private List<String> capabilities;

    private String clientId;

    private Long issuedAt;

    private Long expiresAt;

    public PartnerTokenContext() {
    }

    public boolean isPartnerSubject() {
        return PartnerGatewayConstants.SUBJECT_TYPE_PARTNER.equalsIgnoreCase(subjectType);
    }

    public List<String> safeCapabilities() {
        return capabilities == null ? Collections.emptyList() : capabilities;
    }
}
