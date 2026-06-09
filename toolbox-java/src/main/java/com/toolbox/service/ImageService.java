package com.toolbox.service;

import com.toolbox.config.UploadProperties;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private UploadProperties uploadProperties;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RestTemplate restTemplate;

    private static final String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png", "bmp"};

    /**
     * 图片压缩
     */
    public Map<String, Object> compress(Integer userId, MultipartFile file, Integer width, Integer quality) throws Exception {
        log.info("========== 图片压缩开始 ==========");
        log.info("原始文件名: {}", file.getOriginalFilename());
        log.info("原始文件大小: {} bytes", file.getSize());

        // 1. 参数验证
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请上传图片文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        String lowerName = originalFilename.toLowerCase();
        boolean isValidFormat = false;
        for (String fmt : SUPPORTED_FORMATS) {
            if (lowerName.endsWith("." + fmt)) {
                isValidFormat = true;
                break;
            }
        }
        if (!isValidFormat) {
            throw new RuntimeException("仅支持JPG、PNG、BMP格式的图片");
        }

        // 2. 生成任务号和路径
        String taskNo = "COMPRESS_" + System.currentTimeMillis();
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String outputDir = uploadProperties.getPath() + File.separator + datePath;
        File dir = new File(outputDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("创建输出目录: {}, 结果: {}", outputDir, created);
        }

        // 3. 先保存上传文件到临时目录
        String tempDir = uploadProperties.getPath() + File.separator + "temp";
        File tempDirFile = new File(tempDir);
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs();
        }
        
        String tempFileName = taskNo + "_source" + getExtension(originalFilename);
        String tempFilePath = tempDir + File.separator + tempFileName;
        File tempFile = new File(tempFilePath);
        
        log.info("保存临时文件: {}", tempFilePath);
        file.transferTo(tempFile);
        
        if (!tempFile.exists()) {
            throw new RuntimeException("临时文件保存失败");
        }
        log.info("临时文件保存成功，大小: {} bytes", tempFile.length());

        // 4. 读取图片信息
        BufferedImage originalImage = ImageIO.read(tempFile);
        if (originalImage == null) {
            tempFile.delete();
            throw new RuntimeException("无法读取图片文件");
        }

        int origWidth = originalImage.getWidth();
        int origHeight = originalImage.getHeight();
        log.info("图片尺寸: {}x{}", origWidth, origHeight);

        // 5. 计算压缩参数
        if (width == null || width <= 0) {
            width = origWidth;
        }
        if (quality == null || quality <= 0 || quality > 100) {
            quality = 80;
        }

        int newWidth = width;
        int newHeight = (int) (origHeight * (width * 1.0 / origWidth));
        double scale = width * 1.0 / origWidth;
        
        log.info("压缩参数: 宽度={}, 高度={}, 质量={}%, 比例={}", newWidth, newHeight, quality, scale);

        // 6. 生成输出文件名和路径
        String outputFileName = taskNo + ".jpg";
        String outputFilePath = outputDir + File.separator + outputFileName;
        log.info("输出文件路径: {}", outputFilePath);

        // 7. 执行压缩
        try {
            Thumbnails.of(tempFilePath)
                    .size(newWidth, newHeight)
                    .outputQuality(quality / 100.0)
                    .outputFormat("jpg")
                    .toFile(outputFilePath);
            log.info("压缩完成");
        } catch (Exception e) {
            log.error("Thumbnailator压缩失败: {}", e.getMessage());
            tempFile.delete();
            throw new RuntimeException("图片压缩失败: " + e.getMessage());
        }

        // 8. 清理临时文件
        tempFile.delete();
        log.info("临时文件已清理");

        // 9. 验证输出文件
        File outputFile = new File(outputFilePath);
        if (!outputFile.exists()) {
            throw new RuntimeException("压缩后文件未生成: " + outputFilePath);
        }

        long outputSize = outputFile.length();
        long sourceSize = file.getSize();
        double compressionRate = sourceSize > 0 ? (1 - (double) outputSize / sourceSize) * 100 : 0;

        log.info("输出文件大小: {} bytes", outputSize);
        log.info("压缩率: {}%", String.format("%.2f", compressionRate));

        // 10. 构建返回URL
        String fileUrl = uploadProperties.getBaseUrl() + datePath + "/" + outputFileName;
        log.info("返回URL: {}", fileUrl);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("taskNo", taskNo);
        result.put("fileName", outputFileName);
        result.put("fileUrl", fileUrl);
        result.put("filePath", datePath + "/" + outputFileName);
        result.put("sourceSize", sourceSize);
        result.put("originalSize", sourceSize);
        result.put("outputSize", outputSize);
        result.put("compressedSize", outputSize);
        result.put("compressionRate", String.format("%.2f", compressionRate) + "%");
        result.put("width", newWidth);
        result.put("height", newHeight);
        result.put("originalWidth", origWidth);
        result.put("originalHeight", origHeight);

        log.info("========================================");
        return result;
    }

    /**
     * 换背景
     */
    public Map<String, Object> changeBackground(Integer userId, MultipartFile file, String color) throws Exception {
        log.info("========== 换背景开始 ==========");
        
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请上传图片文件");
        }

        String taskNo = "BGCHANGE_" + System.currentTimeMillis();
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String outputDir = uploadProperties.getPath() + File.separator + datePath;
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 调用 Rembg 服务
        BufferedImage transparentImage = callRembgService(file);
        if (transparentImage == null) {
            throw new RuntimeException("Rembg服务调用失败，请确保RemBG服务正在运行");
        }

        int targetRGB = parseColor(color);
        int width = transparentImage.getWidth();
        int height = transparentImage.getHeight();
        
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = outputImage.createGraphics();
        g2d.setColor(new Color(targetRGB));
        g2d.fillRect(0, 0, width, height);
        g2d.drawImage(transparentImage, 0, 0, null);
        g2d.dispose();

        String outputFileName = taskNo + ".png";
        String outputFilePath = outputDir + File.separator + outputFileName;
        ImageIO.write(outputImage, "png", new File(outputFilePath));

        String fileUrl = uploadProperties.getBaseUrl() + datePath + "/" + outputFileName;

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("taskNo", taskNo);
        result.put("fileName", outputFileName);
        result.put("fileUrl", fileUrl);
        result.put("filePath", datePath + "/" + outputFileName);
        result.put("targetColor", color);
        result.put("targetRGB", "#" + Integer.toHexString(targetRGB));
        
        log.info("========================================");
        return result;
    }

    /**
     * OCR识别
     */
    public Map<String, Object> ocr(Integer userId, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请上传图片文件");
        }

        // 这里需要接入实际的OCR服务
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "OCR功能正在开发中");
        return result;
    }

    /**
     * 调用Rembg服务
     */
    private BufferedImage callRembgService(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", file.getResource());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<byte[]> response = restTemplate.exchange(
                "http://localhost:5000/api/remove-background",
                HttpMethod.POST,
                requestEntity,
                byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return ImageIO.read(new ByteArrayInputStream(response.getBody()));
            }
        } catch (Exception e) {
            log.error("调用Rembg服务失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 解析颜色
     */
    private int parseColor(String color) {
        if (color == null || color.isEmpty()) {
            return Color.WHITE.getRGB();
        }
        try {
            if (color.startsWith("#")) {
                return Color.decode(color).getRGB();
            } else if (color.equalsIgnoreCase("white")) {
                return Color.WHITE.getRGB();
            } else if (color.equalsIgnoreCase("red")) {
                return Color.RED.getRGB();
            } else if (color.equalsIgnoreCase("blue")) {
                return Color.BLUE.getRGB();
            } else if (color.equalsIgnoreCase("green")) {
                return Color.GREEN.getRGB();
            } else if (color.equalsIgnoreCase("black")) {
                return Color.BLACK.getRGB();
            }
            return Color.decode(color).getRGB();
        } catch (Exception e) {
            return Color.WHITE.getRGB();
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot);
        }
        return ".jpg";
    }
}
