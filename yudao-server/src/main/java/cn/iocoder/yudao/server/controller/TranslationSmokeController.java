package cn.iocoder.yudao.server.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/translation")
public class TranslationSmokeController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/smoke")
    @PermitAll
    public CommonResult<Map<String, Object>> smoke() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "translation-app");
        data.put("schemaVersion", "2026-05-20-v8");
        for (String table : List.of(
                "app_user",
                "app_language_pack",
                "app_capability_matrix",
                "app_token_product",
                "app_offline_membership_product",
                "app_translation_record",
                "app_feedback")) {
            data.put(table, count(table));
        }
        return CommonResult.success(data);
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE deleted = 0", Long.class);
    }

}
