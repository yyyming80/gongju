package com.toolbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "toolbox.upload")
public class UploadProperties {
    private String path = "/www/toolbox/uploads";
    private String baseUrl = "http://localhost:8080/files/";

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
