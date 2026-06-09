package com.toolbox.controller;

import com.toolbox.common.Result;
import com.toolbox.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @PostMapping("/merge")
    public Result<Map<String, Object>> merge(
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request) {
        try {
            Integer userId = getUserId(request);
            Map<String, Object> result = pdfService.merge(userId, files);
            return Result.success("PDF合并成功", result);
        } catch (Exception e) {
            return Result.error("PDF合并失败: " + e.getMessage());
        }
    }

    @PostMapping("/split")
    public Result<Map<String, Object>> split(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "pageRange", required = false) String pageRange,
            HttpServletRequest request) {
        try {
            Integer userId = getUserId(request);
            Map<String, Object> result = pdfService.split(userId, file, pageRange);
            return Result.success("PDF拆分成功", result);
        } catch (Exception e) {
            return Result.error("PDF拆分失败: " + e.getMessage());
        }
    }

    @PostMapping("/word-to-pdf")
    public Result<Map<String, Object>> wordToPdf(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            Integer userId = getUserId(request);
            Map<String, Object> result = pdfService.wordToPdf(userId, file);
            return Result.success("Word转PDF成功", result);
        } catch (Exception e) {
            return Result.error("Word转PDF失败: " + e.getMessage());
        }
    }

    @PostMapping("/image-to-pdf")
    public Result<Map<String, Object>> imageToPdf(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "sortOrder", required = false) List<Integer> sortOrder,
            HttpServletRequest request) {
        try {
            Integer userId = getUserId(request);
            Map<String, Object> result = pdfService.imageToPdf(userId, files, sortOrder);
            return Result.success("图片转PDF成功", result);
        } catch (Exception e) {
            return Result.error("图片转PDF失败: " + e.getMessage());
        }
    }

    @PostMapping("/to-image")
    public Result<Map<String, Object>> pdfToImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "page", required = false) Integer page,
            HttpServletRequest request) {
        try {
            Integer userId = getUserId(request);
            Map<String, Object> result = pdfService.pdfToImage(userId, file, page);
            return Result.success("PDF转图片成功", result);
        } catch (Exception e) {
            return Result.error("PDF转图片失败: " + e.getMessage());
        }
    }

    @PostMapping("/info")
    public Result<Map<String, Object>> info(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = pdfService.getPdfInfo(file);
            return Result.success("获取PDF信息成功", result);
        } catch (Exception e) {
            return Result.error("获取PDF信息失败: " + e.getMessage());
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
