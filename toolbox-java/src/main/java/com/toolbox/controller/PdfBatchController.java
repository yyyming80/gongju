package com.toolbox.controller;

import com.toolbox.common.Result;
import com.toolbox.service.PdfBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf-batch")
public class PdfBatchController {

    @Autowired
    private PdfBatchService pdfBatchService;

    /**
     * 上传单张图片（批量处理流程第一步）
     */
    @PostMapping("/upload-image")
    public Result<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId) {
        try {
            Map<String, Object> result = pdfBatchService.uploadImage(sessionId, file);
            return Result.success("图片上传成功", result);
        } catch (Exception e) {
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量转换（批量处理流程第二步）
     */
    @GetMapping("/convert")
    public Result<Map<String, Object>> convert(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("fileCount") Integer fileCount) {
        try {
            Map<String, Object> result = pdfBatchService.convert(sessionId, fileCount);
            return Result.success("PDF转换成功", result);
        } catch (Exception e) {
            return Result.error("PDF转换失败: " + e.getMessage());
        }
    }

    /**
     * 清除会话（可选）
     */
    @DeleteMapping("/clear")
    public Result<Void> clearSession(@RequestParam("sessionId") String sessionId) {
        try {
            pdfBatchService.clearSession(sessionId);
            return Result.success("会话已清除", null);
        } catch (Exception e) {
            return Result.error("清除失败: " + e.getMessage());
        }
    }
}
