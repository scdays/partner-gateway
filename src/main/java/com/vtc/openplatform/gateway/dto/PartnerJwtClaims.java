package com.vtc.openplatform.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Partner JWT subject 内 claims（与 open-api-service {@code createAccessToken} 一致）。
 */
@Data
@Builder
public class PartnerJwtClaims {

    private String partnerId;

    private String typ;

    private String clientId;

    private List<String> capabilities;

    public List<String> safeCapabilities() {
        return capabilities == null ? Collections.emptyList() : capabilities;
    }
}
