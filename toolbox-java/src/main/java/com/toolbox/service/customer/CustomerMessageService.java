package com.toolbox.service.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 客服消息服务
 */
@Service
public class CustomerMessageService {

    private static final Logger log = LoggerFactory.getLogger(CustomerMessageService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 保存消息
     */
    public void saveMessage(String sessionNo, String msgId, int msgType, String content,
                          byte senderType, String senderId, String senderNickname) {
        String sql = "INSERT INTO cs_message (session_id, session_no, msg_id, msg_type, content, " +
                     "sender_type, sender_id, sender_nickname, create_time) " +
                     "SELECT ?, ?, ?, ?, ?, ?, ?, ?, NOW() FROM cs_session WHERE session_no = ?";
        jdbcTemplate.update(sql, getSessionId(sessionNo), sessionNo, msgId, msgType, content,
                senderType, senderId, senderNickname, sessionNo);
        log.debug("保存消息: sessionNo={}, msgId={}", sessionNo, msgId);
    }

    /**
     * 获取会话消息列表
     */
    public List<Map<String, Object>> getMessages(String sessionNo) {
        String sql = "SELECT * FROM cs_message WHERE session_no = ? ORDER BY create_time ASC";
        return jdbcTemplate.queryForList(sql, sessionNo);
    }

    /**
     * 获取会话消息列表（分页）
     */
    public List<Map<String, Object>> getMessages(String sessionNo, int page, int pageSize) {
        String sql = "SELECT * FROM cs_message WHERE session_no = ? " +
                     "ORDER BY create_time DESC LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize;
        return jdbcTemplate.queryForList(sql, sessionNo);
    }

    /**
     * 标记消息已读
     */
    public void markAsRead(String msgId) {
        String sql = "UPDATE cs_message SET is_read = 1, read_time = NOW() WHERE msg_id = ?";
        jdbcTemplate.update(sql, msgId);
    }

    /**
     * 获取会话ID
     */
    private Long getSessionId(String sessionNo) {
        String sql = "SELECT id FROM cs_session WHERE session_no = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, sessionNo);
    }

    /**
     * 删除会话消息
     */
    public void deleteSessionMessages(String sessionNo) {
        String sql = "DELETE FROM cs_message WHERE session_no = ?";
        jdbcTemplate.update(sql, sessionNo);
    }
}