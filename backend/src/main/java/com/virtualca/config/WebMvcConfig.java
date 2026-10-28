package com.virtualca.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseDir = System.getProperty("user.dir");
        File directFrontend = new File(baseDir, "frontend");
        File parentFrontend = new File(baseDir, "../frontend");

        registry.addResourceHandler("/frontend/**")
                .addResourceLocations(
                        "file:" + directFrontend.getAbsolutePath() + "/",
                        "file:" + parentFrontend.getAbsolutePath() + "/",
                        "classpath:/static/frontend/",
                        "classpath:/static/"
                );

        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "file:" + directFrontend.getAbsolutePath() + "/",
                        "classpath:/static/"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-Admin-Key")
                .allowCredentials(false)
                .maxAge(3600);
    }
}

