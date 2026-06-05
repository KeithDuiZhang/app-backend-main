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

if [ ! -f ".env" ]; then
  echo "[deploy] ERROR: ${DEPLOY_ROOT}/.env does not exist. Copy .env.example to .env and set real values first." >&2
  exit 1
fi

read_env_value() {
  local key="$1"
  awk -F= -v key="${key}" '
    $1 ~ "^[[:space:]]*" key "[[:space:]]*$" {
      value=$0
      sub(/^[^=]*=/, "", value)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      print value
      exit
    }
  ' .env
}

FRONTDOOR_MODE="${FRONTDOOR_MODE:-$(read_env_value FRONTDOOR_MODE)}"
FRONTDOOR_MODE="${FRONTDOOR_MODE:-docker-host-nginx}"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-$(read_env_value PUBLIC_BASE_URL)}"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL:-https://translate.kunqiongai.com}"

case "${FRONTDOOR_MODE}" in
  host-nginx|docker-host-nginx|container-nginx) ;;
  *)
    echo "[deploy] ERROR: FRONTDOOR_MODE must be host-nginx, docker-host-nginx, or container-nginx, got: ${FRONTDOOR_MODE}" >&2
    exit 1
    ;;
esac

echo "[deploy] Creating required directories"
mkdir -p backend nginx/html/admin nginx/conf.d nginx/cert mysql redis logs/yudao-server backups/backend backups/frontend
docker network inspect translation-net >/dev/null 2>&1 || docker network create translation-net >/dev/null

if [ "${FRONTDOOR_MODE}" = "container-nginx" ] && { [ ! -f nginx/cert/origin-selfsigned.crt ] || [ ! -f nginx/cert/origin-selfsigned.key ]; }; then
  echo "[deploy] Creating temporary self-signed origin certificate for Cloudflare Full mode"
  openssl req -x509 -nodes -newkey rsa:2048 -days 3650 \
    -keyout nginx/cert/origin-selfsigned.key \
    -out nginx/cert/origin-selfsigned.crt \
    -subj "/CN=translate.kunqiongai.com" \
    -addext "subjectAltName=DNS:translate.kunqiongai.com" >/dev/null 2>&1
fi

COMPOSE_ARGS=(--env-file .env)
COMPOSE_SERVICES=(mysql redis yudao-server)
if [ "${FRONTDOOR_MODE}" = "container-nginx" ]; then
  COMPOSE_ARGS+=(--profile container-nginx)
  COMPOSE_SERVICES+=(nginx)
fi

if [ "${FRONTDOOR_MODE}" = "container-nginx" ] && awk -F= '/^[[:space:]]*CLOUDFLARED_TOKEN[[:space:]]*=/{gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); if ($2 != "" && $2 != "please_change_me") found=1} END{exit found ? 0 : 1}' .env; then
  echo "[deploy] Cloudflare Tunnel token detected; enabling tunnel profile"
  COMPOSE_ARGS+=(--profile tunnel)
  COMPOSE_SERVICES+=(cloudflared)
elif [ "${FRONTDOOR_MODE}" = "host-nginx" ]; then
  echo "[deploy] Host Nginx frontdoor mode; compose nginx and cloudflared are disabled"
elif [ "${FRONTDOOR_MODE}" = "docker-host-nginx" ]; then
  echo "[deploy] Docker host-network Nginx frontdoor mode; compose nginx and cloudflared are disabled"
else
  echo "[deploy] Cloudflare Tunnel token not set; tunnel profile disabled"
fi

reload_host_nginx() {
  local source_conf="${HOST_NGINX_CONF_SOURCE:-${DEPLOY_ROOT}/nginx/host/translate.kunqiongai.com.conf}"
  local target_conf="${HOST_NGINX_CONF_TARGET:-/etc/nginx/conf.d/translate.kunqiongai.com.conf}"
  local install_conf="${HOST_NGINX_INSTALL_CONF:-true}"

  if ! command -v nginx >/dev/null 2>&1; then
    echo "[deploy] ERROR: host nginx command not found. Install Nginx or use FRONTDOOR_MODE=container-nginx." >&2
    exit 1
  fi

  if [ "${install_conf}" = "true" ]; then
    if [ ! -f "${source_conf}" ]; then
      echo "[deploy] ERROR: host nginx config not found: ${source_conf}" >&2
      exit 1
    fi
    echo "[deploy] Installing host nginx config"
    install -d "$(dirname "${target_conf}")"
    install -m 0644 "${source_conf}" "${target_conf}"
  else
    echo "[deploy] Host nginx config install disabled; reloading existing config"
  fi

  echo "[deploy] Testing host nginx config"
  nginx -t

  echo "[deploy] Reloading host nginx"
  if command -v systemctl >/dev/null 2>&1 && systemctl list-unit-files nginx.service >/dev/null 2>&1; then
    systemctl reload nginx
  else
    nginx -s reload
  fi
}

reload_docker_host_nginx() {
  local container_name="${DOCKER_HOST_NGINX_CONTAINER:-kunqiong-nginx-frontdoor}"

  if ! docker ps --format '{{.Names}}' | grep -qx "${container_name}"; then
    echo "[deploy] ERROR: Docker host-network nginx container is not running: ${container_name}" >&2
    exit 1
  fi

  echo "[deploy] Testing docker host-network nginx config"
  docker exec "${container_name}" nginx -t

  echo "[deploy] Restarting docker host-network nginx to refresh bind-mounted frontend assets"
  docker restart "${container_name}" >/dev/null
}

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

echo "[deploy] Starting Docker services (${FRONTDOOR_MODE})"
docker compose "${COMPOSE_ARGS[@]}" up -d --build "${COMPOSE_SERVICES[@]}"

if [ "${FRONTDOOR_MODE}" = "container-nginx" ]; then
  echo "[deploy] Restarting container nginx to refresh upstream Docker DNS"
  docker compose "${COMPOSE_ARGS[@]}" restart nginx
else
  echo "[deploy] Stopping old compose frontdoor services if present"
  docker compose --env-file .env --profile container-nginx --profile tunnel stop cloudflared nginx >/dev/null 2>&1 || true
  if [ "${FRONTDOOR_MODE}" = "host-nginx" ]; then
    reload_host_nginx
  else
    reload_docker_host_nginx
  fi
fi

echo "[deploy] Docker service status"
docker compose "${COMPOSE_ARGS[@]}" ps

echo "[deploy] Recent yudao-server logs"
docker compose "${COMPOSE_ARGS[@]}" logs --tail=100 yudao-server

if ! docker compose "${COMPOSE_ARGS[@]}" ps --status running --services | grep -qx "yudao-server"; then
  echo "[deploy] ERROR: yudao-server is not running" >&2
  exit 1
fi

echo "[deploy] Deployment completed"
echo "${PUBLIC_BASE_URL}"
echo "${PUBLIC_BASE_URL}/admin-api/"
