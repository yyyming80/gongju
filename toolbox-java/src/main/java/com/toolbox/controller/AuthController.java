package com.toolbox.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolbox.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 微信登录控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${wechat.appid:wx_your_appid}")
    private String appid;

    @Value("${wechat.secret:your_app_secret}")
    private String appsecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 微信登录
     * POST /api/auth/wx-login
     */
    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody Map<String, Object> request) {
        log.info("========== 微信登录请求 ==========");
        log.info("请求参数: {}", request);

        try {
            String code = (String) request.get("code");

            if (code == null || code.isEmpty()) {
                return Result.error("登录凭证不能为空");
            }

            // 1. 调用微信接口获取 openid
            Map<String, Object> wxResult = getWxSession(code);

            if (wxResult == null) {
                return Result.error("微信登录失败，请检查App配置");
            }

            String openid = (String) wxResult.get("openid");
            String sessionKey = (String) wxResult.get("session_key");

            if (openid == null || openid.isEmpty()) {
                log.error("获取openid失败: {}", wxResult);
                return Result.error("微信登录失败：无法获取用户标识");
            }

            log.info("微信返回: openid={}, session_key={}", openid, sessionKey != null ? "***" : "null");

            // 2. 查询或创建用户
            Map<String, Object> user = findOrCreateUser(openid, request);

            // 3. 生成 token
            String token = generateToken(openid);

            // 4. 保存会话
            saveSession(openid, sessionKey, token);

            // 5. 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userInfo", user);

            log.info("登录成功: userId={}, openid={}", user.get("id"), openid);
            log.info("========================================");

            return Result.success("登录成功", result);

        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户信息
     * GET /api/auth/user-info
     */
    @GetMapping("/user-info")
    public Result<Map<String, Object>> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String auth) {

        log.info("========== 获取用户信息 ==========");
        log.info("Token: {}", auth);

        if (auth == null || auth.isEmpty()) {
            return Result.error("未登录");
        }

        // 移除 Bearer 前缀
        String token = auth.replace("Bearer ", "").trim();

        try {
            // 根据token查询用户
            String sql = "SELECT u.* FROM wx_user u " +
                        "INNER JOIN wx_session s ON u.openid = s.openid " +
                        "WHERE s.session_token = ? AND s.token_expire_time > NOW()";

            List<Map<String, Object>> users = jdbc.queryForList(sql, token);

            if (users.isEmpty()) {
                return Result.error("登录已过期，请重新登录");
            }

            Map<String, Object> user = users.get(0);
            // 移除敏感字段
            user.remove("session_key");

            return Result.success("获取成功", formatUserInfo(user));

        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return Result.error("获取失败");
        }
    }

    /**
     * 更新用户信息
     * POST /api/auth/update-user
     */
    @PostMapping("/update-user")
    public Result<Void> updateUser(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> userInfo) {

        log.info("========== 更新用户信息 ==========");

        if (auth == null || auth.isEmpty()) {
            return Result.error("未登录");
        }

        String token = auth.replace("Bearer ", "").trim();

        try {
            String sql = "SELECT openid FROM wx_session WHERE session_token = ? AND token_expire_time > NOW()";
            List<Map<String, Object>> sessions = jdbc.queryForList(sql, token);

            if (sessions.isEmpty()) {
                return Result.error("登录已过期");
            }

            String openid = (String) sessions.get(0).get("openid");

            // 更新用户信息
            StringBuilder updateSql = new StringBuilder("UPDATE wx_user SET ");
            List<Object> params = new ArrayList<>();

            if (userInfo.containsKey("nickname")) {
                updateSql.append("nickname = ?, ");
                params.add(userInfo.get("nickname"));
            }
            if (userInfo.containsKey("avatar")) {
                updateSql.append("avatar = ?, ");
                params.add(userInfo.get("avatar"));
            }
            if (userInfo.containsKey("phone")) {
                updateSql.append("phone = ?, ");
                params.add(userInfo.get("phone"));
            }

            if (params.isEmpty()) {
                return Result.success("无需更新", null);
            }

            // 移除末尾的逗号
            String updateQuery = updateSql.toString().replaceAll(", $", "") + " WHERE openid = ?";
            params.add(openid);

            jdbc.update(updateQuery, params.toArray());

            return Result.success("更新成功", null);

        } catch (Exception e) {
            log.error("更新失败: {}", e.getMessage());
            return Result.error("更新失败");
        }
    }

    /**
     * 退出登录
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String auth) {

        log.info("========== 退出登录 ==========");

        if (auth != null && !auth.isEmpty()) {
            String token = auth.replace("Bearer ", "").trim();
            try {
                jdbc.update("DELETE FROM wx_session WHERE session_token = ?", token);
            } catch (Exception e) {
                log.warn("删除会话失败: {}", e.getMessage());
            }
        }

        return Result.success("退出成功", null);
    }

    // ==================== 私有方法 ====================

    /**
     * 调用微信接口获取 openid
     */
    private Map<String, Object> getWxSession(String code) {
        try {
            String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, appsecret, code
            );

            String response = restTemplate.getForObject(url, String.class);
            log.info("微信API响应: {}", response);

            return objectMapper.readValue(response, Map.class);

        } catch (Exception e) {
            log.error("调用微信接口失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 查询或创建用户
     */
    private Map<String, Object> findOrCreateUser(String openid, Map<String, Object> request) {
        String sql = "SELECT * FROM wx_user WHERE openid = ?";
        List<Map<String, Object>> users = jdbc.queryForList(sql, openid);

        if (users.isEmpty()) {
            // 创建新用户
            return createUser(openid, request);
        } else {
            // 更新用户信息
            return updateUserInfo(users.get(0), request);
        }
    }

    /**
     * 创建新用户
     */
    private Map<String, Object> createUser(String openid, Map<String, Object> request) {
        String nickname = getStringValue(request, "nickname", "微信用户");
        String avatar = getStringValue(request, "avatar", "");

        String sql = "INSERT INTO wx_user (openid, nickname, avatar, status, create_time) VALUES (?, ?, ?, 1, NOW())";

        jdbc.update(sql, openid, nickname, avatar);

        // 返回新创建的用户
        String selectSql = "SELECT * FROM wx_user WHERE openid = ?";
        List<Map<String, Object>> users = jdbc.queryForList(selectSql, openid);

        return formatUserInfo(users.get(0));
    }

    /**
     * 更新用户信息
     */
    private Map<String, Object> updateUserInfo(Map<String, Object> dbUser, Map<String, Object> request) {
        String openid = (String) dbUser.get("openid");

        // 只有新数据才更新
        String nickname = getStringValue(request, "nickname", null);
        String avatar = getStringValue(request, "avatar", null);

        if (nickname != null && !nickname.equals(String.valueOf(dbUser.get("nickname")))) {
            jdbc.update("UPDATE wx_user SET nickname = ? WHERE openid = ?", nickname, openid);
            dbUser.put("nickname", nickname);
        }

        if (avatar != null && !avatar.equals(String.valueOf(dbUser.get("avatar")))) {
            jdbc.update("UPDATE wx_user SET avatar = ? WHERE openid = ?", avatar, openid);
            dbUser.put("avatar", avatar);
        }

        return formatUserInfo(dbUser);
    }

    /**
     * 保存会话
     */
    private void saveSession(String openid, String sessionKey, String token) {
        // 删除旧会话
        jdbc.update("DELETE FROM wx_session WHERE openid = ?", openid);

        // 计算过期时间（30天）
        LocalDateTime expireTime = LocalDateTime.now().plusDays(30);
        String expireStr = expireTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 插入新会话
        String sql = "INSERT INTO wx_session (openid, session_key, session_token, token_expire_time, create_time) VALUES (?, ?, ?, ?, NOW())";
        jdbc.update(sql, openid, sessionKey, token, expireStr);
    }

    /**
     * 生成 Token
     */
    private String generateToken(String openid) {
        return "tk_" + UUID.randomUUID().toString().replace("-", "") + "_" + System.currentTimeMillis();
    }

    /**
     * 格式化用户信息（移除敏感字段）
     */
    private Map<String, Object> formatUserInfo(Map<String, Object> user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.get("id"));
        info.put("openid", user.get("openid"));
        info.put("nickname", user.get("nickname"));
        info.put("avatar", user.get("avatar"));
        info.put("phone", user.get("phone"));
        info.put("gender", user.get("gender"));
        boolean isVip = "1".equals(String.valueOf(user.get("is_vip"))) || (user.get("is_vip") != null && (Integer) user.get("is_vip") == 1);
        info.put("isVip", isVip);
        return info;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null && !value.toString().isEmpty() ? value.toString() : defaultValue;
    }
}
