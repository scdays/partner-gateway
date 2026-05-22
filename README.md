# partner-gateway



Partner 专用公网入口（Nacos 服务名：`partner-gateway`），与门户网关 **morningglory 完全隔离**。



## 重要声明



| 项 | 说明 |

|----|------|

| **morningglory** | **零改动**。不承载 `/api/open/v1/**`、不实现 Partner 过滤器 |

| **clover** | **零改动**。不参与 Partner Token / 注册 |

| **open-api-service** | 本任务**不修改**其业务实现；仅经网关路由与可选 Feign introspect |

| Token 签发 | **禁止**在网关签发；仅 **open-api-service** |



公网 Partner 流量**仅**经本服务进入，再路由至 `open-api-service`。



## 职责



| 负责 | 不负责 |

|------|--------|

| TLS / 路由 `/oauth/token`、`/api/open/v1/**` | Token 签发 |

| Partner Token 校验（Redis 或 introspect） | Partner 注册 / 业务 |

| capability 拦截 G4–G5 | SVMP 调用 |

| 注入 `X-Partner-Id`、`X-Request-Id` | Webhook 出站 |



## 模块结构



```text

partner-gateway/

├── pom.xml

├── config/nacos-partner-gateway-sample.yaml

├── scripts/curl-partner-gateway-acceptance.sh

├── src/main/java/com/vtc/openplatform/gateway/

│   ├── ApplicationStart.java

│   ├── PartnerGatewayConstants.java

│   ├── PartnerCapability.java / PartnerCapabilityMatcher.java

│   ├── PartnerTokenResolver.java

│   ├── PartnerRateLimiter.java          # G7，默认关

│   ├── config/PartnerGatewayProperties.java

│   ├── support/OpenApiErrorWriter.java

│   ├── filter/PartnerAuthFilter.java    # G1–G6

│   └── infra/feign/IOpenApiIntrospectClient.java

└── src/main/resources/

    ├── bootstrap.yml

    └── application.yml

```



## 配置开关



| 配置项 | 默认 | 说明 |

|--------|------|------|

| `partner.gateway.enabled` | **false** | 本地/骨架默认关闭；**生产在 Nacos 设为 true** |

| `partner.gateway.introspect-mode` | `redis` | `feign` 时降级调用 open-api-service introspect |

| `partner.gateway.strip-authorization` | `true` | 鉴权通过后剥离 `Authorization`，防业务层误解析 |

| `partner.gateway.rate-limit.enabled` | `false` | G7 限流（P1 backlog） |



开启示例（Nacos `partner-gateway.yaml`）：



```yaml

partner:

  gateway:

    enabled: true

    introspect-mode: redis

```



完整路由 + 网关配置见 [`config/nacos-partner-gateway-sample.yaml`](config/nacos-partner-gateway-sample.yaml)。



## Nacos 路由（公网）



| 序号 | 路径 | 目标 | Partner 鉴权 |

|------|------|------|--------------|

| R1 | `/api/open/v1/**` | `lb://open-api-service` | ✓ |

| R2 | `/oauth/token` | `lb://open-api-service` | ✗ 白名单 |

| R3 | `/api/open/v1/oauth/token` | `lb://open-api-service` | ✗ 白名单 |

| — | `/internal/**` | **禁止公网** | 网关直接 40101 |



## PartnerAuthFilter（G1–G6）



| 步骤 | 编号 | 行为 | 失败 code |

|------|------|------|-----------|

| 路径 | G1 | 仅 `/api/open/v1/**`；`/oauth/token` 等白名单跳过 | — |

| Token | G2 | Redis `partner:token:{sha256}` 或 Feign introspect | 40101 |

| 主体 | G3 | `subjectType == PARTNER` | 40101 |

| 能力 | G4–G5 | `PartnerCapabilityMatcher` | 40301 |

| 注入 | G6 | `X-Partner-Id`、`X-Request-Id`；标记已鉴权防重复拒绝 | — |



错误体（HTTP 200，与 external §9 一致）：



```json

{ "code": 40101, "message": "...", "data": null, "requestId": "req-..." }

```



## Token 校验（对齐 open-api-service：JWTUtils.create + Redis）

签发侧（open-api-service）：

1. `JWTUtils.create(claimsJson, ttlMs)`，claims 含 `sub`、`typ=partner`、`capabilities`、`clientId`
2. Redis 写入 `partner:token:{sha256(accessToken)}` → `PartnerTokenIntrospectResponse` JSON

网关侧（`partner.gateway.introspect-mode`）：

| 模式 | 流程 |
|------|------|
| **redis**（默认） | `JWTUtils.isValidate` → 读 Redis 上下文 → 校验 `partnerId` 与 JWT 一致 |
| **feign** | 本地 JWT 校验（可关）→ `POST /internal/token/introspect` |

配置：

```yaml
partner:
  gateway:
    introspect-mode: redis
    jwt-validate-enabled: true   # 与 open-api-service 同 JWT 密钥（jjwt，不引入 spore-starter-core）
```

网关使用 **jjwt** 校验，算法/密钥与 `JWTUtils` 一致，**不**依赖 `spore-starter-core`（避免 reactive 网关加载 Servlet 配置失败）。



