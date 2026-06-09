package com.toolbox.controller;

import com.toolbox.common.Result;
import com.toolbox.service.ImageConvertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/image-convert")
public class ImageConvertController {

    private static final Logger log = LoggerFactory.getLogger(ImageConvertController.class);

    @Autowired
    private ImageConvertService imageConvertService;

    /**
     * 图片格式转换
     */
    @PostMapping("/convert")
    public Result<Map<String, Object>> convert(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "format", defaultValue = "png") String targetFormat,
            HttpServletRequest request) {
        
        log.info("========== 图片格式转换 ==========");
        log.info("文件名: {}", file.getOriginalFilename());
        log.info("目标格式: {}", targetFormat);
        
        try {
            Integer userId = getUserId(request);
            Map<String, Object> result = imageConvertService.convert(userId, file, targetFormat);
            log.info("转换成功: {}", result.get("fileUrl"));
            return Result.success("图片转换成功", result);
        } catch (Exception e) {
            log.error("转换失败: {}", e.getMessage(), e);
            return Result.error("图片转换失败: " + e.getMessage());
        }
    }

    private Integer getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                return Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
