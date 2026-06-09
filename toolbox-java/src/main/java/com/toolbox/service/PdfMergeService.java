package com.toolbox.service;

import com.toolbox.config.UploadProperties;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PdfMergeService {

    private static final Logger log = LoggerFactory.getLogger(PdfMergeService.class);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UploadProperties uploadProperties;

    // 存储会话信息: sessionId -> List<上传文件路径>
    private final Map<String, List<PdfFileInfo>> sessionFiles = new ConcurrentHashMap<>();

    /**
     * PDF文件信息
     */
    private static class PdfFileInfo {
        String originalName;
        String savedPath;
        long size;
        int index;

        PdfFileInfo(String originalName, String savedPath, long size, int index) {
            this.originalName = originalName;
            this.savedPath = savedPath;
            this.size = size;
            this.index = index;
        }
    }

    /**
     * 上传单个PDF文件
     */
    public Map<String, Object> uploadFile(String sessionId, MultipartFile file, Integer index) throws Exception {
        log.info("========== PdfMergeService.uploadFile ==========");
        log.info("SessionId: {}", sessionId);
        log.info("文件名: {}", file.getOriginalFilename());
        log.info("文件大小: {}", file.getSize());
        log.info("文件索引: {}", index);
        
        if (sessionId == null || sessionId.isEmpty()) {
            throw new RuntimeException("sessionId不能为空");
        }
        
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("仅支持PDF文件");
        }

        // 创建会话目录
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String sessionDir = uploadProperties.getPath() + File.separator + "temp" + File.separator + sessionId;
        File dir = new File(sessionDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("创建会话目录: {}, 结果: {}", sessionDir, created);
        }

        // 保存文件
        int fileIndex = index != null ? index : sessionFiles.computeIfAbsent(sessionId, k -> new ArrayList<>()).size();
        String savedFileName = String.format("%03d_%s", fileIndex, originalFilename);
        String savedFilePath = sessionDir + File.separator + savedFileName;
        
        log.info("保存文件到: {}", savedFilePath);
        File targetFile = new File(savedFilePath);
        file.transferTo(targetFile);
        
        // 验证文件是否保存成功
        if (!targetFile.exists()) {
            throw new RuntimeException("文件保存失败");
        }
        log.info("文件保存成功，大小: {} bytes", targetFile.length());

        // 添加到会话列表
        List<PdfFileInfo> files = sessionFiles.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()));
        files.add(new PdfFileInfo(originalFilename, savedFilePath, file.getSize(), fileIndex));

        // 按索引排序
        files.sort(Comparator.comparingInt(f -> f.index));

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("fileCount", files.size());
        result.put("uploadedFile", originalFilename);
        result.put("fileIndex", fileIndex);
        result.put("savedPath", savedFilePath);
        
        log.info("文件已添加到会话，当前会话文件数: {}", files.size());
        log.info("========================================");
        return result;
    }

    /**
     * 合并会话中的所有PDF文件 - 使用 PDFMergerUtility
     */
    public Map<String, Object> merge(String sessionId) throws Exception {
        List<PdfFileInfo> files = sessionFiles.get(sessionId);
        
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("没有上传的文件，请先上传PDF文件");
        }

        if (files.size() < 2) {
            throw new RuntimeException("至少需要2个PDF文件才能合并");
        }

        String taskNo = "MERGE_" + System.currentTimeMillis();
        log.info("========== PDF合并开始 ==========");
        log.info("SessionId: {}", sessionId);
        log.info("文件数: {}", files.size());

        // 打印合并顺序
        log.info("=== 合并顺序 ===");
        for (PdfFileInfo file : files) {
            log.info("  [{}] {} -> {}", file.index, file.originalName, file.savedPath);
        }

        // 创建输出目录
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String outputDir = uploadProperties.getPath() + File.separator + datePath;
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 使用 PDFMergerUtility 合并 - 这是官方推荐的方式
        String outputFileName = taskNo + ".pdf";
        String outputFilePath = outputDir + File.separator + outputFileName;
        log.info("输出文件: {}", outputFilePath);

        PDFMergerUtility mergerUtility = new PDFMergerUtility();
        mergerUtility.setDestinationFileName(outputFilePath);

        int totalPages = 0;
        List<String> sourceFileNames = new ArrayList<>();

        // 使用临时列表收集源文件
        List<File> sourceFiles = new ArrayList<>();
        
        for (PdfFileInfo fileInfo : files) {
            File pdfFile = new File(fileInfo.savedPath);
            if (!pdfFile.exists()) {
                log.warn("文件不存在，跳过: {}", fileInfo.savedPath);
                continue;
            }
            
            log.info("添加源文件: {}", fileInfo.originalName);
            sourceFiles.add(pdfFile);
            sourceFileNames.add(fileInfo.originalName);
        }

        if (sourceFiles.isEmpty()) {
            throw new RuntimeException("没有有效的PDF文件");
        }

        try {
            // 使用 PDFMergerUtility 合并
            for (File sourceFile : sourceFiles) {
                log.info("正在合并: {}", sourceFile.getName());
                mergerUtility.addSource(sourceFile);
            }
            
            log.info("正在执行合并...");
            mergerUtility.mergeDocuments(null);
            log.info("合并完成");

            // 计算总页数
            for (File sourceFile : sourceFiles) {
                try (PDDocument doc = PDDocument.load(sourceFile)) {
                    totalPages += doc.getNumberOfPages();
                }
            }
            
        } catch (IOException e) {
            log.error("合并失败: {}", e.getMessage(), e);
            throw new RuntimeException("PDF合并失败: " + e.getMessage());
        }

        File outputFile = new File(outputFilePath);
        if (!outputFile.exists()) {
            throw new RuntimeException("合并后文件未生成");
        }
        
        long fileSize = outputFile.length();
        log.info("输出文件大小: {} bytes", fileSize);
        log.info("总页数: {}", totalPages);

        // 清理会话文件
        clearSession(sessionId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("taskNo", taskNo);
        result.put("fileName", outputFileName);
        result.put("fileUrl", uploadProperties.getBaseUrl() + datePath + "/" + outputFileName);
        result.put("filePath", datePath + "/" + outputFileName);
        result.put("pageCount", totalPages);
        result.put("fileSize", fileSize);
        result.put("sourceFileCount", files.size());
        result.put("sourceFiles", sourceFileNames);

        log.info("=== 合并完成 ===");
        log.info("文件URL: {}", result.get("fileUrl"));
        log.info("========================================");

        return result;
    }

    /**
     * 获取会话中的文件列表
     */
    public Map<String, Object> getSessionFiles(String sessionId) {
        List<PdfFileInfo> files = sessionFiles.get(sessionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("fileCount", files != null ? files.size() : 0);
        
        if (files != null) {
            List<Map<String, Object>> fileList = new ArrayList<>();
            for (PdfFileInfo file : files) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", file.originalName);
                fileInfo.put("size", file.size);
                fileInfo.put("index", file.index);
                fileList.add(fileInfo);
            }
            result.put("files", fileList);
        }
        
        return result;
    }

    /**
     * 清理会话
     */
    public void clearSession(String sessionId) {
        List<PdfFileInfo> files = sessionFiles.remove(sessionId);
        if (files != null) {
            for (PdfFileInfo file : files) {
                try {
                    File f = new File(file.savedPath);
                    if (f.exists()) {
                        f.delete();
                    }
                } catch (Exception ignored) {}
            }
            // 删除会话目录
            try {
                File sessionDir = new File(uploadProperties.getPath() + File.separator + "temp" + File.separator + sessionId);
                if (sessionDir.exists()) {
                    sessionDir.delete();
                }
            } catch (Exception ignored) {}
        }
    }
}