### 手动 Redis 测试数据（仅联调骨架；须真实 JWT 或关闭 jwt-validate-enabled）

> 开启 `jwt-validate-enabled: true` 时，仅写 Redis **不够**，Token 须为 open-api-service 签发的合法 JWT。



```bash

TOKEN="test-access-token-partner-01"

HASH=$(printf '%s' "$TOKEN" | sha256sum | awk '{print $1}')

redis-cli SET "partner:token:${HASH}" \

  '{"subjectType":"PARTNER","partnerId":"partner-test-01","capabilities":["TASK_READ"],"clientId":"cid-1","expiresAt":9999999999}'

```



仅 `TASK_READ` 的 Partner：对 `POST /api/open/v1/tasks` 应返回 **40301**。  

capabilities 含 `TASK_WRITE` 时，请求应转发且下游收到 `X-Partner-Id`。



## 构建说明

- **不继承** `esmp-support` 父 POM（避免 Springfox / MyBatis / 数据源等冲突）
- BOM 与 **morningglory** 对齐：`spring-boot 2.6.3` + `spring-cloud 2021.0.1` + `spring-cloud-alibaba 2021.0.1.0`
- 仅引入 Gateway、Nacos、OpenFeign、Redis、Fastjson、Knife4j（Swagger 聚合）

## Swagger / Knife4j（对齐 morningglory）

| 项 | 说明 |
|----|------|
| 访问地址 | `http://{host}:35770/doc.html` 或 `swagger-ui.html` |
| 开放环境 | `knife4j.show-ui-envs` 默认 `DEV`，与 Nacos `spring.application.env` 一致时可见 |
| 聚合规则 | 从 Gateway 路由发现下游服务，文档 URL：`/{serviceId}/v2/api-docs` |
| 配置 | `springfox.documentation.auto-startup: false`（避免 reactive 网关 ServletContext 冲突） |

下游 `open-api-service` 需已暴露 `/v2/api-docs`；网关路由 `Path=/open-api-service/**` 已内置。

## 编译

```bash
mvn compile -DskipTests
```



## P0 验收清单



| # | 场景 | 期望 |

|---|------|------|

| 1 | 无 `Authorization` → `POST /api/open/v1/tasks` | `code=40101` |

| 2 | 有效 Token，capabilities 仅 `TASK_READ` → `POST .../tasks` | `code=40301` |

| 3 | 有效 Token + `TASK_WRITE` → `POST .../tasks` | 转发至 open-api-service，请求头含 `X-Partner-Id` |

| 4 | `POST /oauth/token`（无 Bearer） | 免 Partner 鉴权，到达 open-api-service |

| 5 | `mvn compile -DskipTests` | BUILD SUCCESS |



脚本：[`scripts/curl-partner-gateway-acceptance.sh`](scripts/curl-partner-gateway-acceptance.sh)



```bash

# 1) 启动 partner-gateway（Nacos 中 partner.gateway.enabled=true）

# 2) 验收 40101

GW=http://127.0.0.1:35770 bash scripts/curl-partner-gateway-acceptance.sh



# 3) 验收 40301 / 转发（需 Token 或 Redis 手动数据）

ACCESS_TOKEN=... READ_ONLY_TOKEN=... bash scripts/curl-partner-gateway-acceptance.sh

```



### curl 示例



```bash

# 40101：无 Token

curl -s -X POST "http://127.0.0.1:35770/api/open/v1/tasks" \

  -H "Content-Type: application/json" \

  -d '{"extTaskId":"e1","taskName":"t"}'



# 40301：仅 TASK_READ（READ_ONLY_TOKEN 来自 Redis 仅 TASK_READ 的测试数据）

curl -s -X POST "http://127.0.0.1:35770/api/open/v1/tasks" \

  -H "Authorization: Bearer ${READ_ONLY_TOKEN}" \

  -H "Content-Type: application/json" \

  -d '{"extTaskId":"e2","taskName":"t"}'



# 转发：含 TASK_WRITE

curl -s -X POST "http://127.0.0.1:35770/api/open/v1/tasks" \

  -H "Authorization: Bearer ${ACCESS_TOKEN}" \

  -H "X-Request-Id: req-demo-001" \

  -H "Content-Type: application/json" \

  -d '{"extTaskId":"e3","taskName":"t"}'

```



## Backlog（P1+，未在本迭代实现）



| 项 | 编号 | 说明 |

|----|------|------|

| 按 Partner QPS 限流 | G7 | `partner.gateway.rate-limit.enabled=true`，失败 `42901` |

| HMAC 签名校验 | G8 | `X-Api-Key` + `X-Signature` + `X-Timestamp` |

| IP 白名单 / mTLS | G9 | 连接层策略 |



## 相关文档



- [组件职责与接口映射 §4](../../../svmp/docs/internal/组件职责与接口映射.md)

- [partner-gateway 与 open-api-service 清单 §2](../../../svmp/docs/internal/partner-gateway与open-api-service-模块与接口清单.md)

- [开放平台 API 接口规范 §3、§9](../../../svmp/docs/external/开放平台API接口规范.md)

