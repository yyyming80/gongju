package com.toolbox.controller;

import com.toolbox.common.Result;
import com.toolbox.service.ChatService;
import com.toolbox.service.customer.CustomerSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI客服控制器
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private CustomerSessionService sessionService;

    /**
     * 转人工关键词列表
     */
    private static final Set<String> TRANSFER_KEYWORDS = new HashSet<>(Arrays.asList(
            "转人工", "人工客服", "联系客服", "人工服务",
            "人工", "客服", "投诉", "退款"
    ));

    /**
     * 检查服务状态
     */
    @GetMapping("/check")
    public Result<Map<String, Object>> check() {
        Map<String, Object> result = new HashMap<>();
        result.put("available", chatService.isAvailable());
        result.put("message", chatService.isAvailable() 
            ? "服务正常" 
            : "服务未启动，请运行: ollama serve");
        return Result.success("检查完成", result);
    }

    /**
     * 发送消息
     * POST /api/chat/send
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestBody Map<String, Object> request) {
        log.info("========== AI客服收到请求 ==========");
        
        String message = (String) request.get("message");
        String userId = (String) request.get("userId");
        String userNickname = (String) request.get("userNickname");
        String userAvatar = (String) request.get("userAvatar");
        String sessionNo = (String) request.get("sessionNo");
        
        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息不能为空");
        }
        
        // 检查是否包含转人工关键词
        if (shouldTransferToHuman(message)) {
            log.info("检测到转人工关键词: {}", message);
            return handleTransferToHuman(message, userId, userNickname, userAvatar, sessionNo);
        }
        
        // 继续调用AI
        try {
            String reply = chatService.sendMessage(message.trim());
            
            Map<String, Object> result = new HashMap<>();
            result.put("reply", reply);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("回复: {}", reply);
            return Result.success("获取回复成功", result);
            
        } catch (Exception e) {
            log.error("处理失败: {}", e.getMessage());
            return Result.error("AI客服暂时无法回答: " + e.getMessage());
        }
    }

    /**
     * 检查是否应该转人工
     */
    private boolean shouldTransferToHuman(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase().trim();
        for (String keyword : TRANSFER_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                log.info("匹配到转人工关键词: {}", keyword);
                return true;
            }
        }
        return false;
    }

    /**
     * 处理转人工请求
     */
    private Result<Map<String, Object>> handleTransferToHuman(String message, 
                                                               String userId, 
                                                               String userNickname, 
                                                               String userAvatar,
                                                               String existingSessionNo) {
        try {
            // 生成新的会话编号
            String sessionNo = existingSessionNo;
            if (sessionNo == null || sessionNo.isEmpty()) {
                sessionNo = "sess_" + System.currentTimeMillis();
            }
            
            // 创建客服会话
            sessionService.createSession(sessionNo, userId, userNickname, userAvatar);
            
            // 更新会话状态为等待转人工
            sessionService.updateSessionStatus(sessionNo, 1, message);
            
            log.info("创建客服会话成功: sessionNo={}", sessionNo);
            
            Map<String, Object> result = new HashMap<>();
            result.put("transfer", true);
            result.put("sessionNo", sessionNo);
            result.put("message", "正在为您转接人工客服，请稍候...");
            result.put("timestamp", System.currentTimeMillis());
            
            return Result.success("已转接人工客服", result);
            
        } catch (Exception e) {
            log.error("转人工处理失败: {}", e.getMessage());
            return Result.error("转接人工客服失败: " + e.getMessage());
        }
    }
}