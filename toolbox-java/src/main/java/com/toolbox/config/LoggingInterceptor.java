package com.toolbox.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * 请求日志拦截器
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    
    private static final String REQUEST_ID = "requestId";
    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 生成请求ID
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(REQUEST_ID, requestId);
        
        long startTime = System.currentTimeMillis();
        MDC.put(START_TIME, String.valueOf(startTime));
        
        // 记录请求信息
        log.info(">>> [{}] {} {} - IP:{}", 
            requestId,
            request.getMethod(),
            request.getRequestURI(),
            getClientIP(request));
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
            Object handler, Exception ex) {
        long startTime = Long.parseLong(MDC.get(START_TIME));
        long duration = System.currentTimeMillis() - startTime;
        String requestId = MDC.get(REQUEST_ID);
        
        // 记录响应信息
        if (ex != null) {
            log.error("<<< [{}] {} {} - 耗时:{}ms - 异常:{}", 
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                duration,
                ex.getMessage());
        } else {
            log.info("<<< [{}] {} {} - 耗时:{}ms - 状态:{}", 
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                duration,
                response.getStatus());
        }
        
        // 清理MDC
        MDC.clear();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
