package com.toolbox.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UploadProperties uploadProperties;

    @Autowired
    private LoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取实际的上传目录
        String uploadPath = uploadProperties.getPath();
        
        // 确保目录存在
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // 确保子目录也存在
        String imagesDir = uploadPath + File.separator + "images";
        File imagesFile = new File(imagesDir);
        if (!imagesFile.exists()) {
            imagesFile.mkdirs();
        }

        // 映射 /files/** 到上传目录
        String filesPath = uploadPath;
        if (!filesPath.endsWith(File.separator)) {
            filesPath = filesPath + File.separator;
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + filesPath);

        // 映射 /images/** 到上传目录（兼容旧路径）
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + filesPath);

        // 映射 /resume/** 到上传目录
        String resumeDir = uploadPath + File.separator + "resume";
        File resumeFile = new File(resumeDir);
        if (!resumeFile.exists()) {
            resumeFile.mkdirs();
        }
        String resumePath = filesPath + "resume" + File.separator;
        registry.addResourceHandler("/resume/**")
                .addResourceLocations("file:" + resumePath);

        System.out.println("========================================");
        System.out.println("静态资源映射配置:");
        System.out.println("  /files/** -> " + filesPath);
        System.out.println("  /images/** -> " + filesPath);
        System.out.println("  /resume/** -> " + resumePath);
        System.out.println("  上传目录: " + uploadDir.getAbsolutePath());
        System.out.println("========================================");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
