package com.toolbox.common.utils;

import java.util.HashMap;
import java.util.Map;

public class R extends HashMap<String, Object> {
    
    public static Map<String, Object> ok() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        return result;
    }
    
    public static Map<String, Object> fail(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", message);
        return result;
    }
}
