package com.toolbox.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一返回结果
 */
public class Result<T> {
    
    // ==================== 状态码常量 ====================
    public static final int SUCCESS = 200;
    public static final int ERROR = 500;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SERVER_ERROR = 500;
    
    private int code;
    private String message;
    private T data;
    private long timestamp;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 成功响应 ====================
    public static <T> Result<T> success() {
        return new Result<>(SUCCESS, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS, message, data);
    }

    // ==================== 分页响应 ====================
    public static <T> Result<PageData<T>> successPage(List<T> list, long total, int pageNum, int pageSize) {
        PageData<T> pageData = new PageData<>(list, total, pageNum, pageSize);
        Result<PageData<T>> result = new Result<>(SUCCESS, "查询成功", pageData);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    // ==================== 错误响应 ====================
    public static <T> Result<T> error(String message) {
        return new Result<>(ERROR, message, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(BAD_REQUEST, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(UNAUTHORIZED, message != null ? message : "未授权", null);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(FORBIDDEN, message != null ? message : "禁止访问", null);
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(NOT_FOUND, message, null);
    }

    public static <T> Result<T> serverError(String message) {
        return new Result<>(SERVER_ERROR, message, null);
    }

    // ==================== Map转换 ====================
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("message", message);
        map.put("timestamp", timestamp);
        if (data != null) {
            map.put("data", data);
        }
        if (total != null) {
            map.put("total", total);
        }
        return map;
    }

    // ==================== Getter/Setter ====================
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    // ==================== 分页数据类 ====================
    public static class PageData<T> {
        private List<T> list;
        private long total;
        private int pageNum;
        private int pageSize;
        private int pages;

        public PageData() {}

        public PageData(List<T> list, long total, int pageNum, int pageSize) {
            this.list = list;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.pages = (int) Math.ceil((double) total / pageSize);
        }

        public List<T> getList() { return list; }
        public void setList(List<T> list) { this.list = list; }
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public int getPageNum() { return pageNum; }
        public void setPageNum(int pageNum) { this.pageNum = pageNum; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public int getPages() { return pages; }
        public void setPages(int pages) { this.pages = pages; }
    }
}
