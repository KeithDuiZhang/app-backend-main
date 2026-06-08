package cn.iocoder.yudao.server.service.image.provider;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.BoxRespVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.TextItemRespVO;
import cn.iocoder.yudao.server.service.integration.AppIntegrationConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AliyunImageTextOcrClient {

    private static final String ACTION = "RecognizeGeneral";
    private static final String API_VERSION = "2021-07-07";
    private static final String CONTENT_TYPE = "application/octet-stream";
    private static final String DEFAULT_REGION = "cn-shanghai";

    @Resource
    private AppIntegrationConfigService integrationConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public OcrDetail recognize(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw ServiceExceptionUtil.invalidParamException("图片读取失败，请稍后重试");
        }
        String accessKeyId = first(
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_OCR_ACCESS_KEY_ID),
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_ACCESS_KEY_ID));
        String accessKeySecret = first(
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_OCR_ACCESS_KEY_SECRET),
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_ACCESS_KEY_SECRET));
        if (StrUtil.hasBlank(accessKeyId, accessKeySecret)) {
            throw ServiceExceptionUtil.invalidParamException("图片文字提取暂未开通，请稍后再试");
        }
        String host = "ocr-api." + StrUtil.blankToDefault(
                integrationConfigService.getPlain(AppIntegrationConfigService.ALIYUN_OCR_REGION), DEFAULT_REGION).trim()
                + ".aliyuncs.com";
        try {
            Map<String, String> headers = AliyunOpenApiSigner.acs3Headers(
                    "POST", "/", "", imageBytes, CONTENT_TYPE, ACTION, API_VERSION,
                    accessKeyId, accessKeySecret, host);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://" + host + "/"))
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(imageBytes));
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if ("host".equalsIgnoreCase(entry.getKey())) {
                    continue;
                }
                if ("content-type".equalsIgnoreCase(entry.getKey())) {
                    builder.header("Content-Type", entry.getValue());
                    continue;
                }
                builder.header(entry.getKey(), entry.getValue());
            }
            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw userFailure("Http" + response.statusCode(), response.body());
            }
            return parse(response.body());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("image text OCR request failed type={}", ex.getClass().getSimpleName());
            throw ServiceExceptionUtil.invalidParamException("图片文字提取失败，请稍后重试");
        }
    }

    private OcrDetail parse(String body) throws Exception {
        JsonNode root = StrUtil.isBlank(body) ? objectMapper.createObjectNode() : objectMapper.readTree(body);
        String code = text(root, "Code");
        if (StrUtil.isNotBlank(code) && !"200".equals(code)) {
            throw userFailure(code, text(root, "Message"));
        }
        JsonNode data = root.path("Data");
        if (data.isTextual()) {
            data = objectMapper.readTree(data.asText("{}"));
        }
        OcrDetail detail = new OcrDetail();
        detail.setRequestId(text(root, "RequestId"));
        detail.setRawText(text(data, "content").trim());
        detail.setItems(parseItems(data.path("prism_wordsInfo")));
        if (detail.getRawText().isEmpty()) {
            detail.setRawText(joinSource(detail.getItems()));
        }
        if (detail.getItems().isEmpty() && StrUtil.isNotBlank(detail.getRawText())) {
            TextItemRespVO item = new TextItemRespVO();
            item.setSourceText(detail.getRawText());
            item.setTranslatedText("");
            item.setConfidence(1.0d);
            detail.setItems(Collections.singletonList(item));
        }
        return detail;
    }

    private List<TextItemRespVO> parseItems(JsonNode words) {
        if (words == null || !words.isArray()) {
            return new ArrayList<>();
        }
        ArrayList<TextItemRespVO> items = new ArrayList<>();
        for (JsonNode word : words) {
            String source = firstText(word, "word", "text").trim();
            if (source.isEmpty()) {
                continue;
            }
            TextItemRespVO item = new TextItemRespVO();
            item.setSourceText(source);
            item.setTranslatedText("");
            item.setConfidence(normalizeConfidence(number(word, 1.0d, "prob", "confidence", "score")));
            item.setBox(parseBox(word));
            items.add(item);
        }
        return items;
    }

    private BoxRespVO parseBox(JsonNode node) {
        ArrayList<Double> points = parsePoints(firstNode(node, "pos", "points", "box"));
        if (!points.isEmpty()) {
            return boxFromPoints(points);
        }
        JsonNode boxNode = firstNode(node, "box", "bbox", "boundingBox");
        double x = number(node, Double.NaN, "x", "left", "Left");
        double y = number(node, Double.NaN, "y", "top", "Top");
        double width = number(node, Double.NaN, "width", "w", "Width");
        double height = number(node, Double.NaN, "height", "h", "Height");
        if (boxNode != null && boxNode.isObject()) {
            x = Double.isNaN(x) ? number(boxNode, Double.NaN, "x", "left", "Left") : x;
            y = Double.isNaN(y) ? number(boxNode, Double.NaN, "y", "top", "Top") : y;
            width = Double.isNaN(width) ? number(boxNode, Double.NaN, "width", "w", "Width") : width;
            height = Double.isNaN(height) ? number(boxNode, Double.NaN, "height", "h", "Height") : height;
        }
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(width) || Double.isNaN(height)
                || width <= 0d || height <= 0d) {
            return null;
        }
        BoxRespVO box = new BoxRespVO();
        box.setX(x);
        box.setY(y);
        box.setWidth(width);
        box.setHeight(height);
        box.setPoints(List.of(x, y, x + width, y, x + width, y + height, x, y + height));
        return box;
    }

    private ArrayList<Double> parsePoints(JsonNode positions) {
        ArrayList<Double> points = new ArrayList<>();
        if (positions == null || !positions.isArray()) {
            return points;
        }
        for (JsonNode point : positions) {
            if (point.isObject()) {
                points.add(number(point, 0d, "x", "X"));
                points.add(number(point, 0d, "y", "Y"));
            } else if (point.isArray() && point.size() >= 2) {
                points.add(point.get(0).asDouble(0d));
                points.add(point.get(1).asDouble(0d));
            }
        }
        return points.size() >= 8 ? points : new ArrayList<>();
    }

    private BoxRespVO boxFromPoints(List<Double> points) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = 0d;
        double maxY = 0d;
        for (int i = 0; i + 1 < points.size(); i += 2) {
            double x = points.get(i);
            double y = points.get(i + 1);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        if (minX == Double.MAX_VALUE || minY == Double.MAX_VALUE || maxX <= minX || maxY <= minY) {
            return null;
        }
        BoxRespVO box = new BoxRespVO();
        box.setX(minX);
        box.setY(minY);
        box.setWidth(maxX - minX);
        box.setHeight(maxY - minY);
        box.setPoints(new ArrayList<>(points));
        return box;
    }

    private RuntimeException userFailure(String code, String message) {
        String normalized = StrUtil.blankToDefault(code, "").toLowerCase();
        log.warn("image text OCR failed code={}", StrUtil.maxLength(StrUtil.blankToDefault(code, "Unknown"), 80));
        if (normalized.contains("accesskey") || normalized.contains("signature")
                || normalized.contains("nopermission") || normalized.contains("forbidden")
                || normalized.contains("servicenotopen") || normalized.contains("servicedisabled")) {
            return ServiceExceptionUtil.invalidParamException("图片文字提取暂未开通，请稍后再试");
        }
        if (normalized.contains("image") || normalized.contains("file") || normalized.contains("body")
                || normalized.contains("parameter")) {
            return ServiceExceptionUtil.invalidParamException("图片暂时无法识别，请更换图片后重试");
        }
        return ServiceExceptionUtil.invalidParamException(StrUtil.isNotBlank(message)
                ? "图片文字提取失败，请稍后重试"
                : "图片文字提取服务暂不可用，请稍后重试");
    }

    private String joinSource(List<TextItemRespVO> items) {
        StringBuilder builder = new StringBuilder();
        for (TextItemRespVO item : items) {
            if (item == null || StrUtil.isBlank(item.getSourceText())) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(item.getSourceText().trim());
        }
        return builder.toString();
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            String value = text(node, key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String text(JsonNode node, String key) {
        if (node == null || node.isMissingNode() || !node.has(key) || node.get(key).isNull()) {
            return "";
        }
        return node.get(key).asText("");
    }

    private JsonNode firstNode(JsonNode node, String... keys) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        for (String key : keys) {
            JsonNode child = node.get(key);
            if (child != null && !child.isNull() && !child.isMissingNode()) {
                return child;
            }
        }
        return null;
    }

    private double number(JsonNode node, double defaultValue, String... keys) {
        if (node == null || node.isMissingNode()) {
            return defaultValue;
        }
        for (String key : keys) {
            JsonNode child = node.get(key);
            if (child != null && child.isNumber()) {
                return child.asDouble(defaultValue);
            }
            if (child != null && child.isTextual()) {
                try {
                    return Double.parseDouble(child.asText());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return defaultValue;
    }

    private double normalizeConfidence(double confidence) {
        if (confidence > 1d) {
            return Math.max(0d, Math.min(1d, confidence / 100d));
        }
        return Math.max(0d, Math.min(1d, confidence));
    }

    private String first(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    @Data
    public static class OcrDetail {
        private String requestId = "";
        private String rawText = "";
        private List<TextItemRespVO> items = new ArrayList<>();
    }
}
