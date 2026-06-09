package com.toolbox.common.entity;

public class ResumeRecord extends BaseEntity {
    
    private String recordNo;
    private Long userId;
    private Long templateId;
    private String resumeData;
    private String outputFile;
    private String exportFormat;
    private Integer aiOptimized;
    private String aiFeedback;
    
    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getResumeData() { return resumeData; }
    public void setResumeData(String resumeData) { this.resumeData = resumeData; }
    public String getOutputFile() { return outputFile; }
    public void setOutputFile(String outputFile) { this.outputFile = outputFile; }
    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
    public Integer getAiOptimized() { return aiOptimized; }
    public void setAiOptimized(Integer aiOptimized) { this.aiOptimized = aiOptimized; }
    public String getAiFeedback() { return aiFeedback; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }
}
