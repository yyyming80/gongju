package com.toolbox.controller;

import com.toolbox.config.UploadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private UploadProperties uploadProperties;

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            String originalFilename = file.getOriginalFilename();
            String ext = getFileExtension(originalFilename);
            String fileNo = "F" + System.currentTimeMillis() + (int) (Math.random() * 1000);
            String fileName = fileNo + "." + ext;

            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String uploadDir = uploadProperties.getPath() + File.separator + datePath;
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filePath = datePath + File.separator + fileName;
            File dest = new File(uploadProperties.getPath(), filePath);
            file.transferTo(dest);

            result.put("success", true);
            result.put("fileNo", fileNo);
            result.put("fileName", fileName);
            result.put("originalName", originalFilename);
            result.put("filePath", filePath);
            result.put("fileUrl", uploadProperties.getBaseUrl() + datePath + "/" + fileName);
            result.put("fileSize", file.getSize());
            result.put("fileType", ext);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/download/{datePath1}/{datePath2}/{datePath3}/{fileName:.+}")
    public ResponseEntity<Resource> download(
            @PathVariable String datePath1,
            @PathVariable String datePath2,
            @PathVariable String datePath3,
            @PathVariable String fileName) {
        try {
            String filePath = datePath1 + "/" + datePath2 + "/" + datePath3 + "/" + fileName;
            Path path = Paths.get(uploadProperties.getPath(), filePath);
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(path.toFile());
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }
}
