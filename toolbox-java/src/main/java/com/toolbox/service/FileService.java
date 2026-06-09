package com.toolbox.service;

import com.toolbox.config.UploadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class FileService {

    @Autowired
    private UploadProperties uploadProperties;

    /**
     * 上传文件
     */
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(originalFilename);
        String fileNo = generateFileNo();
        String fileName = fileNo + "." + ext;
        
        // 创建存储目录
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        File dir = new File(uploadProperties.getPath() + "/" + datePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 保存文件
        String filePath = datePath + "/" + fileName;
        File dest = new File(uploadProperties.getPath(), filePath);
        file.transferTo(dest);
        
        Map<String, Object> result = new HashMap<>();
        result.put("fileNo", fileNo);
        result.put("fileName", originalFilename);
        result.put("filePath", filePath);
        result.put("fileUrl", uploadProperties.getBaseUrl() + filePath);
        result.put("fileSize", file.getSize());
        result.put("fileType", ext);
        
        return result;
    }

    /**
     * 获取文件路径
     */
    public String getFilePath(String filePath) {
        return uploadProperties.getPath() + "/" + filePath;
    }

    /**
     * 下载文件
     */
    public byte[] download(String filePath) throws Exception {
        File file = new File(uploadProperties.getPath(), filePath);
        if (!file.exists()) {
            throw new RuntimeException("文件不存在");
        }
        return Files.readAllBytes(file.toPath());
    }

    /**
     * 删除文件
     */
    public void delete(String filePath) {
        File file = new File(uploadProperties.getPath(), filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 生成文件编号
     */
    private String generateFileNo() {
        return "F" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * 获取文件扩展名
     */
    public String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }
}
