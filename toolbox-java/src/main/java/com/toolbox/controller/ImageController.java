package com.toolbox.controller;

import com.toolbox.common.Result;
import com.toolbox.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private ImageService imageService;

    @PostMapping("/compress")
    public Result<Map<String, Object>> compress(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "width", required = false) Integer width,
            @RequestParam(value = "quality", required = false) Integer quality,
            HttpServletRequest request) {
        
        log.info("========== 图片压缩请求 ==========");
        log.info("文件名: {}", file.getOriginalFilename());
        log.info("文件大小: {} bytes", file.getSize());
        log.info("目标宽度: {}, 质量: {}", width, quality);
        
        try {
            Integer userId = getUserId(request);
            Map<String, Object> result = imageService.compress(userId, file, width, quality);
            log.info("压缩成功: {}", result.get("fileUrl"));
            return Result.success("图片压缩成功", result);
        } catch (Exception e) {
            log.error("压缩失败: {}", e.getMessage(), e);
            return Result.error("图片压缩失败: " + e.getMessage());
        }
    }

    @PostMapping("/background")
    public Result<Map<String, Object>> background(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "color", defaultValue = "white") String color,
            HttpServletRequest request) {
        try {
            Integer userId = getUserId(request);
            Map<String, Object> result = imageService.changeBackground(userId, file, color);
            return Result.success("换背景成功", result);
        } catch (Exception e) {
            return Result.error("换背景失败: " + e.getMessage());
        }
    }

    @PostMapping("/ocr")
    public Result<Map<String, Object>> ocr(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            Integer userId = getUserId(request);
            Map<String, Object> result = imageService.ocr(userId, file);
            return Result.success("OCR识别成功", result);
        } catch (Exception e) {
            return Result.error("OCR识别失败: " + e.getMessage());
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
