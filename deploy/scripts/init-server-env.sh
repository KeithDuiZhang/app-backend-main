#!/usr/bin/env bash
set -e

NODE_VERSION="20.20.2"
NPM_VERSION="10.8.2"

echo "[init] Detecting operating system"
. /etc/os-release
OS_ID="${ID:-}"
OS_LIKE="${ID_LIKE:-}"
PACKAGE_MANAGER=""

if command -v apt-get >/dev/null 2>&1; then
  PACKAGE_MANAGER="apt"
elif command -v dnf >/dev/null 2>&1; then
  PACKAGE_MANAGER="dnf"
elif command -v yum >/dev/null 2>&1; then
  PACKAGE_MANAGER="yum"
else
  echo "[init] ERROR: supported package manager not found. Need apt, dnf, or yum." >&2
  exit 1
fi

echo "[init] OS: ${PRETTY_NAME:-unknown}; package manager: ${PACKAGE_MANAGER}"

install_base_tools_apt() {
  echo "[init] Updating apt metadata"
  export DEBIAN_FRONTEND=noninteractive
  apt-get update

  echo "[init] Installing base tools with apt"
  apt-get install -y ca-certificates curl gnupg lsb-release unzip rsync git xz-utils openssl openjdk-17-jdk
}

install_docker_apt() {
  echo "[init] Installing Docker apt repository"
  install -m 0755 -d /etc/apt/keyrings
  if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  fi
  chmod a+r /etc/apt/keyrings/docker.gpg
  . /etc/os-release
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" > /etc/apt/sources.list.d/docker.list
  apt-get update

  echo "[init] Installing Docker 28.x and Docker Compose 2.32.x when available"
  DOCKER_VERSION="$(apt-cache madison docker-ce | awk '/ 5:28\./ { print $3; exit }')"
  DOCKER_CLI_VERSION="$(apt-cache madison docker-ce-cli | awk '/ 5:28\./ { print $3; exit }')"
  COMPOSE_VERSION="$(apt-cache madison docker-compose-plugin | awk '/ 2\.32\./ { print $3; exit }')"

  if [ -n "${DOCKER_VERSION}" ] && [ -n "${DOCKER_CLI_VERSION}" ]; then
    echo "[init] Found Docker package ${DOCKER_VERSION}"
    apt-get install -y "docker-ce=${DOCKER_VERSION}" "docker-ce-cli=${DOCKER_CLI_VERSION}" containerd.io docker-buildx-plugin
  else
    echo "[init] Docker 28.x package not found; installing closest stable Docker package"
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin
  fi

  if [ -n "${COMPOSE_VERSION}" ]; then
    echo "[init] Found Docker Compose plugin ${COMPOSE_VERSION}"
    apt-get install -y "docker-compose-plugin=${COMPOSE_VERSION}"
  else
    echo "[init] Docker Compose 2.32.x package not found; installing closest stable compose plugin"
    apt-get install -y docker-compose-plugin
  fi
}

install_base_tools_rpm() {
  echo "[init] Installing base tools with ${PACKAGE_MANAGER}"
  "${PACKAGE_MANAGER}" install -y ca-certificates curl gnupg unzip rsync git xz openssl java-17-openjdk java-17-openjdk-devel dnf-plugins-core || \
    "${PACKAGE_MANAGER}" install -y ca-certificates curl gnupg unzip rsync git xz openssl java-17-openjdk java-17-openjdk-devel yum-utils
}

