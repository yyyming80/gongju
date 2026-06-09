package com.toolbox.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ToolService {
    
    @Autowired
    private JdbcTemplate jdbc;
    
    /**
     * 获取所有启用的工具（分类+工具）
     */
    public Map<String, Object> getEnabledTools() {
        // 查询启用的分类
        List<Map<String, Object>> categories = jdbc.queryForList(
            "SELECT * FROM tool_category WHERE status=1 ORDER BY sort_order"
        );
        
        // 查询启用的工具
        List<Map<String, Object>> tools = jdbc.queryForList(
            "SELECT * FROM tool_config WHERE status=1 ORDER BY sort_order"
        );
        
        // 按分类分组工具
        Map<Integer, List<Map<String, Object>>> toolsByCategory = new HashMap<>();
        for (Map<String, Object> tool : tools) {
            Integer categoryId = (Integer) tool.get("category_id");
            toolsByCategory.computeIfAbsent(categoryId, k -> new ArrayList<>()).add(tool);
        }
        
        // 组装分类和工具
        for (Map<String, Object> category : categories) {
            Integer catId = (Integer) category.get("id");
            category.put("tools", toolsByCategory.getOrDefault(catId, new ArrayList<>()));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("categories", categories);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 获取所有分类
     */
    public List<Map<String, Object>> getAllCategories() {
        return jdbc.queryForList("SELECT * FROM tool_category ORDER BY sort_order");
    }
    
    /**
     * 创建分类
     */
    public Map<String, Object> createCategory(Map<String, Object> data) {
        String name = (String) data.get("name");
        String icon = (String) data.get("icon");
        Integer sort = data.get("sort_order") != null ? (Integer) data.get("sort_order") : 0;
        
        jdbc.update(
            "INSERT INTO tool_category(name,icon,sort_order,status) VALUES(?,?,?,1)",
            name, icon, sort
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
    
    /**
     * 更新分类
     */
    public void updateCategory(Integer id, Map<String, Object> data) {
        String name = (String) data.get("name");
        String icon = (String) data.get("icon");
        Integer sort = (Integer) data.get("sort_order");
        Integer status = (Integer) data.get("status");
        
        jdbc.update(
            "UPDATE tool_category SET name=?,icon=?,sort_order=?,status=? WHERE id=?",
            name, icon, sort, status, id
        );
    }
    
    /**
     * 删除分类
     */
    public void deleteCategory(Integer id) {
        jdbc.update("DELETE FROM tool_category WHERE id=?", id);
    }
    
    /**
     * 获取所有工具
     */
    public Map<String, Object> getAllTools() {
        List<Map<String, Object>> categories = jdbc.queryForList(
            "SELECT * FROM tool_category ORDER BY sort_order"
        );
        List<Map<String, Object>> tools = jdbc.queryForList(
            "SELECT * FROM tool_config ORDER BY sort_order"
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("categories", categories);
        result.put("tools", tools);
        return result;
    }
    
    /**
     * 创建工具
     */
    public Map<String, Object> createTool(Map<String, Object> data) {
        String toolKey = (String) data.get("tool_key");
        String name = (String) data.get("name");
        String icon = (String) data.get("icon");
        String desc = (String) data.get("description");
        Integer categoryId = data.get("category_id") != null ? ((Number) data.get("category_id")).intValue() : null;
        String pagePath = (String) data.get("page_path");
        Integer sortOrder = data.get("sort_order") != null ? ((Number) data.get("sort_order")).intValue() : 0;
        String configJson = data.get("config") != null ? data.get("config").toString() : null;
        
        jdbc.update(
            "INSERT INTO tool_config(tool_key,name,icon,description,category_id,page_path,sort_order,status,config) VALUES(?,?,?,?,?,?,?,1,?)",
            toolKey, name, icon, desc, categoryId, pagePath, sortOrder, configJson
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
    
    /**
     * 更新工具
     */
    public void updateTool(Integer id, Map<String, Object> data) {
        String name = (String) data.get("name");
        String icon = (String) data.get("icon");
        String desc = (String) data.get("description");
        Integer categoryId = data.get("category_id") != null ? ((Number) data.get("category_id")).intValue() : null;
        String pagePath = (String) data.get("page_path");
        Integer sortOrder = data.get("sort_order") != null ? ((Number) data.get("sort_order")).intValue() : 0;
        Integer status = data.get("status") != null ? ((Number) data.get("status")).intValue() : 1;
        
        jdbc.update(
            "UPDATE tool_config SET name=?,icon=?,description=?,category_id=?,page_path=?,sort_order=?,status=? WHERE id=?",
            name, icon, desc, categoryId, pagePath, sortOrder, status, id
        );
    }
    
    /**
     * 删除工具
     */
    public void deleteTool(Integer id) {
        jdbc.update("DELETE FROM tool_config WHERE id=?", id);
    }
    
    /**
     * 批量更新工具排序
     */
    public void updateToolsSort(List<Map<String, Object>> tools) {
        for (Map<String, Object> tool : tools) {
            Integer id = ((Number) tool.get("id")).intValue();
            Integer sortOrder = ((Number) tool.get("sort_order")).intValue();
            jdbc.update(
                "UPDATE tool_config SET sort_order=? WHERE id=?",
                sortOrder, id
            );
        }
    }
    
    /**
     * 批量更新分类排序
     */
    public void updateCategoriesSort(List<Map<String, Object>> categories) {
        for (Map<String, Object> category : categories) {
            Integer id = ((Number) category.get("id")).intValue();
            Integer sortOrder = ((Number) category.get("sort_order")).intValue();
            jdbc.update(
                "UPDATE tool_category SET sort_order=? WHERE id=?",
                sortOrder, id
            );
        }
    }
}
