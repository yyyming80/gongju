package com.toolbox.common.entity;

import java.time.LocalDateTime;

public class SysAdvertisement extends BaseEntity {
    
    private String adKey;
    private String adName;
    private String adType;
    private String adContent;
    private String position;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer sortOrder;
    private Integer status;
    
    public String getAdKey() { return adKey; }
    public void setAdKey(String adKey) { this.adKey = adKey; }
    public String getAdName() { return adName; }
    public void setAdName(String adName) { this.adName = adName; }
    public String getAdType() { return adType; }
    public void setAdType(String adType) { this.adType = adType; }
    public String getAdContent() { return adContent; }
    public void setAdContent(String adContent) { this.adContent = adContent; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