install_docker_rpm() {
  echo "[init] Installing Docker rpm repository"
  if command -v dnf >/dev/null 2>&1; then
    dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo || true
    PKG_CMD="dnf"
  else
    yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo || true
    PKG_CMD="yum"
  fi

  echo "[init] Installing Docker 28.x and Docker Compose 2.32.x when available"
  DOCKER_VERSION="$(${PKG_CMD} --showduplicates list docker-ce 2>/dev/null | awk '/28\./ { print $2; exit }')"
  DOCKER_CLI_VERSION="$(${PKG_CMD} --showduplicates list docker-ce-cli 2>/dev/null | awk '/28\./ { print $2; exit }')"
  COMPOSE_VERSION="$(${PKG_CMD} --showduplicates list docker-compose-plugin 2>/dev/null | awk '/2\.32\./ { print $2; exit }')"

  if [ -n "${DOCKER_VERSION}" ] && [ -n "${DOCKER_CLI_VERSION}" ]; then
    echo "[init] Found Docker package ${DOCKER_VERSION}"
    "${PKG_CMD}" install -y "docker-ce-${DOCKER_VERSION}" "docker-ce-cli-${DOCKER_CLI_VERSION}" containerd.io docker-buildx-plugin
  else
    echo "[init] Docker 28.x package not found; installing closest stable Docker package"
    "${PKG_CMD}" install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin
  fi

  if [ -n "${COMPOSE_VERSION}" ]; then
    echo "[init] Found Docker Compose plugin ${COMPOSE_VERSION}"
    "${PKG_CMD}" install -y "docker-compose-plugin-${COMPOSE_VERSION}"
  else
    echo "[init] Docker Compose 2.32.x package not found; installing closest stable compose plugin"
    "${PKG_CMD}" install -y docker-compose-plugin
  fi
}

install_node_binary() {
  echo "[init] Installing Node.js ${NODE_VERSION}"
  MACHINE="$(uname -m)"
  case "${MACHINE}" in
    x86_64 | amd64) NODE_ARCH="x64" ;;
    aarch64 | arm64) NODE_ARCH="arm64" ;;
    *) echo "[init] Unsupported architecture for direct Node install: ${MACHINE}"; NODE_ARCH="" ;;
  esac

  if [ -n "${NODE_ARCH}" ]; then
    NODE_TARBALL="node-v${NODE_VERSION}-linux-${NODE_ARCH}.tar.xz"
    NODE_URL="https://nodejs.org/dist/v${NODE_VERSION}/${NODE_TARBALL}"
    if curl -fsSL --head "${NODE_URL}" >/dev/null; then
      rm -rf "/usr/local/lib/nodejs/node-v${NODE_VERSION}-linux-${NODE_ARCH}"
      mkdir -p /usr/local/lib/nodejs
      curl -fsSL "${NODE_URL}" -o "/tmp/${NODE_TARBALL}"
      tar -xJf "/tmp/${NODE_TARBALL}" -C /usr/local/lib/nodejs
      ln -sf "/usr/local/lib/nodejs/node-v${NODE_VERSION}-linux-${NODE_ARCH}/bin/node" /usr/local/bin/node
      ln -sf "/usr/local/lib/nodejs/node-v${NODE_VERSION}-linux-${NODE_ARCH}/bin/npm" /usr/local/bin/npm
      ln -sf "/usr/local/lib/nodejs/node-v${NODE_VERSION}-linux-${NODE_ARCH}/bin/npx" /usr/local/bin/npx
      ln -sf "/usr/local/lib/nodejs/node-v${NODE_VERSION}-linux-${NODE_ARCH}/bin/corepack" /usr/local/bin/corepack
      return
    fi
  fi

  echo "[init] Node.js ${NODE_VERSION} binary not found; installing closest stable Node.js 20.x"
  if [ "${PACKAGE_MANAGER}" = "apt" ]; then
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
    apt-get install -y nodejs
  else
    curl -fsSL https://rpm.nodesource.com/setup_20.x | bash -
    "${PACKAGE_MANAGER}" install -y nodejs
  fi
}

if [ "${PACKAGE_MANAGER}" = "apt" ]; then
  install_base_tools_apt
  install_docker_apt
else
  install_base_tools_rpm
  install_docker_rpm
fi

echo "[init] Enabling Docker service"
systemctl enable --now docker

install_node_binary

echo "[init] Installing npm ${NPM_VERSION}"
npm install -g "npm@${NPM_VERSION}"

echo "[init] Installed versions"
docker --version
docker compose version
node -v
npm -v
java -version
git --version
