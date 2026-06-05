# 自托管正式服务器部署说明

## 部署目标

- 后端仓库：`D:\Code_Project\app-backend-main`
- 前端仓库：`D:\Code_Project\app-frontend-main`
- 正式服务器：`120.53.11.250:22`
- 正式域名：`https://translate.kunqiongai.com`
- 部署目录：`/opt/kunqiong-translation`
- Gitea：`http://120.53.11.250:3210`
- 后端容器端口：`48080`，默认只映射到宿主机 `127.0.0.1:48080`
- Docker host-network Nginx：对外绑定 `80/443`，服务前端并代理 `/admin-api/`、`/app-api/`

正式部署默认使用 `FRONTDOOR_MODE=docker-host-nginx`。服务器当前由 `/opt/kunqiong-frontdoor` 下的 Docker Nginx 使用 host network 接管 `80/443`；后端 Compose 不再要求业务栈里的容器 Nginx 绑定宿主机 `80/443`。宿主机包安装版 Nginx 可通过 `FRONTDOOR_MODE=host-nginx` 启用，老的业务栈容器 Nginx 可通过 `FRONTDOOR_MODE=container-nginx` 启用。

生产环境不要写死 `localhost`、`127.0.0.1`、服务器 IP 或本地穿透地址到前端配置或移动端配置中。宿主机 Nginx 访问后端使用 `127.0.0.1:48080` 是部署内网连接，不是对外业务地址。

## 前置条件

服务器需要提前具备：

- Docker 和 Docker Compose。
- Docker Nginx 前置入口 `/opt/kunqiong-frontdoor` 已创建并运行。
- `translate.kunqiongai.com` 的 TLS 证书已放在 Docker Nginx 前置入口目录：
  - `/opt/kunqiong-frontdoor/cert/translate.kunqiongai.com/fullchain.crt`
  - `/opt/kunqiong-frontdoor/cert/translate.kunqiongai.com/privkey.key`
- Apache SVN Web 调整为只监听 `127.0.0.1:8080`。`svnserve` 继续使用 `3690`，不要占用或迁移该端口。

宿主机包安装版 Nginx 配置模板位于：

```text
deploy/nginx/host/translate.kunqiongai.com.conf
```

`deploy.sh` 在 `FRONTDOOR_MODE=host-nginx` 时默认安装该配置到：

```text
/etc/nginx/conf.d/translate.kunqiongai.com.conf
```

并执行 `nginx -t` 与 reload。`FRONTDOOR_MODE=docker-host-nginx` 时，脚本只执行 `docker exec kunqiong-nginx-frontdoor nginx -t` 和 reload，不覆盖 `/opt/kunqiong-frontdoor` 配置。若服务器已有手工维护的 Host Nginx 配置，可设置：

```bash
HOST_NGINX_INSTALL_CONF=false
```

此时脚本只测试并 reload 现有 Nginx 配置。

## 服务器首次初始化

```bash
mkdir -p /opt/kunqiong-translation
cd /opt/kunqiong-translation
chmod +x scripts/*.sh
./scripts/init-server-env.sh
cp .env.example .env
```

编辑 `.env`，至少设置：

```bash
MYSQL_ROOT_PASSWORD=change_to_real_password
MYSQL_DATABASE=ruoyi-vue-pro
REDIS_PASSWORD=change_to_real_password
SPRING_PROFILES_ACTIVE=prod
SERVER_DOMAIN=translate.kunqiongai.com
FRONTDOOR_MODE=docker-host-nginx
PUBLIC_BASE_URL=https://translate.kunqiongai.com
YUDAO_SERVER_HOST_BIND=127.0.0.1
YUDAO_SERVER_HOST_PORT=48080
```

不要把真实 `.env`、SSH 私钥、数据库密码或第三方密钥提交到仓库。

## Gitea Actions

`.gitea/workflows/deploy-self-server.yml` 默认从 Gitea 内网地址克隆：

```text
http://gitea:3000/superpowersai/app-backend-main.git
http://gitea:3000/superpowersai/app-frontend-main.git
```

默认部署到：

```text
/opt/kunqiong-translation
```

需要在 Gitea Secrets 中配置：

- `SERVER_SSH_KEY_B64`：部署私钥的 base64 内容。
- `FRONTEND_REPO_TOKEN`：可读取前端仓库的令牌。

可选 Secrets：

- `SERVER_HOST`：默认 `120.53.11.250`。
- `SERVER_PORT`：默认 `22`。
- `SERVER_USER`：默认 `root`。
- `DEPLOY_DIR`：默认 `/opt/kunqiong-translation`。

工作流不会保存服务器密码；SSH 登录依赖私钥。

## 手动部署

将产物放到：

```text
/opt/kunqiong-translation/release/backend/yudao-server.jar
/opt/kunqiong-translation/release/frontend/index.html
```

执行：

```bash
cd /opt/kunqiong-translation
chmod +x scripts/*.sh
FRONTDOOR_MODE=docker-host-nginx ./scripts/deploy.sh
```

脚本会备份旧 Jar 和旧前端 dist，安装新产物，启动 MySQL、Redis、`yudao-server`，然后测试并 reload Docker Nginx 前置入口。

## 容器 Nginx 兼容模式

如需恢复旧的容器 Nginx 前门：

```bash
cd /opt/kunqiong-translation
FRONTDOOR_MODE=container-nginx ./scripts/deploy.sh
```

该模式会启用 Compose 的 `container-nginx` profile，并重启容器 Nginx。若 `.env` 中设置了 `CLOUDFLARED_TOKEN`，脚本会同时启用 tunnel profile。

## 验收

```bash
curl -I https://translate.kunqiongai.com
curl -I https://translate.kunqiongai.com/admin-api/
curl -I https://translate.kunqiongai.com/app-api/
docker compose --env-file .env ps
docker compose --env-file .env logs -f --tail=200 yudao-server
```

SVN Web 由 Docker Nginx 前置入口代理到 Apache：

```bash
curl -I https://translate.kunqiongai.com/svn/
```

Gitea 直接访问：

```text
http://120.53.11.250:3210
```
