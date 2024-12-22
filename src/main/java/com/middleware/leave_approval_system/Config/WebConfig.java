package com.middleware.leave_approval_system.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration  // Marks this class as a configuration class for Spring, used to configure the application's web behavior
public class WebConfig implements WebMvcConfigurer { // // Implements WebMvcConfigurer to customize Spring MVC configuration

    // Override the addCorsMappings method to configure Cross-Origin Resource Sharing (CORS) settings
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Allow all paths
                .allowedOrigins("http://localhost:3000") // Allow requests from your frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed methods
                .allowCredentials(true); // Allow credentials (if needed)
    }
}
