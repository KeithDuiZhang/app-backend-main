# Love Match Local Setup

## Backend prerequisites

1. Run `sql/mysql/love-match-init.sql`.
2. Ensure OAuth2 client `love-mini-app` exists in `system_oauth2_client`.
3. In the admin pay app management page, create or verify app key `love-mini-app`.
4. In the admin pay channel management page, configure WeChat Mini Program pay with the real merchant settings.
5. Verify `wx.miniapp.appid` and `wx.miniapp.secret` in `application-local.yaml`.

## Verification commands

```powershell
& 'D:/soft_Install/apache-maven-3.9.15/bin/mvn.cmd' --% -pl yudao-module-love -am -Dtest=AppLoveAuthControllerTest,LoveAuthServiceImplTest,AppLoveAuthOrderControllerTest,LoveAuthOrderServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
& 'D:/soft_Install/apache-maven-3.9.15/bin/mvn.cmd' -pl yudao-module-love -am -DskipTests compile
```
