package com.toolbox.service;

import com.toolbox.config.UploadProperties;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageConvertService {

    private static final Logger log = LoggerFactory.getLogger(ImageConvertService.class);

    @Autowired
    private UploadProperties uploadProperties;

    /**
     * 图片格式转换
     * 支持：png, jpg, jpeg, bmp, webp
     */
    public Map<String, Object> convert(Integer userId, MultipartFile file, String targetFormat) throws Exception {
        log.info("========== ImageConvertService.convert ==========");
        
        // 参数验证
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请上传图片文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        log.info("原始文件名: {}", originalFilename);
        log.info("原始文件大小: {} bytes", file.getSize());

        // 标准化目标格式
        targetFormat = targetFormat.toLowerCase().trim();
        if (targetFormat.equals("jpeg")) {
            targetFormat = "jpg";
        }

        // 验证支持的目标格式
        if (!isValidFormat(targetFormat)) {
            throw new RuntimeException("不支持的目标格式: " + targetFormat + "，仅支持 png/jpg/bmp/webp");
        }

        String taskNo = "CONVERT_" + System.currentTimeMillis();

        // 创建输出目录
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String outputDir = uploadProperties.getPath() + File.separator + datePath;
        File dir = new File(outputDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("创建输出目录: {}, 结果: {}", outputDir, created);
        }

        // 生成输出文件名
        String baseName = originalFilename;
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalFilename.substring(0, dotIndex);
        }
        String outputFileName = taskNo + "_" + baseName + "." + targetFormat;
        String outputFilePath = outputDir + File.separator + outputFileName;

        log.info("输出文件路径: {}", outputFilePath);

        // 使用 Thumbnailator 进行格式转换
        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new RuntimeException("无法读取图片文件，图片格式可能不支持");
            }

            log.info("图片尺寸: {}x{}", originalImage.getWidth(), originalImage.getHeight());

            // 转换格式
            Thumbnails.of(originalImage)
                    .size(originalImage.getWidth(), originalImage.getHeight())
                    .outputFormat(targetFormat)
                    .outputQuality(0.92)  // 保持高质量
                    .toFile(outputFilePath);

            log.info("格式转换完成");

        } catch (Exception e) {
            log.error("Thumbnailator转换失败，尝试使用ImageIO: {}", e.getMessage());
            
            // 备用方案：使用ImageIO
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new RuntimeException("无法读取图片文件");
            }

            File outputFile = new File(outputFilePath);
            boolean success = ImageIO.write(image, targetFormat, outputFile);
            if (!success) {
                throw new RuntimeException("ImageIO转换失败");
            }
        }

        // 验证文件是否生成
        File outputFile = new File(outputFilePath);
        if (!outputFile.exists()) {
            throw new RuntimeException("转换后文件未生成: " + outputFilePath);
        }

        long outputSize = outputFile.length();
        log.info("输出文件大小: {} bytes", outputSize);

        // 获取图片信息
        BufferedImage resultImage = ImageIO.read(outputFile);
        int width = resultImage != null ? resultImage.getWidth() : 0;
        int height = resultImage != null ? resultImage.getHeight() : 0;

        // 构建返回URL
        String fileUrl = uploadProperties.getBaseUrl() + datePath + "/" + outputFileName;
        log.info("返回URL: {}", fileUrl);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("taskNo", taskNo);
        result.put("fileName", outputFileName);
        result.put("fileUrl", fileUrl);
        result.put("filePath", datePath + "/" + outputFileName);
        result.put("sourceSize", file.getSize());
        result.put("outputSize", outputSize);
        result.put("sourceFormat", getFileExtension(originalFilename));
        result.put("targetFormat", targetFormat);
        result.put("width", width);
        result.put("height", height);

        log.info("========================================");
        return result;
    }

    /**
     * 验证格式是否支持
     */
    private boolean isValidFormat(String format) {
        return "png".equals(format) || 
               "jpg".equals(format) || 
               "jpeg".equals(format) || 
               "bmp".equals(format) || 
               "webp".equals(format);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
}
