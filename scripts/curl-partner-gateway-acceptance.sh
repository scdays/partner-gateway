#!/usr/bin/env bash
# partner-gateway P0 验收（默认 35770）
set -euo pipefail

GW="${GW:-http://127.0.0.1:35770}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"
READ_ONLY_TOKEN="${READ_ONLY_TOKEN:-}"

echo "=== 验收 1：无 Token → POST /api/open/v1/tasks → 40101 ==="
curl -s -X POST "${GW}/api/open/v1/tasks" \
  -H "Content-Type: application/json" \
  -d '{"extTaskId":"t1","taskName":"demo"}' | tee /tmp/pg-40101.json
echo ""
grep -q '"code"[[:space:]]*:[[:space:]]*40101' /tmp/pg-40101.json && echo "PASS 40101" || echo "FAIL: expected code 40101"

if [ -z "${ACCESS_TOKEN}" ]; then
  echo ""
  echo "跳过验收 2–4：请设置 ACCESS_TOKEN（TASK_WRITE）与 READ_ONLY_TOKEN（仅 TASK_READ）"
  echo "  或先在 Redis 写入 partner:token:{sha256(token)}，见 README「手动 Redis 测试数据」"
  exit 0
fi

echo ""
echo "=== 验收 2：有效 Token + 无 TASK_WRITE → 40301 ==="
if [ -n "${READ_ONLY_TOKEN}" ]; then
  curl -s -X POST "${GW}/api/open/v1/tasks" \
    -H "Authorization: Bearer ${READ_ONLY_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{"extTaskId":"t2","taskName":"demo"}' | tee /tmp/pg-40301.json
  echo ""
  grep -q '"code"[[:space:]]*:[[:space:]]*40301' /tmp/pg-40301.json && echo "PASS 40301" || echo "FAIL: expected code 40301"
else
  echo "跳过：未设置 READ_ONLY_TOKEN"
fi

echo ""
echo "=== 验收 3：有效 Token + TASK_WRITE → 转发（观察下游或 Mock 日志 X-Partner-Id）==="
curl -s -X POST "${GW}/api/open/v1/tasks" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: req-acceptance-001" \
  -d '{"extTaskId":"t3","taskName":"demo"}' | tee /tmp/pg-ok.json
echo ""

echo ""
echo "=== 验收 4：Token 白名单 /oauth/token 免鉴权（应到达 open-api-service，非 40101）==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "${GW}/oauth/token" \
  -H "Content-Type: application/json" \
  -d '{"grantType":"client_credentials","clientId":"x","clientSecret":"y"}'
