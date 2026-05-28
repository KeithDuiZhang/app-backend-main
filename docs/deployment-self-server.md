# 个人服务器自动部署说明

## 部署目标

- 后端仓库：`D:\Code_Project\love-match-backend`
- 前端仓库：`D:\Code_Project\love-match-frontend-main`
- 正式域名：`https://translation.superpowersai.cn`
- 后端容器端口：`48080`
- Nginx 对外端口：`80`、`443`
- 生产 API：`/admin-api`
- App API / 支付回调：`/app-api`

生产环境不要写死 `localhost`、`127.0.0.1`、服务器 IP 或花生壳地址。花生壳地址只允许出现在本地开发说明或 dev/local 配置中。

## 已生成的部署文件

- `deploy/scripts/init-server-env.sh`：初始化 Ubuntu 服务器，安装或检查 Docker、Docker Compose、Node.js、npm、JDK 17、Git、curl、unzip、rsync。
- `deploy/backend/Dockerfile`：使用 JDK 17 运行 `yudao-server.jar`，默认启用 `prod` profile，暴露容器端口 `48080`。
- `deploy/docker-compose.yml`：编排 MySQL 8.0、Redis 7、yudao-server、Nginx，并通过独立 Docker network 内网互通。
- `deploy/.env.example`：生产 `.env` 示例，只提交示例，不提交真实 `.env`。
- `deploy/nginx/conf.d/admin.conf`：HTTP 80 配置，支持 Vue history 路由，并反向代理 `/admin-api/`、`/app-api/`。
- `deploy/scripts/deploy.sh`：备份旧 Jar 和旧前端 dist，安装新产物，执行 `docker compose up -d --build`，输出容器状态和后端日志。
- `.github/workflows/deploy-self-server.yml`：GitHub Actions 自动部署。
- `.gitea/workflows/deploy-self-server.yml`：Gitea Actions 自动部署。
- `yudao-server/src/main/resources/application-prod.yaml`：生产环境 MySQL 使用 `mysql`，Redis 使用 `redis`，支付回调域名使用 `SERVER_DOMAIN`。

## 服务器首次初始化

首次在服务器上准备部署目录：

```bash
mkdir -p /opt/translation
cd /opt/translation
```

复制 `deploy/` 目录到服务器后执行：

```bash
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
SERVER_DOMAIN=translation.superpowersai.cn
```

MySQL 和 Redis 不安装到宿主机，由 Docker Compose 容器运行。

## GitHub / Gitea Secrets

GitHub Actions 和 Gitea Actions 使用相同的敏感变量名：

- `SERVER_HOST`：服务器公网 IP 或域名。
- `SERVER_PORT`：SSH 端口，默认通常是 `22`。
- `SERVER_USER`：SSH 用户。
- `SERVER_SSH_KEY`：可登录服务器的私钥内容。
- `DEPLOY_DIR`：服务器部署目录，例如 `/opt/translation`。

可选仓库变量：

- `FRONTEND_REPOSITORY`：前端仓库，默认 `KeithDuiZhang/love-match-frontend-main`。
- `FRONTEND_REF`：前端分支，默认 `main`。

服务器需要把对应公钥加入部署用户的 `~/.ssh/authorized_keys`。CI 使用 SSH key 部署，不使用密码登录。

## 自动部署触发

推送后端仓库的 `main` 或 `test` 分支会触发自动部署：

```bash
git push origin main
git push origin test
```

也可以在 GitHub Actions 或 Gitea Actions 页面手动触发 `Deploy Self Server`。

CI 会执行：

1. 构建后端 `yudao-server.jar`。
2. 构建前端 vben `apps/web-antd`。
3. 整理产物到 `release/backend/yudao-server.jar` 和 `release/frontend`。
4. 通过 SSH 上传 `deploy/` 和 `release/`。
5. 在服务器执行 `scripts/deploy.sh`。

## 手动部署

如果不用 CI，也可以把产物放到服务器：

```text
/opt/translation/release/backend/yudao-server.jar
/opt/translation/release/frontend/index.html
```

然后执行：

```bash
cd /opt/translation
chmod +x scripts/*.sh
./scripts/deploy.sh
```

`deploy.sh` 会拒绝部署过小的后端 Jar，避免把非可执行 thin jar 推到生产环境。

## 查看日志和状态

查看容器状态：

```bash
cd /opt/translation
docker compose --env-file .env ps
```

查看后端日志：

```bash
cd /opt/translation
docker compose --env-file .env logs -f --tail=200 yudao-server
```

查看 Nginx 日志：

```bash
cd /opt/translation
docker compose --env-file .env logs -f --tail=200 nginx
```

## 回滚上一版

后端 Jar 备份目录：

```text
/opt/translation/backups/backend/
```

前端 dist 备份目录：

```text
/opt/translation/backups/frontend/
```

回滚示例：

```bash
cd /opt/translation
cp backups/backend/yudao-server.jar.YYYYMMDDHHMMSS backend/yudao-server.jar
rm -rf nginx/html/admin
mkdir -p nginx/html/admin
tar -xzf backups/frontend/admin-dist.YYYYMMDDHHMMSS.tar.gz -C nginx/html/admin
docker compose --env-file .env up -d --build
docker compose --env-file .env ps
```

## 验收方式

前端成功：

```bash
curl -I https://translation.superpowersai.cn
```

返回 `200` 或 Nginx 正常响应，并且浏览器能打开管理端页面。

后端成功：

```bash
curl -I https://translation.superpowersai.cn/admin-api/
```

接口路径存在反向代理响应，`yudao-server` 容器保持 `running`。

MySQL 成功：

```bash
docker compose --env-file .env exec mysql mysqladmin ping -uroot -p
```

Redis 成功：

```bash
docker compose --env-file .env exec redis redis-cli -a "$REDIS_PASSWORD" ping
```

支付回调生产域名统一使用：

```text
https://translation.superpowersai.cn/app-api/pay/notify/wechat
https://translation.superpowersai.cn/app-api/pay/notify/alipay
```

具体路径以项目实际接口为准，但域名必须是 `translation.superpowersai.cn`。

## Cloudflare SSL/TLS

Cloudflare 中 `translation.superpowersai.cn` 已经开启 Proxied 后，推荐：

1. 优先使用 `Full (strict)`。
2. 源站证书可以使用 Cloudflare Origin Certificate，也可以使用 Let's Encrypt 证书。
3. 证书和私钥放到服务器 `/opt/translation/nginx/cert/`，当前 Nginx 配置使用：
   - `/etc/nginx/cert/origin-fullchain.pem`
   - `/etc/nginx/cert/origin-privkey.pem`
4. 执行：

```bash
cd /opt/translation
docker compose --env-file .env restart nginx
```

如果 Cloudflare 到源站 443 出现 `525 SSL handshake failed`，但源站本机和其它外部服务器直连 443 都正常，优先检查是否存在运营商或机房链路对特定 SNI 的 TLS reset。本服务器最终采用 Cloudflare Tunnel 绕过入站 443 回源：

```bash
cd /opt/translation
# 服务器 .env 中设置真实 CLOUDFLARED_TOKEN 后，部署脚本会自动启用 tunnel profile
docker compose --env-file .env --profile tunnel up -d cloudflared
docker compose --env-file .env --profile tunnel ps
```

Cloudflare DNS 中 `translation.superpowersai.cn` 和 `git.superpowersai.cn` 应指向 Tunnel 的 `*.cfargotunnel.com` CNAME，并保持 Proxied。Tunnel 使用内网访问 `http://nginx:80`，公网用户仍访问 `https://translation.superpowersai.cn`。
