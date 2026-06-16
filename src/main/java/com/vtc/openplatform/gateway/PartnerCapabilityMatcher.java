package com.vtc.openplatform.gateway;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 路径 + 方法 → capability 映射（G4–G5）。
 */
@Component
public class PartnerCapabilityMatcher {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<CapabilityRule> RULES = Collections.unmodifiableList(Arrays.asList(
            rule(HttpMethod.POST, "/api/open/v1/tasks/vul", PartnerCapability.TASK_WRITE),
            rule(HttpMethod.POST, "/api/open/v1/tasks/file", PartnerCapability.TASK_WRITE),
            rule(HttpMethod.GET, "/api/open/v1/tasks", PartnerCapability.TASK_READ),
            rule(HttpMethod.GET, "/api/open/v1/tasks/*", PartnerCapability.TASK_READ),
            rule(HttpMethod.POST, "/api/open/v1/instances/search", PartnerCapability.INSTANCE_READ),
            rule(HttpMethod.GET, "/api/open/v1/instances/*", PartnerCapability.INSTANCE_READ),
            rule(HttpMethod.POST, "/api/open/v1/instances/*/verify", PartnerCapability.INSTANCE_VERIFY),
            rule(HttpMethod.POST, "/api/open/v1/instances/verify:batch", PartnerCapability.INSTANCE_VERIFY),
            rule(HttpMethod.POST, "/api/open/v1/instances/*/remediate", PartnerCapability.INSTANCE_REMEDIATE),
            rule(HttpMethod.POST, "/api/open/v1/instances/remediate:batch", PartnerCapability.INSTANCE_REMEDIATE),
            rule(HttpMethod.POST, "/api/open/v1/instances/*/archive", PartnerCapability.INSTANCE_ARCHIVE),
            rule(HttpMethod.POST, "/api/open/v1/instances/*/verify-fix", PartnerCapability.INSTANCE_VERIFY_FIX),
            rule(HttpMethod.POST, "/api/open/v1/instances/verify-fix:batch", PartnerCapability.INSTANCE_VERIFY_FIX),
            rule(HttpMethod.GET, "/api/open/v1/exports/*", PartnerCapability.EXPORT_READ),
            rule(HttpMethod.GET, "/api/open/v1/exports/*/download", PartnerCapability.EXPORT_READ),
            rule(HttpMethod.GET, "/api/open/v1/tasks/*/exports", PartnerCapability.EXPORT_READ),
            rule(HttpMethod.POST, "/api/open/v1/instances/*/unfixable-records", PartnerCapability.INSTANCE_ARCHIVE)
    ));

    public Optional<PartnerCapability> requiredCapability(String method, String path) {
        if (!StringUtils.hasText(method) || !StringUtils.hasText(path)) {
            return Optional.empty();
        }
        HttpMethod httpMethod;
        try {
            httpMethod = HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        for (CapabilityRule rule : RULES) {
            if (rule.matches(httpMethod, path)) {
                return Optional.of(rule.getCapability());
            }
        }
        return Optional.empty();
    }

    private static CapabilityRule rule(HttpMethod method, String pattern, PartnerCapability capability) {
        return new CapabilityRule(method, pattern, capability);
    }

    private static final class CapabilityRule {

        private final HttpMethod method;

        private final String pattern;

        private final PartnerCapability capability;

        private CapabilityRule(HttpMethod method, String pattern, PartnerCapability capability) {
            this.method = method;
            this.pattern = pattern;
            this.capability = capability;
        }

        private boolean matches(HttpMethod httpMethod, String path) {
            return method == httpMethod && PATH_MATCHER.match(pattern, path);
        }

        private PartnerCapability getCapability() {
            return capability;
        }
    }
}
