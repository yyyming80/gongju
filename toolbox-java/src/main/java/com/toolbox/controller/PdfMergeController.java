package com.toolbox.controller;

import com.toolbox.common.Result;
import com.toolbox.service.PdfMergeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf-merge")
public class PdfMergeController {

    private static final Logger log = LoggerFactory.getLogger(PdfMergeController.class);

    @Autowired
    private PdfMergeService pdfMergeService;

    /**
     * 上传单个PDF文件到会话
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "index", required = false) Integer index,
            HttpServletRequest request) {
        
        log.info("========== PDF合并文件上传 ==========");
        log.info("请求方法: POST");
        log.info("请求URL: /api/pdf-merge/upload");
        
        // 打印所有请求参数
        log.info("--- 请求参数 ---");
        Map<String, String[]> paramMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            log.info("参数[{}] = {}", entry.getKey(), String.join(", ", entry.getValue()));
        }
        
        // 检查文件
        if (file == null) {
            log.error("文件为空！检查wx.uploadFile的name参数");
            log.info("常见问题：wx.uploadFile的name参数应为'file'");
            return Result.error("上传失败: 未收到文件，请检查前端name参数");
        }
        
        if (file.isEmpty()) {
            log.error("文件大小为0");
            return Result.error("上传失败: 文件为空");
        }
        
        if (sessionId == null || sessionId.isEmpty()) {
            log.error("sessionId为空");
            return Result.error("上传失败: sessionId为空");
        }
        
        log.info("--- 文件信息 ---");
        log.info("文件名: {}", file.getOriginalFilename());
        log.info("文件大小: {} bytes", file.getSize());
        log.info("内容类型: {}", file.getContentType());
        log.info("SessionId: {}", sessionId);
        log.info("文件索引: {}", index);
        
        try {
            Map<String, Object> result = pdfMergeService.uploadFile(sessionId, file, index);
            log.info("上传成功，当前会话文件数: {}", result.get("fileCount"));
            log.info("========================================");
            return Result.success("上传成功", result);
        } catch (Exception e) {
            log.error("上传失败: {}", e.getMessage(), e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 合并会话中的所有PDF文件
     */
    @GetMapping("/merge")
    public Result<Map<String, Object>> merge(
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        
        log.info("========== PDF合并请求 ==========");
        log.info("请求方法: GET");
        log.info("请求URL: /api/pdf-merge/merge");
        log.info("SessionId: {}", sessionId);
        
        if (sessionId == null || sessionId.isEmpty()) {
            log.error("sessionId为空！");
            return Result.error("合并失败: sessionId不能为空");
        }
        
        try {
            Map<String, Object> result = pdfMergeService.merge(sessionId);
            log.info("合并成功！");
            log.info("文件URL: {}", result.get("fileUrl"));
            log.info("页数: {}", result.get("pageCount"));
            log.info("========================================");
            return Result.success("合并成功", result);
        } catch (Exception e) {
            log.error("合并失败: {}", e.getMessage(), e);
            return Result.error("合并失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话中的文件列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        log.info("获取会话文件列表: {}", sessionId);
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                return Result.error("sessionId不能为空");
            }
            Map<String, Object> result = pdfMergeService.getSessionFiles(sessionId);
            return Result.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取失败: {}", e.getMessage(), e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
