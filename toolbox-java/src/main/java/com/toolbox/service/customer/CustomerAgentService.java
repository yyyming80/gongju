package com.toolbox.service.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * 客服账号服务
 */
@Service
public class CustomerAgentService {

    private static final Logger log = LoggerFactory.getLogger(CustomerAgentService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 客服登录
     */
    public Map<String, Object> login(String username, String password) {
        // 直接使用明文密码比对（数据库中存储明文）
        String sql = "SELECT * FROM cs_agent WHERE username = ? AND password = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, username, password);
        if (results.isEmpty()) {
            log.warn("登录失败: username={}, password={}", username, password);
            return null;
        }

        Map<String, Object> agent = results.get(0);
        // 更新最后登录时间
        String updateSql = "UPDATE cs_agent SET last_login_time = NOW() WHERE id = ?";
        jdbcTemplate.update(updateSql, agent.get("id"));
        return agent;
    }

    /**
     * 根据ID获取客服
     */
    public Map<String, Object> getAgentById(Long agentId) {
        String sql = "SELECT * FROM cs_agent WHERE id = ?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, agentId);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 获取在线客服列表
     */
    public List<Map<String, Object>> getOnlineAgents() {
        String sql = "SELECT * FROM cs_agent WHERE status = 1 AND current_sessions < max_sessions ORDER BY current_sessions ASC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 更新客服状态
     */
    public void updateStatus(Long agentId, int status) {
        String sql = "UPDATE cs_agent SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, agentId);
        log.info("客服状态更新: agentId={}, status={}", agentId, status);
    }

    /**
     * 获取所有客服
     */
    public List<Map<String, Object>> getAllAgents() {
        String sql = "SELECT id, username, nickname, role, status, current_sessions, max_sessions, total_served, rating, last_login_time FROM cs_agent ORDER BY role DESC, id ASC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 添加客服
     */
    public void addAgent(String username, String password, String nickname, int role) {
        String sql = "INSERT INTO cs_agent (username, password, nickname, role, status, max_sessions) VALUES (?, ?, ?, ?, 0, 50)";
        jdbcTemplate.update(sql, username, password, nickname, role);
        log.info("添加客服: username={}", username);
    }

    /**
     * 更新客服
     */
    public void updateAgent(Long agentId, String nickname, int role) {
        String sql = "UPDATE cs_agent SET nickname = ?, role = ? WHERE id = ?";
        jdbcTemplate.update(sql, nickname, role, agentId);
        log.info("更新客服: agentId={}", agentId);
    }

    /**
     * 删除客服
     */
    public void deleteAgent(Long agentId) {
        String sql = "DELETE FROM cs_agent WHERE id = ? AND role != 2";
        jdbcTemplate.update(sql, agentId);
        log.info("删除客服: agentId={}", agentId);
    }

    /**
     * 获取统计数据
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();

        // 总会话数
        String totalSql = "SELECT COUNT(*) FROM cs_session";
        stats.put("totalSessions", jdbcTemplate.queryForObject(totalSql, Long.class));

        // AI接待会话数
        String aiSql = "SELECT COUNT(*) FROM cs_session WHERE status = 0";
        stats.put("aiSessions", jdbcTemplate.queryForObject(aiSql, Long.class));

        // 人工接待会话数
        String humanSql = "SELECT COUNT(*) FROM cs_session WHERE status = 2";
        stats.put("humanSessions", jdbcTemplate.queryForObject(humanSql, Long.class));

        // 今日会话数
        String todaySql = "SELECT COUNT(*) FROM cs_session WHERE DATE(create_time) = CURDATE()";
        stats.put("todaySessions", jdbcTemplate.queryForObject(todaySql, Long.class));

        // 等待中的会话数
        String waitingSql = "SELECT COUNT(*) FROM cs_session WHERE status = 1";
        stats.put("waitingSessions", jdbcTemplate.queryForObject(waitingSql, Long.class));

        // 在线客服数
        String onlineSql = "SELECT COUNT(*) FROM cs_agent WHERE status = 1";
        stats.put("onlineAgents", jdbcTemplate.queryForObject(onlineSql, Long.class));

        return stats;
    }
}