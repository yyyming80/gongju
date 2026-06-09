package com.toolbox.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.UUID;

/**
 * JWT工具类（简化版，可替换为真实JWT库如jjwt）
 */
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    
    // Token过期时间（毫秒）：7天
    private static final long EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;
    
    // Token前缀
    private static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 生成Token
     */
    public static String generateToken(String openid) {
        long timestamp = System.currentTimeMillis();
        String token = encrypt(openid, timestamp);
        log.debug("生成Token: openid={}, expire={}", openid, EXPIRE_TIME);
        return token;
    }

    /**
     * 验证Token
     */
    public static boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        try {
            TokenInfo info = parseToken(token);
            if (info == null) {
                return false;
            }
            
            // 检查是否过期
            if (System.currentTimeMillis() - info.timestamp > EXPIRE_TIME) {
                log.debug("Token已过期");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Token获取openid
     */
    public static String getOpenidFromToken(String token) {
        try {
            TokenInfo info = parseToken(token);
            return info != null ? info.openid : null;
        } catch (Exception e) {
            log.warn("解析Token失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从请求头获取Token
     */
    public static String getTokenFromRequest(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith(TOKEN_PREFIX)) {
            return auth.substring(TOKEN_PREFIX.length());
        }
        return auth;
    }

    /**
     * 简单的加密（实际项目建议使用JWT库）
     */
    private static String encrypt(String openid, long timestamp) {
        String raw = openid + ":" + timestamp + ":" + UUID.randomUUID().toString();
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    /**
     * 解析Token
     */
    private static TokenInfo parseToken(String token) {
        try {
            String raw = new String(Base64.getDecoder().decode(token));
            String[] parts = raw.split(":");
            if (parts.length < 2) {
                return null;
            }
            TokenInfo info = new TokenInfo();
            info.openid = parts[0];
            info.timestamp = Long.parseLong(parts[1]);
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Token信息内部类
     */
    private static class TokenInfo {
        String openid;
        long timestamp;
    }
}
