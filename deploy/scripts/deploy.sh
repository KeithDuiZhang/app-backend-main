#!/usr/bin/env bash
set -euo pipefail

echo "[deploy] Resolving deployment paths"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RELEASE_ROOT="${RELEASE_ROOT:-${DEPLOY_ROOT}/release}"
JAR_SOURCE="${JAR_SOURCE:-${RELEASE_ROOT}/backend/yudao-server.jar}"
FRONTEND_SOURCE="${FRONTEND_SOURCE:-${RELEASE_ROOT}/frontend}"
TIMESTAMP="$(date +%Y%m%d%H%M%S)"

cd "${DEPLOY_ROOT}"

echo "[deploy] Creating required directories"
mkdir -p backend nginx/html/admin nginx/conf.d nginx/cert mysql redis logs/yudao-server backups/backend backups/frontend
docker network inspect translation-net >/dev/null 2>&1 || docker network create translation-net >/dev/null

if [ ! -f nginx/cert/origin-selfsigned.crt ] || [ ! -f nginx/cert/origin-selfsigned.key ]; then
  echo "[deploy] Creating temporary self-signed origin certificate for Cloudflare Full mode"
  openssl req -x509 -nodes -newkey rsa:2048 -days 3650 \
    -keyout nginx/cert/origin-selfsigned.key \
    -out nginx/cert/origin-selfsigned.crt \
    -subj "/CN=translation.superpowersai.cn" \
    -addext "subjectAltName=DNS:translation.superpowersai.cn,DNS:git.superpowersai.cn" >/dev/null 2>&1
fi

if [ ! -f ".env" ]; then
  echo "[deploy] ERROR: ${DEPLOY_ROOT}/.env does not exist. Copy .env.example to .env and set real values first." >&2
  exit 1
fi

if [ ! -f "${JAR_SOURCE}" ]; then
  echo "[deploy] ERROR: backend jar not found: ${JAR_SOURCE}" >&2
  exit 1
fi

JAR_SIZE="$(wc -c < "${JAR_SOURCE}")"
if [ "${JAR_SIZE}" -lt 50000000 ]; then
  echo "[deploy] ERROR: backend jar looks too small (${JAR_SIZE} bytes). Build the executable Spring Boot jar before deploying." >&2
  exit 1
fi

if [ ! -d "${FRONTEND_SOURCE}" ] && [ ! -f "${FRONTEND_SOURCE}.zip" ]; then
  echo "[deploy] ERROR: frontend dist not found: ${FRONTEND_SOURCE}" >&2
  exit 1
fi

echo "[deploy] Backing up old backend jar"
if [ -f backend/yudao-server.jar ]; then
  cp -a backend/yudao-server.jar "backups/backend/yudao-server.jar.${TIMESTAMP}"
fi

echo "[deploy] Backing up old frontend dist"
if [ -d nginx/html/admin ] && [ -n "$(find nginx/html/admin -mindepth 1 -maxdepth 1 2>/dev/null)" ]; then
  tar -czf "backups/frontend/admin-dist.${TIMESTAMP}.tar.gz" -C nginx/html/admin .
fi

echo "[deploy] Installing new backend jar"
cp -a "${JAR_SOURCE}" backend/yudao-server.jar

echo "[deploy] Installing new frontend dist"
rm -rf nginx/html/admin.tmp
mkdir -p nginx/html/admin.tmp

if [ -f "${FRONTEND_SOURCE}.zip" ]; then
  unzip -q "${FRONTEND_SOURCE}.zip" -d nginx/html/admin.tmp
elif [ -f "${FRONTEND_SOURCE}/dist.zip" ]; then
  unzip -q "${FRONTEND_SOURCE}/dist.zip" -d nginx/html/admin.tmp
else
  rsync -a --delete "${FRONTEND_SOURCE}/" nginx/html/admin.tmp/
fi

if [ -f nginx/html/admin.tmp/dist/index.html ]; then
  rm -rf nginx/html/admin.tmp.normalized
  mv nginx/html/admin.tmp/dist nginx/html/admin.tmp.normalized
  rm -rf nginx/html/admin.tmp
  mv nginx/html/admin.tmp.normalized nginx/html/admin.tmp
fi

if [ ! -f nginx/html/admin.tmp/index.html ]; then
  echo "[deploy] ERROR: frontend dist must contain index.html" >&2
  exit 1
fi

rm -rf nginx/html/admin
mv nginx/html/admin.tmp nginx/html/admin

echo "[deploy] Starting Docker services"
docker compose --env-file .env up -d --build

echo "[deploy] Docker service status"
docker compose --env-file .env ps

echo "[deploy] Recent yudao-server logs"
docker compose --env-file .env logs --tail=100 yudao-server

if ! docker compose --env-file .env ps --status running --services | grep -qx "yudao-server"; then
  echo "[deploy] ERROR: yudao-server is not running" >&2
  exit 1
fi

echo "[deploy] Deployment completed"
echo "https://translation.superpowersai.cn"
echo "https://translation.superpowersai.cn/admin-api/"
