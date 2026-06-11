package com.toolbox.service.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 客服会话服务
 */
@Service
public class CustomerSessionService {

    private static final Logger log = LoggerFactory.getLogger(CustomerSessionService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 保存系统消息
     */
    public void saveSystemMessage(String sessionNo, String msgId, String content) {
        // 先获取session_id
        Long sessionId = getSessionId(sessionNo);
        if (sessionId == null) {
            log.error("会话不存在: sessionNo={}", sessionNo);
            return;
        }
        
        String sql = "INSERT INTO cs_message (session_id, session_no, msg_id, msg_type, content, " +
                     "sender_type, sender_id, sender_nickname, create_time) " +
                     "VALUES (?, ?, ?, 1, ?, 4, 'system', '系统助手', NOW())";
        jdbcTemplate.update(sql, sessionId, sessionNo, msgId, content);
        log.info("保存系统消息成功: sessionNo={}, msgId={}, content={}", sessionNo, msgId, content);
    }
    
    /**
     * 保存用户消息
     */
    public void saveMessage(String sessionNo, String msgId, int msgType, String content,
                          byte senderType, String senderId, String senderNickname) {
        Long sessionId = getSessionId(sessionNo);
        if (sessionId == null) {
            log.error("会话不存在: sessionNo={}", sessionNo);
            return;
        }
        
        String sql = "INSERT INTO cs_message (session_id, session_no, msg_id, msg_type, content, " +
                     "sender_type, sender_id, sender_nickname, create_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, sessionId, sessionNo, msgId, msgType, content, senderType, senderId, senderNickname);
        log.info("保存消息成功: sessionNo={}, senderType={}", sessionNo, senderType);
    }
    
    /**
     * 创建新会话（幂等实现）
     * 使用 INSERT IGNORE 保证幂等性，相同 session_no 不会重复插入
     */
    public void createSession(String sessionNo, String userId, String userNickname, String userAvatar) {
        String sql = "INSERT IGNORE INTO cs_session (session_no, user_id, user_nickname, user_avatar, status, start_time) " +
                     "VALUES (?, ?, ?, ?, 0, NOW())";
        int rows = jdbcTemplate.update(sql, sessionNo, userId, userNickname, userAvatar);
        if (rows > 0) {
            log.info("创建会话成功: sessionNo={}, userId={}", sessionNo, userId);
        } else {
            log.info("会话已存在，跳过创建: sessionNo={}", sessionNo);
        }
    }
    
    /**
     * 创建或获取会话（推荐使用）
     * 返回 true 表示新建会话，false 表示会话已存在
     */
    public boolean createOrGetSession(String sessionNo, String userId, String userNickname, String userAvatar) {
        // 先查询是否存在
        Long existingId = getSessionId(sessionNo);
        if (existingId != null) {
            log.info("会话已存在: sessionNo={}", sessionNo);
            return false;
        }
        
        // 不存在则创建（INSERT IGNORE 保证幂等）
        String sql = "INSERT IGNORE INTO cs_session (session_no, user_id, user_nickname, user_avatar, status, start_time) " +
                     "VALUES (?, ?, ?, ?, 0, NOW())";
        int rows = jdbcTemplate.update(sql, sessionNo, userId, userNickname, userAvatar);
        if (rows > 0) {
            log.info("创建会话成功: sessionNo={}, userId={}", sessionNo, userId);
            return true;
        } else {
            log.info("会话已存在（并发）: sessionNo={}", sessionNo);
            return false;
        }
    }

    /**
     * 更新会话状态
     */
    public void updateSessionStatus(String sessionNo, int status, String reason) {
        String sql = "UPDATE cs_session SET status = ?, transfer_reason = ?, transfer_time = NOW() WHERE session_no = ?";
        jdbcTemplate.update(sql, status, reason, sessionNo);
        log.info("更新会话状态: sessionNo={}, status={}", sessionNo, status);
    }
    
    /**
     * 分配客服
     */
    public void assignAgent(String sessionNo, Long agentId, String agentNickname) {
        String sql = "UPDATE cs_session SET agent_id = ?, agent_nickname = ?, status = 2 WHERE session_no = ?";
        jdbcTemplate.update(sql, agentId, agentNickname, sessionNo);
        log.info("分配客服: sessionNo={}, agentId={}", sessionNo, agentId);
    }
    
    /**
     * 结束会话
     */
    public void endSession(String sessionNo, Integer rating) {
        String sql;
        if (rating != null) {
            sql = "UPDATE cs_session SET status = 3, rating = ?, rating_time = NOW(), " +
                  "end_time = NOW(), duration = TIMESTAMPDIFF(SECOND, start_time, NOW()) " +
                  "WHERE session_no = ?";
            jdbcTemplate.update(sql, rating, sessionNo);
        } else {
            sql = "UPDATE cs_session SET status = 3, " +
                  "end_time = NOW(), duration = TIMESTAMPDIFF(SECOND, start_time, NOW()) " +
                  "WHERE session_no = ?";
            jdbcTemplate.update(sql, sessionNo);
        }
        log.info("结束会话: sessionNo={}", sessionNo);
    }
    
    /**
     * 获取会话详情
     */
    public Map<String, Object> getSessionDetail(String sessionNo) {
        String sql = "SELECT * FROM cs_session WHERE session_no = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, sessionNo);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * 获取会话列表
     */
    public List<Map<String, Object>> getSessionList(int status, int page, int pageSize) {
        String sql;
        Object[] params;
        int offset = (page - 1) * pageSize;
        
        if (status >= 0) {
            sql = "SELECT * FROM cs_session WHERE status = ? ORDER BY COALESCE(last_message_time, create_time) DESC LIMIT ? OFFSET ?";
            params = new Object[]{status, pageSize, offset};
        } else {
            sql = "SELECT * FROM cs_session ORDER BY COALESCE(last_message_time, create_time) DESC LIMIT ? OFFSET ?";
            params = new Object[]{pageSize, offset};
        }
        
        log.info("查询会话列表: status={}, page={}, pageSize={}", status, page, pageSize);
        return jdbcTemplate.queryForList(sql, params);
    }
    
    /**
     * 获取等待中的会话
     */
    public List<Map<String, Object>> getWaitingSessions() {
        String sql = "SELECT * FROM cs_session WHERE status = 1 ORDER BY transfer_time ASC";
        return jdbcTemplate.queryForList(sql);
    }
    
    /**
     * 获取sessionId
     */
    private Long getSessionId(String sessionNo) {
        String sql = "SELECT id FROM cs_session WHERE session_no = ?";
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, sessionNo);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 更新最后消息
     */
    public void updateLastMessage(String sessionNo, String lastMessage) {
        String sql = "UPDATE cs_session SET last_message = ?, last_message_time = NOW() WHERE session_no = ?";
        jdbcTemplate.update(sql, lastMessage, sessionNo);
    }
    
    /**
     * 获取会话状态
     */
    public int getSessionStatus(String sessionNo) {
        String sql = "SELECT status FROM cs_session WHERE session_no = ?";
        Integer status = jdbcTemplate.queryForObject(sql, Integer.class, sessionNo);
        return status != null ? status : 0;
    }
    
    /**
     * 增加客服未读消息数
     */
    public void incrementAgentUnreadCount(String sessionNo) {
        String sql = "UPDATE cs_session SET agent_unread_count = agent_unread_count + 1 WHERE session_no = ?";
        jdbcTemplate.update(sql, sessionNo);
    }
    
    /**
     * 获取用户活跃会话
     */
    public Map<String, Object> getUserActiveSession(String userId) {
        String sql = "SELECT * FROM cs_session WHERE user_id = ? AND status < 3 ORDER BY create_time DESC LIMIT 1";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
        return results.isEmpty() ? null : results.get(0);
    }
}
