package cn.iocoder.yudao.server.service.image.provider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class AliyunOpenApiSigner {

    private AliyunOpenApiSigner() {
    }

    static Map<String, String> sign(String action,
                                    String version,
                                    Map<String, String> businessParams,
                                    String accessKeyId,
                                    String accessKeySecret) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", accessKeyId);
        params.put("Action", action);
        params.put("Format", "JSON");
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", UUID.randomUUID() + "-" + new SecureRandom().nextInt(100000));
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now()));
        params.put("Version", version);
        params.putAll(businessParams);

        String canonicalized = canonicalize(params);
        String stringToSign = "POST&%2F&" + percentEncode(canonicalized);
        params.put("Signature", hmacSha1(stringToSign, accessKeySecret + "&"));
        return params;
    }

    static String formBody(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                builder.append('&');
            }
            builder.append(percentEncode(entry.getKey())).append('=').append(percentEncode(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    static Map<String, String> acs3Headers(String method,
                                           String path,
                                           String query,
                                           String body,
                                           String action,
                                           String version,
                                           String accessKeyId,
                                           String accessKeySecret,
                                           String host) {
        return acs3Headers(method, path, query,
                (body == null ? "" : body).getBytes(StandardCharsets.UTF_8),
                "application/json; charset=utf-8",
                action, version, accessKeyId, accessKeySecret, host);
    }

    static Map<String, String> acs3Headers(String method,
                                           String path,
                                           String query,
                                           byte[] bodyBytes,
                                           String contentType,
                                           String action,
                                           String version,
                                           String accessKeyId,
                                           String accessKeySecret,
                                           String host) {
        byte[] bytes = bodyBytes != null ? bodyBytes : new byte[0];
        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("content-type", contentType == null || contentType.isBlank()
                ? "application/json; charset=utf-8"
                : contentType);
        headers.put("host", host);
        headers.put("x-acs-action", action);
        headers.put("x-acs-content-sha256", sha256Hex(bytes));
        headers.put("x-acs-date", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now()));
        headers.put("x-acs-signature-nonce", UUID.randomUUID() + "-" + new SecureRandom().nextInt(100000));
        headers.put("x-acs-version", version);

        StringBuilder canonicalHeaders = new StringBuilder();
        StringBuilder signedHeaders = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            canonicalHeaders.append(entry.getKey()).append(':').append(trimHeaderValue(entry.getValue())).append('\n');
            if (!first) {
                signedHeaders.append(';');
            }
            signedHeaders.append(entry.getKey());
            first = false;
        }
        String canonicalRequest = method.toUpperCase() + "\n"
                + path + "\n"
                + (query != null ? query : "") + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + headers.get("x-acs-content-sha256");
        String stringToSign = "ACS3-HMAC-SHA256\n" + sha256Hex(canonicalRequest);
        String signature = hmacSha256Hex(stringToSign, accessKeySecret);

        Map<String, String> output = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            output.put(entry.getKey(), entry.getValue());
        }
        output.put("authorization", "ACS3-HMAC-SHA256 Credential=" + accessKeyId
                + ",SignedHeaders=" + signedHeaders
                + ",Signature=" + signature);
        return output;
    }

    private static String canonicalize(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            builder.append(percentEncode(entry.getKey())).append("=").append(percentEncode(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    private static String hmacSha1(String plainText, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign Aliyun OpenAPI request", ex);
        }
    }

    private static String hmacSha256Hex(String plainText, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return hex(mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign Aliyun OpenAPI ACS3 request", ex);
        }
    }

    private static String sha256Hex(String value) {
        return sha256Hex((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return hex(digest.digest(value != null ? value : new byte[0]));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash Aliyun OpenAPI request", ex);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String value = Integer.toHexString(b & 0xff);
            if (value.length() == 1) {
                builder.append('0');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String trimHeaderValue(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static String percentEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
}
