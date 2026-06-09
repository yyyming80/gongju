package com.toolbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

/**
 * AI客服服务
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen2.5}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 平台功能介绍（作为系统提示的一部分）
     */
    private static final String PLATFORM_INFO = 
        "你是一个专业的工具箱平台客服助手。请根据以下平台功能信息回答用户的问题。\n\n" +
        "【平台功能】\n" +
        "1. PDF合并：将多个PDF文件合并为一个\n" +
        "   - 进入\"PDF合并\"页面\n" +
        "   - 选择多个PDF文件\n" +
        "   - 点击合并按钮\n" +
        "   - 等待生成完成后可预览或下载\n\n" +
        "2. PDF拆分：将PDF文件拆分成多个\n" +
        "   - 进入\"PDF拆分\"页面\n" +
        "   - 上传PDF文件\n" +
        "   - 输入要拆分的页码（如1-3,5,7）\n" +
        "   - 点击拆分按钮\n\n" +
        "3. 图片转PDF：将多张图片合并为一个PDF\n" +
        "   - 进入\"图片转PDF\"页面\n" +
        "   - 选择多张图片\n" +
        "   - 可调整图片顺序\n" +
        "   - 点击生成PDF\n\n" +
        "4. 图片压缩：压缩图片文件大小\n" +
        "   - 进入\"图片压缩\"页面\n" +
        "   - 选择要压缩的图片\n" +
        "   - 调整压缩质量\n" +
        "   - 点击压缩按钮\n\n" +
        "5. 图片换背景：去除图片背景或更换背景色\n" +
        "   - 进入\"图片换背景\"页面\n" +
        "   - 上传带背景的图片\n" +
        "   - 选择目标背景色\n" +
        "   - 点击处理\n\n" +
        "6. 图片格式转换：转换图片格式（PNG/JPG/BMP/WebP）\n" +
        "   - 进入\"图片转换\"页面\n" +
        "   - 选择图片\n" +
        "   - 选择目标格式\n" +
        "   - 点击转换按钮\n\n" +
        "【使用注意】\n" +
        "- 上传文件大小有限制，请确保文件格式正确\n" +
        "- 如果上传失败，请检查网络连接\n" +
        "- 生成的PDF/图片可以在预览页面查看或下载";

    /**
     * 检查服务是否可用
     */
    public boolean isAvailable() {
        try {
            String checkUrl = ollamaUrl + "/api/tags";
            ResponseEntity<String> response = restTemplate.getForEntity(checkUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Ollama服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 发送消息并获取AI回复
     */
    public String sendMessage(String userMessage) throws Exception {
        log.info("========== AI客服收到消息 ==========");
        log.info("用户消息: {}", userMessage);

        if (!isAvailable()) {
            return "抱歉，AI客服暂时不可用。请确保Ollama服务已启动（运行命令：ollama serve）。";
        }

        // 构建完整的提示
        String fullPrompt = buildPrompt(userMessage);

        try {
            String generateUrl = ollamaUrl + "/api/generate";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", fullPrompt);
            requestBody.put("stream", false);
            
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.7);
            options.put("top_p", 0.9);
            options.put("num_predict", 1024);
            requestBody.put("options", options);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("正在调用Ollama API...");
            ResponseEntity<String> response = restTemplate.exchange(
                generateUrl,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                String reply = (String) responseMap.get("response");
                
                log.info("AI回复长度: {}", reply != null ? reply.length() : 0);
                return reply != null ? reply.trim() : "抱歉，我暂时无法回答这个问题。";
            } else {
                log.error("API调用失败: {}", response.getStatusCode());
                return "抱歉，AI客服暂时无法回答您的问题，请稍后再试。";
            }

        } catch (Exception e) {
            log.error("调用Ollama失败: {}", e.getMessage());
            return "抱歉，AI客服暂时无法回答您的问题，请稍后再试。";
        }
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(PLATFORM_INFO);
        prompt.append("\n\n");
        prompt.append("【用户问题】\n");
        prompt.append(userMessage);
        prompt.append("\n\n");
        prompt.append("请根据平台功能信息回答用户的问题。如果用户询问的功能不在上述列表中，请告知用户该功能暂不支持。");
        prompt.append("回答要简洁、专业、有帮助。");
        return prompt.toString();
    }
}
