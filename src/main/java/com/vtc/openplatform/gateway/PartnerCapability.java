package com.vtc.openplatform.gateway;

/**
 * 开放平台能力码，与 OpenAPI / external 接口规范 §8 一致。
 */
public enum PartnerCapability {

    TASK_WRITE,
    TASK_READ,
    INSTANCE_READ,
    INSTANCE_VERIFY,
    INSTANCE_REMEDIATE,
    INSTANCE_ARCHIVE,
    INSTANCE_VERIFY_FIX,
    EXPORT_READ,
    EVENT_SUBSCRIBE
}
