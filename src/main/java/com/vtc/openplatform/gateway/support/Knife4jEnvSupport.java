package com.vtc.openplatform.gateway.support;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Knife4j 环境开关判断（消除 Controller / Provider 重复逻辑）。
 */
public final class Knife4jEnvSupport {

    private Knife4jEnvSupport() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 当前 Nacos group 是否在允许展示 Swagger UI 的环境列表中。
     *
     * @param nacosGroup   Nacos 发现 group
     * @param showUiEnvs   配置项 knife4j.show-ui-envs 解析后的列表
     * @return 是否允许
     */
    public static boolean isSwaggerEnv(String nacosGroup, List<String> showUiEnvs) {
        if (!StringUtils.hasText(nacosGroup) || CollectionUtils.isEmpty(showUiEnvs)) {
            return false;
        }
        String trimmedGroup = nacosGroup.trim();
        for (String env : showUiEnvs) {
            if (env != null && trimmedGroup.equalsIgnoreCase(env.trim())) {
                return true;
            }
        }
        return false;
    }
}
