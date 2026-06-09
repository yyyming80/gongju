package com.toolbox.service.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * AI客服回复服务
 */
@Service
public class AiCustomerService {

    private static final Logger log = LoggerFactory.getLogger(AiCustomerService.class);

    // 转人工关键词
    private static final List<String> TRANSFER_KEYWORDS = Arrays.asList(
        "转人工", "人工", "客服", "投诉", "投诉"
    );

    /**
     * AI回复
     */
    public String chat(String content, String userId) {
        log.info("AI回复: userId={}, content={}", userId, content);
        
        // 简单匹配回复
        String lower = content.toLowerCase();
        
        if (lower.contains("你好") || lower.contains("在吗") || lower.contains("hi") || lower.contains("hello")) {
            return "您好，请问有什么可以帮助您的？";
        }
        
        if (lower.contains("pdf") || lower.contains("合并")) {
            return "PDF合并功能可以帮助您将多个PDF文件合并为一个。请进入'PDF合并'页面，选择要合并的文件后点击合并按钮。";
        }
        
        if (lower.contains("图片") && (lower.contains("压缩") || lower.contains("大") || lower.contains("小")) {
            return "图片压缩可以将图片文件变小。进入'图片压缩'页面，选择图片并调整质量后压缩。";
        }
        
        if (lower.contains("转换") || lower.contains("格式")) {
            return "图片格式转换支持PNG/JPG/BMP/WebP互转。进入'图片转换'页面选择图片和目标格式即可。";
        }
        
        if (lower.contains("登录") || lower.contains("注册")) {
            return "点击首页'账户设置'进入登录页面，使用微信登录。";
        }
        
        return "您好，我已收到您的问题。请问还有什么可以帮到您？";
    }

    /**
     * 是否应该转人工
     */
    public boolean shouldTransferHuman(String content) {
        String lower = content.toLowerCase();
        for (String keyword : TRANSFER_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
