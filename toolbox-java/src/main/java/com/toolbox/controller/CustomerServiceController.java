package com.toolbox.controller;

import com.toolbox.common.Result;
import com.toolbox.service.customer.CustomerAgentService;
import com.toolbox.service.customer.CustomerMessageService;
import com.toolbox.service.customer.CustomerSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 客服服务控制器
 */
@RestController
@RequestMapping("/api/customer")
@CrossOrigin(origins = "*")
public class CustomerServiceController {

    @Autowired
    private CustomerSessionService sessionService;

    @Autowired
    private CustomerMessageService messageService;

    @Autowired
    private CustomerAgentService agentService;

    /**
     * 客服登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        Map<String, Object> agent = agentService.login(username, password);
        if (agent == null) {
            return Result.error("用户名或密码错误");
        }
        return Result.success(agent);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result getSessions(@RequestParam(defaultValue = "-1") int status,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int pageSize) {
        List<Map<String, Object>> sessions = sessionService.getSessionList(status, page, pageSize);
        return Result.success(sessions);
    }

    /**
     * 获取等待中的会话
     */
    @GetMapping("/sessions/waiting")
    public Result getWaitingSessions() {
        List<Map<String, Object>> sessions = sessionService.getWaitingSessions();
        return Result.success(sessions);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/session/{sessionNo}")
    public Result getSessionDetail(@PathVariable String sessionNo) {
        Map<String, Object> session = sessionService.getSessionDetail(sessionNo);
        if (session == null) {
            return Result.error("会话不存在");
        }
        return Result.success(session);
    }

    /**
     * 获取会话消息
     */
    @GetMapping("/messages/{sessionNo}")
    public Result getMessages(@PathVariable String sessionNo,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "50") int pageSize) {
        List<Map<String, Object>> messages = messageService.getMessages(sessionNo, page, pageSize);
        return Result.success(messages);
    }

    /**
     * 创建会话（用户端）
     */
    @PostMapping("/session/start")
    public Result startSession(@RequestBody Map<String, String> params) {
        String userId = params.get("userId");
        String userNickname = params.get("userNickname");
        String userAvatar = params.get("userAvatar");

        // 检查是否有活跃会话
        Map<String, Object> activeSession = sessionService.getUserActiveSession(userId);
        if (activeSession != null) {
            return Result.success(activeSession);
        }

        // 创建新会话
        String sessionNo = "sess_" + System.currentTimeMillis();
        sessionService.createSession(sessionNo, userId, userNickname, userAvatar);

        Map<String, Object> session = sessionService.getSessionDetail(sessionNo);
        return Result.success(session);
    }

    /**
     * 结束会话
     */
    @PostMapping("/session/end")
    public Result endSession(@RequestBody Map<String, Object> params) {
        String sessionNo = (String) params.get("sessionNo");
        Integer rating = params.get("rating") != null ? (Integer) params.get("rating") : null;
        sessionService.endSession(sessionNo, rating);
        return Result.success();
    }

    /**
     * 客服接受会话
     */
    @PostMapping("/session/accept")
    public Result acceptSession(@RequestBody Map<String, Object> params) {
        String sessionNo = (String) params.get("sessionNo");
        Long agentId = Long.parseLong((String) params.get("agentId"));
        String agentNickname = (String) params.get("agentNickname");
        sessionService.assignAgent(sessionNo, agentId, agentNickname);
        return Result.success();
    }

    /**
     * 转人工
     */
    @PostMapping("/transfer")
    public Result transferToHuman(@RequestBody Map<String, String> params) {
        String sessionNo = params.get("sessionNo");
        String reason = params.get("reason");
        sessionService.updateSessionStatus(sessionNo, 1, reason);
        return Result.success();
    }

    /**
     * 获取客服列表
     */
    @GetMapping("/agents")
    public Result getAgents() {
        List<Map<String, Object>> agents = agentService.getAllAgents();
        return Result.success(agents);
    }

    /**
     * 获取在线客服
     */
    @GetMapping("/agents/online")
    public Result getOnlineAgents() {
        List<Map<String, Object>> agents = agentService.getOnlineAgents();
        return Result.success(agents);
    }

    /**
     * 更新客服状态
     */
    @PostMapping("/agent/status")
    public Result updateAgentStatus(@RequestBody Map<String, Object> params) {
        Long agentId = Long.parseLong((String) params.get("agentId"));
        int status = (int) params.get("status");
        agentService.updateStatus(agentId, status);
        return Result.success();
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public Result getStatistics() {
        Map<String, Object> stats = agentService.getStatistics();
        return Result.success(stats);
    }

    /**
     * 创建工单
     */
    @PostMapping("/work-order")
    public Result createWorkOrder(@RequestBody Map<String, Object> params) {
        String orderNo = "wo_" + System.currentTimeMillis();
        String sql = "INSERT INTO cs_work_order (order_no, session_id, user_id, agent_id, category, title, description, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, 0)";
        // 简化实现，实际需要更完善的参数处理
        return Result.success();
    }

    /**
     * 获取工单列表
     */
    @GetMapping("/work-orders")
    public Result getWorkOrders(@RequestParam(defaultValue = "-1") int status) {
        String sql = "SELECT * FROM cs_work_order";
        if (status >= 0) {
            sql += " WHERE status = " + status;
        }
        sql += " ORDER BY create_time DESC";
        return Result.success();
    }
}