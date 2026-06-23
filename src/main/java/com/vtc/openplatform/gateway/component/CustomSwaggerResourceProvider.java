package com.vtc.openplatform.gateway.component;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.vtc.openplatform.gateway.PartnerGatewayConstants;
import com.vtc.openplatform.gateway.support.Knife4jEnvSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 聚合下游服务 Swagger；WebFlux 下异步缓存路由，禁止在 {@link #get()} 中 block。
 */
@Primary
@Component
public class CustomSwaggerResourceProvider implements SwaggerResourcesProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomSwaggerResourceProvider.class);

    private final RouteLocator routeLocator;

    private final ObjectProvider<NacosDiscoveryProperties> discoveryPropertiesProvider;

    private final AtomicReference<List<SwaggerResource>> cachedResources =
            new AtomicReference<>(Collections.emptyList());

    @Value("${spring.application.name:partner-gateway}")
    private String applicationName;

    @Value("${spring.application.env:DEV}")
    private String applicationEnv;

    @Value("#{'${knife4j.show-ui-envs:DEV}'.split(',')}")
    private List<String> showSwaggerUiEnvs;

    public CustomSwaggerResourceProvider(RouteLocator routeLocator,
                                         ObjectProvider<NacosDiscoveryProperties> discoveryPropertiesProvider) {
        this.routeLocator = routeLocator;
        this.discoveryPropertiesProvider = discoveryPropertiesProvider;
    }

    @PostConstruct
    public void init() {
        refreshRoutesCache();
    }

    @EventListener(RefreshRoutesEvent.class)
    public void onRoutesRefreshed(RefreshRoutesEvent event) {
        refreshRoutesCache();
    }

    private void refreshRoutesCache() {
        routeLocator.getRoutes()
                .filter(route -> route.getUri().getHost() != null)
                .filter(route -> !applicationName.equals(route.getUri().getHost()))
                .map(route -> route.getUri().getHost())
                .distinct()
                .collectList()
                .subscribe(
                        this::rebuildCache,
                        error -> LOGGER.warn("刷新 Swagger 路由缓存失败: {}", error.getMessage())
                );
    }

    private void rebuildCache(List<String> routeHosts) {
        if (CollectionUtils.isEmpty(routeHosts)) {
            cachedResources.set(Collections.emptyList());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Swagger 聚合：未发现下游路由");
            }
            return;
        }
        Set<String> seenUrls = new HashSet<>(routeHosts.size());
        List<SwaggerResource> resources = new ArrayList<>(routeHosts.size());
        for (String serviceId : routeHosts) {
            String url = "/" + serviceId + PartnerGatewayConstants.SWAGGER2_URL_SUFFIX;
            if (seenUrls.add(url)) {
                SwaggerResource swaggerResource = new SwaggerResource();
                swaggerResource.setUrl(url);
                swaggerResource.setName(serviceId);
                resources.add(swaggerResource);
            }
        }
        cachedResources.set(Collections.unmodifiableList(resources));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Swagger 聚合已刷新，下游: {}", routeHosts);
        }
    }

    @Override
    public List<SwaggerResource> get() {
        if (!isSwaggerEnv()) {
            return Collections.emptyList();
        }
        return cachedResources.get();
    }

    private boolean isSwaggerEnv() {
        return Knife4jEnvSupport.isSwaggerEnv(resolveSwaggerEnv(), showSwaggerUiEnvs)
                || Knife4jEnvSupport.isSwaggerEnv(applicationEnv, showSwaggerUiEnvs);
    }

    private String resolveSwaggerEnv() {
        NacosDiscoveryProperties discoveryProperties = discoveryPropertiesProvider.getIfAvailable();
        if (discoveryProperties != null && StringUtils.hasText(discoveryProperties.getGroup())) {
            return discoveryProperties.getGroup();
        }
        return applicationEnv;
    }
}
