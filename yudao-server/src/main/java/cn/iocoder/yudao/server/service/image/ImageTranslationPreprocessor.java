package cn.iocoder.yudao.server.service.image;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.server.service.image.ImageTranslationModels.ProcessedImage;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

@Service
public class ImageTranslationPreprocessor {

    private static final int MAX_SIDE = 2560;
    private static final float JPEG_QUALITY = 0.92f;

    public ProcessedImage process(byte[] originalBytes, String filename, String contentType) {
        if (originalBytes == null || originalBytes.length == 0) {
            throw ServiceExceptionUtil.invalidParamException("图片为空，请重新选择");
        }
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (original == null) {
                throw ServiceExceptionUtil.invalidParamException("图片格式暂不支持，请更换图片");
            }
            BufferedImage enhanced = scaleIfNeeded(toRgb(original), MAX_SIDE);
            byte[] enhancedBytes = writeJpeg(enhanced);
            return new ProcessedImage()
                    .setOriginalBytes(originalBytes)
                    .setEnhancedBytes(enhancedBytes)
                    .setOriginalExt(resolveExt(filename, contentType))
                    .setEnhancedExt("jpg")
                    .setOriginalWidth(original.getWidth())
                    .setOriginalHeight(original.getHeight())
                    .setEnhancedWidth(enhanced.getWidth())
                    .setEnhancedHeight(enhanced.getHeight())
                    .setOriginalSizeBytes(originalBytes.length)
                    .setEnhancedSizeBytes(enhancedBytes.length);
        } catch (IOException ex) {
            throw ServiceExceptionUtil.invalidParamException("图片处理失败，请重新选择");
        }
    }

    private BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgb;
    }

    private BufferedImage scaleIfNeeded(BufferedImage source, int maxSide) {
        int width = source.getWidth();
        int height = source.getHeight();
        int side = Math.max(width, height);
        if (side <= maxSide) {
            return source;
        }
        double ratio = maxSide / (double) side;
        int targetWidth = Math.max(1, (int) Math.round(width * ratio));
        int targetHeight = Math.max(1, (int) Math.round(height * ratio));
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return scaled;
    }

    private byte[] writeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    private String resolveExt(String filename, String contentType) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot >= 0 && dot < lower.length() - 1) {
            String ext = lower.substring(dot + 1);
            if (ext.matches("jpg|jpeg|png|webp|bmp")) {
                return "jpeg".equals(ext) ? "jpg" : ext;
            }
        }
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("png")) {
            return "png";
        }
        if (type.contains("webp")) {
            return "webp";
        }
        if (type.contains("bmp")) {
            return "bmp";
        }
        return "jpg";
    }
}
