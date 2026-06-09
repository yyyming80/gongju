package com.toolbox.service.customer;

import com.toolbox.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * AI客服服务
 */
@Service
public class AiCustomerService {

    private static final Logger log = LoggerFactory.getLogger(AiCustomerService.class);

    @Autowired
    private ChatService chatService;

    // 转人工关键词
    private static final Set<String> TRANSFER_KEYWORDS = new HashSet<>(Arrays.asList(
            "转人工", "人工客服", "人工服务", "投诉", "退款", "还钱",
            "退款", "要退款", "退款处理", "投诉", "举报", "骗子",
            "太差了", "不满意", "没用", "垃圾", "非常不满"
    ));

    // AI无法处理的问题类型
    private static final Set<String> AI_WEAK_TOPICS = new HashSet<>(Arrays.asList(
            "退款", "投诉", "举报", "账户安全", "资金问题",
            "支付问题", "退款处理", "严重问题"
    ));

    /**
     * 获取AI回复
     */
    public String getResponse(String userMessage) {
        try {
            return chatService.sendMessage(userMessage);
        } catch (Exception e) {
            log.error("AI回复失败", e);
            return "抱歉，我现在无法回答您的问题，请稍后再试或转人工客服。";
        }
    }

    /**
     * 判断是否需要转人工
     */
    public boolean shouldTransferHuman(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        // 检查转人工关键词
        for (String keyword : TRANSFER_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                log.info("检测到转人工关键词: {}", keyword);
                return true;
            }
        }

        // 检查AI弱势领域
        for (String topic : AI_WEAK_TOPICS) {
            if (lowerMessage.contains(topic)) {
                log.info("检测到AI弱势领域: {}", topic);
                return true;
            }
        }

        return false;
    }

    /**
     * 获取欢迎消息
     */
    public String getWelcomeMessage() {
        return "您好！我是智能客服助手，请问有什么可以帮助您？\n\n" +
               "您可以：\n" +
               "• 咨询平台功能使用方法\n" +
               "• 询问常见问题\n" +
               "• 反馈问题或建议\n\n" +
               "如需人工帮助，请回复\"转人工\"。";
    }
}