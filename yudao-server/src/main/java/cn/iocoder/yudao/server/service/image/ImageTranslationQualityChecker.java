package cn.iocoder.yudao.server.service.image;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.server.config.ImageTranslationProperties;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.BoxRespVO;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.DisplayMode;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProviderResult;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.QualityResult;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.QualityStatus;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.TextItemRespVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class ImageTranslationQualityChecker {

    private static final String DEFAULT_WARNING = "当前图片较复杂，部分文字可能未完全识别，建议裁剪文字区域后重试。";
    private static final String FAILED_MESSAGE = "图片翻译失败，建议裁剪文字区域或重新拍摄后重试。";

    @Resource
    private ImageTranslationProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public QualityResult check(ProviderResult result) {
        List<TextItemRespVO> textItems = new ArrayList<>();
        if (result != null && result.getTextItems() != null) {
            textItems.addAll(result.getTextItems());
        }
        textItems.addAll(extractTextItems(result != null ? result.getRawResponseJson() : ""));
        List<String> reasons = new ArrayList<>();
        boolean hasFinalImage = result != null
                && (StrUtil.isNotBlank(result.getFinalImageUrl())
                || (result.getFinalImageBytes() != null && result.getFinalImageBytes().length > 0));
        if (result == null || !result.isSuccess()) {
            reasons.add(result != null && StrUtil.isNotBlank(result.getErrorCode()) ? result.getErrorCode() : "provider_failed");
        }
        if (!hasFinalImage) {
            reasons.add("final_image_empty");
        }
        int boxCount = countBoxes(textItems);
        boolean boxTooFew = boxCount < Math.max(0, properties.getQuality().getMinBoxCount());
        if (boxTooFew) {
            reasons.add("box_count_too_low");
        }
        if (hasLowConfidence(textItems)) {
            reasons.add("confidence_too_low");
        }
        if (maxOverlapRatio(textItems) > properties.getQuality().getMaxOverlapRatio()) {
            reasons.add("translated_boxes_overlap");
        }
        if (looksStacked(textItems)) {
            reasons.add("target_text_stacked");
        }

        QualityResult quality = new QualityResult().setTextItems(textItems);
        if (!properties.getQuality().isEnableQualityCheck()) {
            return quality.setStatus(QualityStatus.PASS)
                    .setDisplayMode(DisplayMode.FINAL_IMAGE)
                    .setQualityScore(1.0d)
                    .setQualityJson(toQualityJson("PASS", List.of(), textItems, 1.0d));
        }
        if (result != null && result.isSuccess() && hasFinalImage && reasons.isEmpty()) {
            return quality.setStatus(QualityStatus.PASS)
                    .setDisplayMode(DisplayMode.FINAL_IMAGE)
                    .setQualityScore(1.0d)
                    .setQualityJson(toQualityJson("PASS", reasons, textItems, 1.0d));
        }
        if (result != null && result.isSuccess() && hasFinalImage && onlySoftWarnings(reasons)) {
            return quality.setStatus(QualityStatus.WARN)
                    .setDisplayMode(DisplayMode.FINAL_IMAGE)
                    .setQualityScore(0.72d)
                    .setWarningMessage(DEFAULT_WARNING)
                    .setFailReason(String.join("|", reasons))
                    .setQualityJson(toQualityJson("WARN", reasons, textItems, 0.72d));
        }
        if (!textItems.isEmpty()) {
            return quality.setStatus(QualityStatus.FAIL_BUT_HAS_TEXT)
                    .setDisplayMode(DisplayMode.TEXT_LIST)
                    .setQualityScore(0.45d)
                    .setWarningMessage(DEFAULT_WARNING)
                    .setFailReason(String.join("|", reasons))
                    .setQualityJson(toQualityJson("FAIL_BUT_HAS_TEXT", reasons, textItems, 0.45d));
        }
        return quality.setStatus(QualityStatus.FAIL)
                .setDisplayMode(DisplayMode.FAILED)
                .setQualityScore(0d)
                .setWarningMessage(FAILED_MESSAGE)
                .setFailReason(String.join("|", reasons))
                .setQualityJson(toQualityJson("FAIL", reasons, textItems, 0d));
    }

    public List<TextItemRespVO> extractTextItems(String rawJson) {
        List<TextItemRespVO> items = new ArrayList<>();
        if (StrUtil.isBlank(rawJson)) {
            return items;
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            collectTextItems(root, items);
            JsonNode data = root.has("Data") ? root.path("Data") : root.path("data");
            for (String key : List.of("TemplateJson", "EditorData", "EditorDataJson", "Result", "result")) {
                String nested = text(data, key);
                if (StrUtil.isNotBlank(nested) && nested.trim().startsWith("{")) {
                    collectTextItems(objectMapper.readTree(nested), items);
                }
            }
        } catch (Exception ignored) {
            return items;
        }
        return dedupe(items);
    }

    private void collectTextItems(JsonNode node, List<TextItemRespVO> items) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            String source = firstText(node, "sourceText", "SourceText", "srcText", "originalText", "OriginalText", "source", "Source", "text", "Text");
            String translated = firstText(node, "translatedText", "TranslatedText", "targetText", "TargetText", "target", "Target", "translation", "Translation", "transText");
            if (StrUtil.isNotBlank(source) || StrUtil.isNotBlank(translated)) {
                TextItemRespVO item = new TextItemRespVO();
                item.setSourceText(source);
                item.setTranslatedText(translated);
                item.setConfidence(number(node, 1.0d, "confidence", "Confidence", "score", "Score"));
                item.setBox(parseBox(node));
                items.add(item);
            }
            Iterator<JsonNode> iterator = node.elements();
            while (iterator.hasNext()) {
                collectTextItems(iterator.next(), items);
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectTextItems(child, items);
            }
        }
    }

    private BoxRespVO parseBox(JsonNode node) {
        double left = number(node, Double.NaN, "left", "Left", "x", "X");
        double top = number(node, Double.NaN, "top", "Top", "y", "Y");
        double width = number(node, Double.NaN, "width", "Width", "w", "W");
        double height = number(node, Double.NaN, "height", "Height", "h", "H");
        JsonNode boxNode = firstNode(node, "box", "Box", "boundingBox", "BoundingBox", "bbox", "BBox");
        if (boxNode != null && boxNode.isObject()) {
            left = Double.isNaN(left) ? number(boxNode, Double.NaN, "left", "x") : left;
            top = Double.isNaN(top) ? number(boxNode, Double.NaN, "top", "y") : top;
            width = Double.isNaN(width) ? number(boxNode, Double.NaN, "width", "w") : width;
            height = Double.isNaN(height) ? number(boxNode, Double.NaN, "height", "h") : height;
        }
        JsonNode upLeft = firstNode(node, "upLeft", "UpLeft", "upperLeft", "UpperLeft");
        JsonNode downRight = firstNode(node, "downRight", "DownRight", "lowerRight", "LowerRight");
        if (upLeft != null && downRight != null) {
            double x1 = number(upLeft, Double.NaN, "x", "X", "left", "Left");
            double y1 = number(upLeft, Double.NaN, "y", "Y", "top", "Top");
            double x2 = number(downRight, Double.NaN, "x", "X", "right", "Right");
            double y2 = number(downRight, Double.NaN, "y", "Y", "bottom", "Bottom");
            if (!Double.isNaN(x1) && !Double.isNaN(y1) && !Double.isNaN(x2) && !Double.isNaN(y2)) {
                left = Math.min(x1, x2);
                top = Math.min(y1, y2);
                width = Math.abs(x2 - x1);
                height = Math.abs(y2 - y1);
            }
        }
        if (Double.isNaN(left) || Double.isNaN(top) || Double.isNaN(width) || Double.isNaN(height)
                || width <= 0d || height <= 0d) {
            return null;
        }
        BoxRespVO box = new BoxRespVO();
        box.setX(left);
        box.setY(top);
        box.setWidth(width);
        box.setHeight(height);
        return box;
    }

    private int countBoxes(List<TextItemRespVO> items) {
        int count = 0;
        for (TextItemRespVO item : items) {
            if (item.getBox() != null) {
                count++;
            }
        }
        return count;
    }

    private boolean hasLowConfidence(List<TextItemRespVO> items) {
        for (TextItemRespVO item : items) {
            if (item.getConfidence() > 0d && item.getConfidence() < properties.getQuality().getMinConfidence()) {
                return true;
            }
        }
        return false;
    }

    private double maxOverlapRatio(List<TextItemRespVO> items) {
        double max = 0d;
        for (int i = 0; i < items.size(); i++) {
            BoxRespVO a = items.get(i).getBox();
            if (a == null) {
                continue;
            }
            for (int j = i + 1; j < items.size(); j++) {
                BoxRespVO b = items.get(j).getBox();
                if (b == null) {
                    continue;
                }
                double overlap = overlapArea(a, b);
                double minArea = Math.min(a.getWidth() * a.getHeight(), b.getWidth() * b.getHeight());
                if (minArea > 0d) {
                    max = Math.max(max, overlap / minArea);
                }
            }
        }
        return max;
    }

    private double overlapArea(BoxRespVO a, BoxRespVO b) {
        double left = Math.max(a.getX(), b.getX());
        double top = Math.max(a.getY(), b.getY());
        double right = Math.min(a.getX() + a.getWidth(), b.getX() + b.getWidth());
        double bottom = Math.min(a.getY() + a.getHeight(), b.getY() + b.getHeight());
        return Math.max(0d, right - left) * Math.max(0d, bottom - top);
    }

    private boolean looksStacked(List<TextItemRespVO> items) {
        int narrow = 0;
        int boxed = 0;
        for (TextItemRespVO item : items) {
            BoxRespVO box = item.getBox();
            if (box == null) {
                continue;
            }
            boxed++;
            if (box.getHeight() > 0d && box.getWidth() / box.getHeight() < 0.75d) {
                narrow++;
            }
        }
        return boxed >= 3 && narrow >= Math.max(2, boxed / 2);
    }

    private boolean onlySoftWarnings(List<String> reasons) {
        return reasons.isEmpty() || reasons.stream().allMatch(reason ->
                "box_count_too_low".equals(reason) || "confidence_too_low".equals(reason));
    }

    private List<TextItemRespVO> dedupe(List<TextItemRespVO> items) {
        List<TextItemRespVO> result = new ArrayList<>();
        for (TextItemRespVO item : items) {
            String key = StrUtil.blankToDefault(item.getSourceText(), "") + "\n" + StrUtil.blankToDefault(item.getTranslatedText(), "");
            boolean exists = false;
            for (TextItemRespVO existing : result) {
                String existingKey = StrUtil.blankToDefault(existing.getSourceText(), "") + "\n" + StrUtil.blankToDefault(existing.getTranslatedText(), "");
                if (existingKey.equals(key)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                result.add(item);
            }
        }
        return result;
    }

    private String toQualityJson(String status, List<String> reasons, List<TextItemRespVO> textItems, double score) {
        try {
            return objectMapper.writeValueAsString(new QualityJson(status, reasons, textItems.size(), score));
        } catch (Exception ex) {
            return "{\"status\":\"" + status + "\"}";
        }
    }

    private String text(JsonNode node, String key) {
        if (node == null || node.isMissingNode() || !node.has(key) || node.get(key).isNull()) {
            return "";
        }
        JsonNode value = node.get(key);
        if (value.isTextual() || value.isNumber() || value.isBoolean()) {
            return value.asText("");
        }
        return firstScalarText(value);
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

    private String firstScalarText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText("");
        }
        for (JsonNode child : node) {
            String value = firstScalarText(child);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private JsonNode firstNode(JsonNode node, String... keys) {
        if (node == null) {
            return null;
        }
        for (String key : keys) {
            if (node.has(key)) {
                return node.get(key);
            }
        }
        return null;
    }

    private double number(JsonNode node, double defaultValue, String... keys) {
        for (String key : keys) {
            if (node != null && node.has(key) && node.get(key).isNumber()) {
                return node.get(key).asDouble(defaultValue);
            }
        }
        return defaultValue;
    }

    private record QualityJson(String status, List<String> reasons, int textItemCount, double score) {
    }
}
