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
 * Ollama 本地模型客户端
 */
@Service
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen2.5}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查Ollama服务是否可用
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
     * 检查模型是否已下载
     */
    public boolean isModelReady() {
        try {
            String tagsUrl = ollamaUrl + "/api/tags";
            ResponseEntity<String> response = restTemplate.getForEntity(tagsUrl, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> models = (List<Map<String, Object>>) result.get("models");
                
                if (models != null) {
                    for (Map<String, Object> m : models) {
                        String name = (String) m.get("name");
                        if (name != null && name.startsWith(model)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("检查模型失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 调用Ollama生成文本
     * @param prompt 提示词
     * @return 生成的文本
     */
    public String generate(String prompt) throws Exception {
        log.info("========== 调用Ollama生成文本 ==========");
        log.info("模型: {}", model);
        log.info("Prompt长度: {}", prompt.length());

        // 检查服务是否可用
        if (!isAvailable()) {
            throw new RuntimeException("Ollama服务未启动，请先运行: ollama serve");
        }

        // 检查模型是否已下载
        if (!isModelReady()) {
            throw new RuntimeException("模型 " + model + " 未下载，请先运行: ollama pull " + model);
        }

        try {
            String generateUrl = ollamaUrl + "/api/generate";
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            
            // options 使用 HashMap（兼容 Java 8）
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.7);
            options.put("top_p", 0.9);
            options.put("num_predict", 4096);
            requestBody.put("options", options);

            // 设置请求头
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
                String responseBody = response.getBody();
                log.info("API响应长度: {}", responseBody.length());
                
                // 解析响应
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                String content = (String) responseMap.get("response");
                
                log.info("生成内容长度: {}", content != null ? content.length() : 0);
                log.info("========================================");
                
                return content;
            } else {
                throw new RuntimeException("Ollama API调用失败: " + response.getStatusCode());
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用Ollama失败: {}", e.getMessage(), e);
            throw new RuntimeException("调用Ollama失败: " + e.getMessage());
        }
    }

    /**
     * 生成简历（带系统提示）
     */
    public String generateWithSystemPrompt(String systemPrompt, String userPrompt) throws Exception {
        log.info("========== 带系统提示调用Ollama ==========");
        
        if (!isAvailable()) {
            throw new RuntimeException("Ollama服务未启动，请先运行: ollama serve");
        }

        try {
            String generateUrl = ollamaUrl + "/api/generate";
            
            // 构建请求体（Ollama使用单prompt格式，系统提示放在开头）
            String fullPrompt = systemPrompt + "\n\n" + userPrompt;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", fullPrompt);
            requestBody.put("stream", false);
            
            // options 使用 HashMap（兼容 Java 8）
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.7);
            options.put("top_p", 0.9);
            options.put("num_predict", 4096);
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
                String content = (String) responseMap.get("response");
                log.info("生成内容长度: {}", content != null ? content.length() : 0);
                return content;
            } else {
                throw new RuntimeException("Ollama API调用失败");
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用Ollama失败: {}", e.getMessage());
            throw new RuntimeException("调用Ollama失败: " + e.getMessage());
        }
    }
}
