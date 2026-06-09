package com.toolbox.service;

import com.toolbox.config.UploadProperties;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PdfBatchService {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UploadProperties uploadProperties;

    // 存储会话中的图片文件路径
    private final Map<String, List<String>> sessionImages = new ConcurrentHashMap<>();

    /**
     * 上传单张图片到会话
     */
    public Map<String, Object> uploadImage(String sessionId, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请上传图片文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        String lowerName = filename.toLowerCase();
        if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg") &&
            !lowerName.endsWith(".png") && !lowerName.endsWith(".bmp") &&
            !lowerName.endsWith(".gif")) {
            throw new RuntimeException("仅支持 JPG、PNG、BMP、GIF 格式");
        }

        // 保存图片到临时目录
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String tempDir = uploadProperties.getPath() + File.separator + "temp" + File.separator + sessionId;
        File dir = new File(tempDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String ext = filename.substring(filename.lastIndexOf("."));
        String savedFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        String savedFilePath = tempDir + File.separator + savedFileName;
        file.transferTo(new File(savedFilePath));

        // 添加到会话列表
        sessionImages.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
                     .add(savedFilePath);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("uploadedCount", sessionImages.get(sessionId).size());
        result.put("filePath", "temp/" + sessionId + "/" + savedFileName);
        return result;
    }

    /**
     * 将会话中的图片转换为PDF
     */
    public Map<String, Object> convert(String sessionId, Integer fileCount) throws Exception {
        List<String> imagePaths = sessionImages.get(sessionId);
        if (imagePaths == null || imagePaths.isEmpty()) {
            throw new RuntimeException("没有上传的图片文件");
        }

        String taskNo = "IMG2PDF_" + System.currentTimeMillis();
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String outputDir = uploadProperties.getPath() + File.separator + datePath;
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        PDDocument doc = new PDDocument();
        int pageCount = 0;

        try {
            for (String imagePath : imagePaths) {
                File imageFile = new File(imagePath);
                if (!imageFile.exists()) {
                    continue;
                }

                BufferedImage image = ImageIO.read(imageFile);
                if (image == null) {
                    continue;
                }

                PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
                doc.addPage(page);

                PDImageXObject pdImage = PDImageXObject.createFromFileByContent(imageFile, doc);
                PDPageContentStream contentStream = new PDPageContentStream(doc, page);
                contentStream.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
                contentStream.close();
                pageCount++;
            }

            if (doc.getNumberOfPages() == 0) {
                doc.close();
                throw new RuntimeException("没有有效的图片文件");
            }

            String outputFileName = taskNo + ".pdf";
            String outputFilePath = outputDir + File.separator + outputFileName;
            doc.save(outputFilePath);

            File outputFile = new File(outputFilePath);
            long fileSize = outputFile.length();

            Map<String, Object> result = new HashMap<>();
            result.put("taskNo", taskNo);
            result.put("fileName", outputFileName);
            result.put("fileUrl", uploadProperties.getBaseUrl() + datePath + "/" + outputFileName);
            result.put("filePath", datePath + "/" + outputFileName);
            result.put("pageCount", pageCount);
            result.put("fileSize", fileSize);

            return result;

        } finally {
            doc.close();
            // 清理临时文件
            clearSession(sessionId);
        }
    }

    /**
     * 清除会话
     */
    public void clearSession(String sessionId) {
        List<String> imagePaths = sessionImages.remove(sessionId);
        if (imagePaths != null) {
            for (String path : imagePaths) {
                try {
                    File f = new File(path);
                    if (f.exists()) {
                        f.delete();
                    }
                    // 尝试删除会话目录
                    File sessionDir = f.getParentFile();
                    if (sessionDir != null && sessionDir.exists()) {
                        sessionDir.delete();
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
