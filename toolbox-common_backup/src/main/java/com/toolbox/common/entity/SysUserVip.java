package com.toolbox.common.entity;

import java.time.LocalDateTime;

public class SysUserVip extends BaseEntity {
    
    private Long id;
    private LocalDateTime createTime;
    private Integer deleted;
    private Long userId;
    private Integer vipLevel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getVipLevel() { return vipLevel; }
    public void setVipLevel(Integer vipLevel) { this.vipLevel = vipLevel; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
