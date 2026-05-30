USE `ruoyi-vue-pro`;

SELECT 'app_user' AS table_name, COUNT(*) AS row_count FROM `app_user` WHERE `deleted` = 0
UNION ALL
SELECT 'app_user_identity', COUNT(*) FROM `app_user_identity` WHERE `deleted` = 0
UNION ALL
SELECT 'app_device', COUNT(*) FROM `app_device` WHERE `deleted` = 0
UNION ALL
SELECT 'app_token_account', COUNT(*) FROM `app_token_account` WHERE `deleted` = 0
UNION ALL
SELECT 'app_token_ledger', COUNT(*) FROM `app_token_ledger` WHERE `deleted` = 0
UNION ALL
SELECT 'app_token_product', COUNT(*) FROM `app_token_product` WHERE `deleted` = 0
UNION ALL
SELECT 'app_payment_order', COUNT(*) FROM `app_payment_order` WHERE `deleted` = 0
UNION ALL
SELECT 'app_offline_membership', COUNT(*) FROM `app_offline_membership` WHERE `deleted` = 0
UNION ALL
SELECT 'app_language_pack', COUNT(*) FROM `app_language_pack` WHERE `deleted` = 0
UNION ALL
SELECT 'app_capability_matrix', COUNT(*) FROM `app_capability_matrix` WHERE `deleted` = 0
UNION ALL
SELECT 'app_translation_record', COUNT(*) FROM `app_translation_record` WHERE `deleted` = 0
UNION ALL
SELECT 'app_favorite', COUNT(*) FROM `app_favorite` WHERE `deleted` = 0
UNION ALL
SELECT 'app_feedback', COUNT(*) FROM `app_feedback` WHERE `deleted` = 0
UNION ALL
SELECT 'app_content', COUNT(*) FROM `app_content` WHERE `deleted` = 0
UNION ALL
SELECT 'app_sms_verification', COUNT(*) FROM `app_sms_verification` WHERE `deleted` = 0
UNION ALL
SELECT 'app_external_auth_config', COUNT(*) FROM `app_external_auth_config` WHERE `deleted` = 0;

SELECT
  `mode`,
  `scene`,
  `status`,
  COUNT(*) AS capability_count
FROM `app_capability_matrix`
WHERE `deleted` = 0
GROUP BY `mode`, `scene`, `status`
ORDER BY `mode`, `scene`, `status`;
