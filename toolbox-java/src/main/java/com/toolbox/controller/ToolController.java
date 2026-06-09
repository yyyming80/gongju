package com.toolbox.controller;

import com.toolbox.service.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ToolController {
    
    @Autowired
    private ToolService toolService;
    
    /**
     * 获取所有启用的工具分类和工具列表
     */
    @GetMapping("/tools")
    public Map<String, Object> getTools() {
        return toolService.getEnabledTools();
    }
    
    /**
     * 获取所有分类
     */
    @GetMapping("/categories")
    public List<Map<String, Object>> getCategories() {
        return toolService.getAllCategories();
    }
    
    /**
     * 创建分类
     */
    @PostMapping("/category")
    public Map<String, Object> createCategory(@RequestBody Map<String, Object> data) {
        return toolService.createCategory(data);
    }
    
    /**
     * 更新分类
     */
    @PutMapping("/category/{id}")
    public void updateCategory(@PathVariable Integer id, @RequestBody Map<String, Object> data) {
        toolService.updateCategory(id, data);
    }
    
    /**
     * 删除分类
     */
    @DeleteMapping("/category/{id}")
    public void deleteCategory(@PathVariable Integer id) {
        toolService.deleteCategory(id);
    }
    
    /**
     * 获取所有工具
     */
    @GetMapping("/admin/tools")
    public Map<String, Object> getAllTools() {
        return toolService.getAllTools();
    }
    
    /**
     * 创建工具
     */
    @PostMapping("/admin/tool")
    public Map<String, Object> createTool(@RequestBody Map<String, Object> data) {
        return toolService.createTool(data);
    }
    
    /**
     * 更新工具
     */
    @PutMapping("/admin/tool/{id}")
    public void updateTool(@PathVariable Integer id, @RequestBody Map<String, Object> data) {
        toolService.updateTool(id, data);
    }
    
    /**
     * 删除工具
     */
    @DeleteMapping("/admin/tool/{id}")
    public void deleteTool(@PathVariable Integer id) {
        toolService.deleteTool(id);
    }
    
    /**
     * 批量更新工具排序
     */
    @PostMapping("/admin/tools/sort")
    public void updateToolsSort(@RequestBody List<Map<String, Object>> tools) {
        toolService.updateToolsSort(tools);
    }
    
    /**
     * 批量更新分类排序
     */
    @PostMapping("/admin/categories/sort")
    public void updateCategoriesSort(@RequestBody List<Map<String, Object>> categories) {
        toolService.updateCategoriesSort(categories);
    }
}
