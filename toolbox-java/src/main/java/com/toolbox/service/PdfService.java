package com.toolbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolbox.config.UploadProperties;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PdfService {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UploadProperties uploadProperties;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> merge(Integer userId, List<MultipartFile> files) {
        if (files == null || files.size() < 2) {
            throw new RuntimeException("至少需要2个PDF文件");
        }

        String taskNo = "MERGE_" + System.currentTimeMillis();
        List<String> sourceFileNames = new ArrayList<>();
        PDDocument outputDoc = new PDDocument();
        int totalPages = 0;

        try {
            for (MultipartFile file : files) {
                if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                    continue;
                }
                sourceFileNames.add(file.getOriginalFilename());
                PDDocument doc = PDDocument.load(file.getBytes());
                for (int i = 0; i < doc.getNumberOfPages(); i++) {
                    outputDoc.addPage(doc.getPage(i));
                }
                totalPages += doc.getNumberOfPages();
                doc.close();
            }

            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String outputDir = uploadProperties.getPath() + File.separator + datePath;
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String outputFileName = taskNo + ".pdf";
            String outputFilePath = outputDir + File.separator + outputFileName;
            outputDoc.save(outputFilePath);
            outputDoc.close();

            File outputFile = new File(outputFilePath);
            long fileSize = outputFile.length();

            String sourceFilesJson = objectMapper.writeValueAsString(sourceFileNames);
            jdbc.update(
                "INSERT INTO pdf_merge_record (user_id, task_no, source_files, output_file, file_count, page_count, file_size, status, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, 1, NOW())",
                userId, taskNo, sourceFilesJson, outputFileName, files.size(), totalPages, fileSize
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskNo", taskNo);
            result.put("fileName", outputFileName);
            result.put("fileUrl", uploadProperties.getBaseUrl() + datePath + "/" + outputFileName);
            result.put("filePath", datePath + "/" + outputFileName);
            result.put("fileCount", files.size());
            result.put("pageCount", totalPages);
            result.put("fileSize", fileSize);
            return result;

        } catch (Exception e) {
            try {
                outputDoc.close();
            } catch (Exception ignored) {}
            throw new RuntimeException("PDF合并失败: " + e.getMessage());
        }
    }

    public Map<String, Object> split(Integer userId, MultipartFile file, String pageRange) {
        if (file == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("请上传有效的PDF文件");
        }

        String taskNo = "SPLIT_" + System.currentTimeMillis();
        List<Map<String, Object>> outputFiles = new ArrayList<>();

        try {
            PDDocument doc = PDDocument.load(file.getBytes());
            int totalPages = doc.getNumberOfPages();

            List<Integer> pages = parsePageRange(pageRange, totalPages);
            if (pages.isEmpty()) {
                doc.close();
                throw new RuntimeException("页码范围无效");
            }

            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String outputDir = uploadProperties.getPath() + File.separator + datePath;
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            for (int pageNum : pages) {
                if (pageNum < 1 || pageNum > totalPages) {
                    continue;
                }

                PDDocument singleDoc = new PDDocument();
                singleDoc.addPage(doc.getPage(pageNum - 1));

                String singleFileName = taskNo + "_page_" + pageNum + ".pdf";
                String singleFilePath = outputDir + File.separator + singleFileName;
                singleDoc.save(singleFilePath);
                singleDoc.close();

                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("page", pageNum);
                fileInfo.put("fileName", singleFileName);
                fileInfo.put("fileUrl", uploadProperties.getBaseUrl() + datePath + "/" + singleFileName);
                fileInfo.put("filePath", datePath + "/" + singleFileName);
                outputFiles.add(fileInfo);
            }
            doc.close();

            String zipFileName = taskNo + ".zip";
            String zipFilePath = outputDir + File.separator + zipFileName;
            createZipFile(outputFiles, outputDir, zipFilePath);

            String outputFilesJson = objectMapper.writeValueAsString(outputFiles);
            jdbc.update(
                "INSERT INTO pdf_split_record (user_id, task_no, source_file, source_file_name, output_files, page_range, total_pages, split_count, status, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, NOW())",
                userId, taskNo, file.getOriginalFilename(), file.getOriginalFilename(), outputFilesJson, pageRange, totalPages, outputFiles.size()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskNo", taskNo);
            result.put("files", outputFiles);
            result.put("zipFileName", zipFileName);
            result.put("zipFileUrl", uploadProperties.getBaseUrl() + datePath + "/" + zipFileName);
            result.put("zipFilePath", datePath + "/" + zipFileName);
            result.put("totalPages", totalPages);
            result.put("splitCount", outputFiles.size());
            return result;

        } catch (Exception e) {
            throw new RuntimeException("PDF拆分失败: " + e.getMessage());
        }
    }

    public Map<String, Object> wordToPdf(Integer userId, MultipartFile file) {
        if (file == null) {
            throw new RuntimeException("请上传文件");
        }

        String originalFilename = file.getOriginalFilename().toLowerCase();
        if (!originalFilename.endsWith(".doc") && !originalFilename.endsWith(".docx")) {
            throw new RuntimeException("仅支持.doc和.docx格式的Word文件");
        }

        String taskNo = "WORD2PDF_" + System.currentTimeMillis();

        try {
            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String outputDir = uploadProperties.getPath() + File.separator + datePath;
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String outputFileName = taskNo + ".pdf";
            String outputFilePath = outputDir + File.separator + outputFileName;

            PDDocument doc = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            contentStream.beginText();
            contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("Word转PDF功能演示 - 需要LibreOffice或其他专业转换库");
            contentStream.newLine();
            contentStream.showText("文件名: " + file.getOriginalFilename());
            contentStream.newLine();
            contentStream.showText("文件大小: " + file.getSize() + " bytes");
            contentStream.endText();
            contentStream.close();

            doc.save(outputFilePath);
            doc.close();

            File outputFile = new File(outputFilePath);
            long fileSize = outputFile.length();

            jdbc.update(
                "INSERT INTO word_to_pdf_record (user_id, task_no, source_file, source_file_name, output_file, file_size, status, create_time) VALUES (?, ?, ?, ?, ?, ?, 1, NOW())",
                userId, taskNo, file.getOriginalFilename(), file.getOriginalFilename(), outputFileName, fileSize
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskNo", taskNo);
            result.put("fileName", outputFileName);
            result.put("fileUrl", uploadProperties.getBaseUrl() + datePath + "/" + outputFileName);
            result.put("filePath", datePath + "/" + outputFileName);
            result.put("fileSize", fileSize);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Word转PDF失败: " + e.getMessage());
        }
    }

    public Map<String, Object> imageToPdf(Integer userId, List<MultipartFile> files, List<Integer> sortOrder) {
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("请上传图片文件");
        }

        String taskNo = "IMG2PDF_" + System.currentTimeMillis();
        List<String> sourceFileNames = new ArrayList<>();

        try {
            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String outputDir = uploadProperties.getPath() + File.separator + datePath;
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            PDDocument doc = new PDDocument();

            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String filename = file.getOriginalFilename().toLowerCase();
                if (!filename.endsWith(".jpg") && !filename.endsWith(".jpeg") &&
                    !filename.endsWith(".png") && !filename.endsWith(".bmp")) {
                    continue;
                }

                sourceFileNames.add(file.getOriginalFilename());

                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image == null) {
                    continue;
                }

                PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
                doc.addPage(page);

                File tempFile = File.createTempFile("img2pdf_", ".tmp");
                file.transferTo(tempFile);

                PDImageXObject pdImage = PDImageXObject.createFromFileByContent(tempFile, doc);
                PDPageContentStream contentStream = new PDPageContentStream(doc, page);
                contentStream.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
                contentStream.close();

                tempFile.delete();
            }

            if (doc.getNumberOfPages() == 0) {
                doc.close();
                throw new RuntimeException("没有有效的图片文件");
            }

            String outputFileName = taskNo + ".pdf";
            String outputFilePath = outputDir + File.separator + outputFileName;
            doc.save(outputFilePath);
            doc.close();

            File outputFile = new File(outputFilePath);
            long fileSize = outputFile.length();

            String sourceFilesJson = objectMapper.writeValueAsString(sourceFileNames);
            String sortOrderJson = sortOrder != null ? objectMapper.writeValueAsString(sortOrder) : null;

            jdbc.update(
                "INSERT INTO image_to_pdf_record (user_id, task_no, source_files, output_file, image_count, page_count, file_size, sort_order, status, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, NOW())",
                userId, taskNo, sourceFilesJson, outputFileName, files.size(), sourceFileNames.size(), fileSize, sortOrderJson
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskNo", taskNo);
            result.put("fileName", outputFileName);
            result.put("fileUrl", uploadProperties.getBaseUrl() + datePath + "/" + outputFileName);
            result.put("filePath", datePath + "/" + outputFileName);
            result.put("imageCount", sourceFileNames.size());
            result.put("pageCount", sourceFileNames.size());
            result.put("fileSize", fileSize);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("图片转PDF失败: " + e.getMessage());
        }
    }

    public Map<String, Object> pdfToImage(Integer userId, MultipartFile file, Integer page) {
        try {
            PDDocument doc = PDDocument.load(file.getBytes());
            int totalPages = doc.getNumberOfPages();
            if (page == null || page < 1 || page > totalPages) {
                page = 1;
            }

            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(page - 1, 300);
            String taskNo = "PDF2IMG_" + System.currentTimeMillis();
            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String outputDir = uploadProperties.getPath() + File.separator + datePath;
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String outputFileName = taskNo + ".png";
            String outputFilePath = outputDir + File.separator + outputFileName;
            ImageIO.write(image, "png", new File(outputFilePath));
            doc.close();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskNo", taskNo);
            result.put("fileName", outputFileName);
            result.put("fileUrl", uploadProperties.getBaseUrl() + datePath + "/" + outputFileName);
            result.put("filePath", datePath + "/" + outputFileName);
            result.put("width", image.getWidth());
            result.put("height", image.getHeight());
            result.put("totalPages", totalPages);
            result.put("currentPage", page);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("PDF转图片失败: " + e.getMessage());
        }
    }

    public Map<String, Object> getPdfInfo(MultipartFile file) {
        try {
            PDDocument doc = PDDocument.load(file.getBytes());
            Map<String, Object> info = new HashMap<>();
            info.put("pages", doc.getNumberOfPages());
            info.put("fileName", file.getOriginalFilename());
            info.put("fileSize", file.getSize());
            doc.close();
            return info;
        } catch (Exception e) {
            throw new RuntimeException("获取PDF信息失败: " + e.getMessage());
        }
    }

    private List<Integer> parsePageRange(String range, int maxPages) {
        List<Integer> pages = new ArrayList<>();
        if (range == null || range.trim().isEmpty()) {
            for (int i = 1; i <= maxPages; i++) {
                pages.add(i);
            }
            return pages;
        }

        range = range.trim();
        String[] parts = range.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] bounds = part.split("-");
                try {
                    int start = Integer.parseInt(bounds[0].trim());
                    int end = bounds.length > 1 ? Integer.parseInt(bounds[1].trim()) : maxPages;
                    for (int i = start; i <= end; i++) {
                        if (i >= 1 && i <= maxPages && !pages.contains(i)) {
                            pages.add(i);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            } else {
                try {
                    int page = Integer.parseInt(part.trim());
                    if (page >= 1 && page <= maxPages && !pages.contains(page)) {
                        pages.add(page);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        Collections.sort(pages);
        return pages;
    }

    private void createZipFile(List<Map<String, Object>> files, String baseDir, String zipFilePath) throws Exception {
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos);

        for (Map<String, Object> fileInfo : files) {
            String fileName = (String) fileInfo.get("fileName");
            File sourceFile = new File(baseDir + File.separator + fileName);
            if (sourceFile.exists()) {
                FileInputStream fis = new FileInputStream(sourceFile);
                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                fis.close();
            }
        }
        zos.close();
        fos.close();
    }
}
