package com.bancofortaleza.transactions.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private static final String[] LOCAL_ORIGIN_PATTERNS = {
        "http://localhost:*",
        "https://localhost:*",
        "http://127.0.0.1:*",
        "https://127.0.0.1:*",
        "http://0.0.0.0:*",
        "https://0.0.0.0:*",
        "http://host.docker.internal:*",
        "https://host.docker.internal:*"
    };

    private final TokenValidationInterceptor tokenValidationInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(LOCAL_ORIGIN_PATTERNS)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
            .allowedHeaders("*")
            .exposedHeaders("Authorization", "Location", "x-page", "x-page-size", "x-total-count", "x-total-pages")
            .allowCredentials(true)
            .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenValidationInterceptor)
            .addPathPatterns("/channel/v1/**");
    }
}
