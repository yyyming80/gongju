package com.toolbox.service.customer;

import com.toolbox.common.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 客服会话服务
 */
@Service
public class CustomerSessionService {

    private static final Logger log = LoggerFactory.getLogger(CustomerSessionService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 创建新会话
     */
    public void createSession(String sessionNo, String userId, String userNickname, String userAvatar) {
        String sql = "INSERT INTO cs_session (session_no, user_id, user_nickname, user_avatar, status, start_time) " +
                     "VALUES (?, ?, ?, ?, 0, NOW())";
        jdbcTemplate.update(sql, sessionNo, userId, userNickname, userAvatar);
        log.info("创建会话: sessionNo={}, userId={}", sessionNo, userId);
    }

    /**
     * 更新最后消息
     */
    public void updateLastMessage(String sessionNo, String lastMessage) {
        String sql = "UPDATE cs_session SET last_message = ?, last_message_time = NOW() WHERE session_no = ?";
        jdbcTemplate.update(sql, lastMessage, sessionNo);
    }

    /**
     * 更新会话状态
     */
    public void updateSessionStatus(String sessionNo, int status, String reason) {
        String sql = "UPDATE cs_session SET status = ?, transfer_reason = ?, transfer_time = NOW() WHERE session_no = ?";
        jdbcTemplate.update(sql, status, reason, sessionNo);
        log.info("会话状态更新: sessionNo={}, status={}", sessionNo, status);
    }

    /**
     * 分配客服
     */
    public void assignAgent(String sessionNo, Long agentId, String agentNickname) {
        String sql = "UPDATE cs_session SET agent_id = ?, agent_nickname = ?, status = 2 WHERE session_no = ?";
        jdbcTemplate.update(sql, agentId, agentNickname, sessionNo);

        // 更新客服当前会话数
        String updateAgent = "UPDATE cs_agent SET current_sessions = current_sessions + 1 WHERE id = ?";
        jdbcTemplate.update(updateAgent, agentId);

        log.info("分配客服: sessionNo={}, agentId={}", sessionNo, agentId);
    }

    /**
     * 结束会话
     */
    public void endSession(String sessionNo, Integer rating) {
        String sql = "UPDATE cs_session SET status = 3, end_time = NOW(), " +
                     "duration = TIMESTAMPDIFF(SECOND, start_time, NOW())";
        if (rating != null) {
            sql += ", rating = ?, rating_time = NOW()";
            jdbcTemplate.update(sql, sessionNo, rating);
        } else {
            jdbcTemplate.update(sql, sessionNo);
        }

        // 更新客服会话数
        String getAgent = "SELECT agent_id FROM cs_session WHERE session_no = ?";
        Long agentId = jdbcTemplate.queryForObject(getAgent, Long.class, sessionNo);
        if (agentId != null) {
            String updateAgent = "UPDATE cs_agent SET current_sessions = current_sessions - 1 WHERE id = ?";
            jdbcTemplate.update(updateAgent, agentId);
        }

        log.info("会话结束: sessionNo={}, rating={}", sessionNo, rating);
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
        String sql = "SELECT * FROM cs_session";
        if (status >= 0) {
            sql += " WHERE status = " + status;
        }
        sql += " ORDER BY last_message_time DESC LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize;
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 获取等待中的会话
     */
    public List<Map<String, Object>> getWaitingSessions() {
        String sql = "SELECT * FROM cs_session WHERE status = 1 ORDER BY transfer_time ASC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 增加客服未读消息数
     */
    public void incrementAgentUnreadCount(String sessionNo) {
        String sql = "UPDATE cs_session SET agent_unread_count = agent_unread_count + 1 WHERE session_no = ?";
        jdbcTemplate.update(sql, sessionNo);
    }

    /**
     * 重置未读消息数
     */
    public void resetUnreadCount(String sessionNo, String userType) {
        String sql;
        if ("agent".equals(userType)) {
            sql = "UPDATE cs_session SET agent_unread_count = 0 WHERE session_no = ?";
        } else {
            sql = "UPDATE cs_session SET unread_count = 0 WHERE session_no = ?";
        }
        jdbcTemplate.update(sql, sessionNo);
    }

    /**
     * 获取用户的活跃会话
     */
    public Map<String, Object> getUserActiveSession(String userId) {
        String sql = "SELECT * FROM cs_session WHERE user_id = ? AND status < 3 ORDER BY create_time DESC LIMIT 1";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 根据会话ID获取会话
     */
    public Map<String, Object> getSessionById(Long sessionId) {
        String sql = "SELECT * FROM cs_session WHERE id = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, sessionId);
        return results.isEmpty() ? null : results.get(0);
    }
}