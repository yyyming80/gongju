package com.toolbox.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolbox.service.customer.AiCustomerService;
import com.toolbox.service.customer.CustomerMessageService;
import com.toolbox.service.customer.CustomerSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客服系统 WebSocket处理器
 */
@Component
public class CustomerWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomerWebSocketHandler.class);

    @Autowired
    private CustomerSessionService sessionService;

    @Autowired
    private CustomerMessageService messageService;

    @Autowired
    private AiCustomerService aiCustomerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // sessionNo -> WebSocketSession
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // agentId -> WebSocketSession (客服工作台)
    private final Map<Long, WebSocketSession> agentSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: {}", session.getId());

        // 从session中获取参数
        String sessionNo = getParam(session, "sessionNo");
        String userId = getParam(session, "userId");
        String userType = getParam(session, "userType"); // user or agent

        if ("agent".equals(userType)) {
            // 客服工作台连接
            Long agentId = Long.parseLong(userId);
            agentSessions.put(agentId, session);
            log.info("客服连接: agentId={}", agentId);
        } else {
            // 用户连接
            log.info("========== 用户建立连接 ==========");
            log.info("sessionNo={}", sessionNo);
            log.info("userId={}", userId);
            
            userSessions.put(sessionNo, session);
            
            log.info("当前所有连接={}", userSessions.keySet());
            log.info("=================================");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到消息: {}", payload);

        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        String action = (String) data.get("action");

        switch (action) {
            case "start_session":
                handleStartSession(session, data);
                break;
            case "send_message":
                handleSendMessage(session, data);
                break;
            case "transfer_to_human":
                handleTransferToHuman(session, data);
                break;
            case "end_session":
                handleEndSession(session, data);
                break;
            case "agent_accept":
                handleAgentAccept(session, data);
                break;
            case "ping":
                session.sendMessage(new TextMessage("{\"action\":\"pong\"}"));
                break;
            default:
                log.warn("未知动作: {}", action);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket连接关闭: {}, status={}", session.getId(), status);

        // 清理session
        userSessions.values().removeIf(s -> s.getId().equals(session.getId()));
        agentSessions.values().removeIf(s -> s.getId().equals(session.getId()));
    }

    /**
     * 开始会话
     */
    private void handleStartSession(WebSocketSession session, Map<String, Object> data) throws IOException {
        log.info("========== handleStartSession 进入 ==========");
        log.info("收到data: {}", data);
        
        String sessionNo = (String) data.get("sessionNo");
        String userId = (String) data.get("userId");
        String userNickname = (String) data.get("userNickname");
        String userAvatar = (String) data.get("userAvatar");

        // 创建会话
        sessionService.createSession(sessionNo, userId, userNickname, userAvatar);

        // 保存session映射
        userSessions.put(sessionNo, session);

        // 发送欢迎消息（使用统一来源）
        String welcomeMsg = aiCustomerService.getWelcomeMessage();
        messageService.saveMessage(sessionNo, "msg_" + System.currentTimeMillis(),
                1, welcomeMsg, (byte) 2, "AI", "智能客服");

        // 发送欢迎消息给用户
        sendToUser(sessionNo, createMessage("ai", welcomeMsg, "AI", "智能客服"));
    }

    /**
     * 处理发送消息
     */
    private void handleSendMessage(WebSocketSession session, Map<String, Object> data) throws IOException {
        // 诊断日志：打印收到的完整WebSocket消息
        log.info("========== handleSendMessage 诊断日志 ==========");
        log.info("收到完整WebSocket消息: {}", data);
        log.info("各字段类型:");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            log.info("  {} = {} (class: {})", entry.getKey(), entry.getValue(), 
                    entry.getValue() != null ? entry.getValue().getClass().getName() : "null");
        }
        log.info("WebSocket session.getId() = {}", session.getId());
        
        // 兼容处理：Integer、Long、String三种类型
        Object sessionNoObj = data.get("sessionNo");
        Object contentObj = data.get("content");
        Object msgIdObj = data.get("msgId");
        Object senderIdObj = data.get("senderId");
        Object senderNicknameObj = data.get("senderNickname");
        
        String sessionNo = objToString(sessionNoObj);
        String content = objToString(contentObj);
        String msgId = objToString(msgIdObj);
        String senderId = objToString(senderIdObj);
        String senderNickname = objToString(senderNicknameObj);
        
        log.info("转换后: sessionNo={}, content={}, msgId={}, senderId={}, senderNickname={}", 
                sessionNo, content, msgId, senderId, senderNickname);
        
        // 判断发送者是客服还是用户
        String userType = getParam(session, "userType");
        log.info("userType = {}", userType);
        
        // 获取会话状态
        int status = sessionService.getSessionStatus(sessionNo);
        log.info("sessionStatus = {}", status);

        if (status == 0) {
            // AI接待中
            // 保存用户消息
            messageService.saveMessage(sessionNo, msgId, 1, content, (byte) 1, senderId, senderNickname);
            // 更新会话最后消息
            sessionService.updateLastMessage(sessionNo, content);
            
            // 调用AI服务
            String aiResponse = aiCustomerService.getResponse(content);
            boolean shouldTransfer = AiCustomerService.shouldTransfer(content);

            // 保存AI消息
            messageService.saveMessage(sessionNo, "msg_ai_" + System.currentTimeMillis(),
                    1, aiResponse, (byte) 2, "AI", "智能客服");

            // 发送AI回复
            sendToUser(sessionNo, createMessage("ai", aiResponse, "AI", "智能客服"));

            if (shouldTransfer) {
                // 建议转人工
                sendToUser(sessionNo, createMessage("system",
                        "如果您的问题未能解决，可以回复'转人工'或点击下方按钮联系人工客服。", "system", "系统"));
            }
        } else if (status == 2) {
            // 人工接待中
            if ("agent".equals(userType)) {
                // 客服发送的消息
                log.info("客服发送消息，转发给用户");
                
                // 保存客服消息到数据库（sender_type=3）
                messageService.saveMessage(sessionNo, msgId, 1, content, (byte) 3, senderId, senderNickname);
                log.info("客服消息已保存到数据库: sender_type=3");
                
                // 转发给用户
                Map<String, Object> msg = createMessage("agent", content, senderId, senderNickname);
                msg.put("sessionNo", sessionNo);
                sendToUser(sessionNo, msg);
            } else {
                // 用户发送的消息
                log.info("用户发送消息，转发给客服");
                
                // 保存用户消息
                messageService.saveMessage(sessionNo, msgId, 1, content, (byte) 1, senderId, senderNickname);
                // 更新会话最后消息
                sessionService.updateLastMessage(sessionNo, content);
                
                // 转发给客服
                Map<String, Object> msg = createMessage("user", content, senderId, senderNickname);
                msg.put("sessionNo", sessionNo);
                sendToAllAgents(objectMapper.writeValueAsString(msg));

                // 更新未读数
                sessionService.incrementAgentUnreadCount(sessionNo);
            }
        }
        log.info("==============================================");
    }
    
    /**
     * 兼容转换：Integer、Long、String -> String
     */
    private String objToString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof Long) {
            return ((Long) obj).toString();
        } else if (obj instanceof Integer) {
            return ((Integer) obj).toString();
        } else {
            return obj.toString();
        }
    }

    /**
     * 处理转人工请求
     */
    private void handleTransferToHuman(WebSocketSession session, Map<String, Object> data) throws IOException {
        log.info("========== handleTransferToHuman 进入 ==========");
        log.info("收到data: {}", data);
        
        String sessionNo = (String) data.get("sessionNo");
        String reason = (String) data.get("reason");

        // 更新会话状态为等待转人工
        sessionService.updateSessionStatus(sessionNo, 1, reason);

        // 保存系统消息
        messageService.saveMessage(sessionNo, "msg_sys_" + System.currentTimeMillis(),
                1, "正在为您转接人工客服，请稍候...", (byte) 2, "system", "系统");

        // 通知客服有新会话需要接入
        String msg = "{\"action\":\"new_session\",\"sessionNo\":\"" + sessionNo + "\",\"reason\":\"" + reason + "\"}";
        sendToAllAgents(msg);

        // 发送消息给用户
        sendToUser(sessionNo, createMessage("system", "正在为您转接人工客服，请稍候...", "system", "系统"));
    }

    /**
     * 客服接受会话
     */
    private void handleAgentAccept(WebSocketSession session, Map<String, Object> data) throws IOException {
        log.info("========== handleAgentAccept 进入 ==========");
        log.info("收到data: {}", data);
        
        String sessionNo = (String) data.get("sessionNo");
        Long agentId = Long.parseLong((String) data.get("agentId"));
        String agentNickname = (String) data.get("agentNickname");

        // 更新会话状态
        sessionService.assignAgent(sessionNo, agentId, agentNickname);

        // 保存系统消息
        messageService.saveMessage(sessionNo, "msg_sys_" + System.currentTimeMillis(),
                1, "已为您分配客服[" + agentNickname + "]，客服正在为您服务。", (byte) 2, "system", "系统");

        // 发送消息给用户
        sendToUser(sessionNo, createMessage("system",
                "已为您分配客服[" + agentNickname + "]，客服正在为您服务。", "system", "系统"));
    }

    /**
     * 结束会话
     */
    private void handleEndSession(WebSocketSession session, Map<String, Object> data) throws IOException {
        String sessionNo = (String) data.get("sessionNo");
        Integer rating = data.get("rating") != null ? (Integer) data.get("rating") : null;

        // 结束会话
        sessionService.endSession(sessionNo, rating);

        // 保存系统消息
        messageService.saveMessage(sessionNo, "msg_sys_" + System.currentTimeMillis(),
                1, "会话已结束，感谢您的使用！如有问题欢迎随时咨询。", (byte) 2, "system", "系统");

        // 发送结束消息
        sendToUser(sessionNo, createMessage("system", "会话已结束，感谢您的使用！", "system", "系统"));
    }

    /**
     * 发送消息给用户
     */
    private void sendToUser(String sessionNo, Map<String, Object> message) throws IOException {
        log.info("========== sendToUser ==========");
        log.info("sessionNo={}", sessionNo);
        log.info("userSessions.size={}", userSessions.size());
        log.info("所有连接key={}", userSessions.keySet());
        
        WebSocketSession userSession = userSessions.get(sessionNo);
        
        log.info("查找到的session={}", userSession);
        
        if (userSession == null) {
            log.error("未找到用户连接 sessionNo={}", sessionNo);
            return;
        }
        
        log.info("session.isOpen={}", userSession.isOpen());
        
        if (userSession.isOpen()) {
            userSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            log.info("消息已发送给用户");
        } else {
            log.warn("用户连接已关闭");
        }
        log.info("=================================");
    }

    /**
     * 发送给所有客服
     */
    private void sendToAllAgents(String message) throws IOException {
        for (WebSocketSession agentSession : agentSessions.values()) {
            if (agentSession.isOpen()) {
                agentSession.sendMessage(new TextMessage(message));
            }
        }
    }
    
    /**
     * 通知用户会话已关闭（供Controller调用）
     * 发送 SESSION_CLOSED 事件给小程序
     */
    public void notifySessionClosed(String sessionNo) {
        log.info("========== notifySessionClosed ==========");
        log.info("sessionNo={}", sessionNo);
        
        WebSocketSession userSession = userSessions.get(sessionNo);
        
        if (userSession != null && userSession.isOpen()) {
            try {
                // 创建会话关闭消息
                Map<String, Object> closeMsg = new ConcurrentHashMap<>();
                closeMsg.put("event", "SESSION_CLOSED");
                closeMsg.put("sessionNo", sessionNo);
                closeMsg.put("content", "客服已结束会话，已为您切换到AI客服模式");
                closeMsg.put("timestamp", System.currentTimeMillis());
                
                String jsonMsg = objectMapper.writeValueAsString(closeMsg);
                userSession.sendMessage(new TextMessage(jsonMsg));
                log.info("会话关闭通知已发送: sessionNo={}", sessionNo);
            } catch (IOException e) {
                log.error("发送会话关闭通知失败: sessionNo={}", sessionNo, e);
            }
        } else {
            log.info("用户未在线或连接已关闭，跳过通知: sessionNo={}", sessionNo);
        }
        log.info("======================================");
    }

    /**
     * 创建消息map
     */
    private Map<String, Object> createMessage(String type, String content, String senderId, String senderNickname) {
        Map<String, Object> msg = new ConcurrentHashMap<>();
        msg.put("type", type);
        msg.put("content", content);
        msg.put("senderId", senderId);
        msg.put("senderNickname", senderNickname);
        msg.put("timestamp", System.currentTimeMillis());
        return msg;
    }

    /**
     * 从session获取参数
     */
    private String getParam(WebSocketSession session, String name) {
        String uri = session.getUri().toString();
        String query = "";
        if (uri.contains("?")) {
            query = uri.substring(uri.indexOf("?") + 1);
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && kv[0].equals(name)) {
                    return kv[1];
                }
            }
        }
        return null;
    }
}