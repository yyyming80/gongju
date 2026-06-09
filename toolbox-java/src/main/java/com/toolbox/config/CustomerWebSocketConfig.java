package com.toolbox.config;

import com.toolbox.handler.CustomerWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 客服系统 WebSocket配置
 */
@Configuration
@EnableWebSocket
public class CustomerWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private CustomerWebSocketHandler customerWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(customerWebSocketHandler, "/ws/customer")
                .setAllowedOrigins("*");
    }
}