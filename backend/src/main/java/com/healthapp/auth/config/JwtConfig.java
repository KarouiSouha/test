package com.healthapp.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    
    private String secret ;
    private long accessTokenExpiration = 900000; // 15 minutes
    private long refreshTokenExpiration = 604800000; // 7 days
    private String issuer;
}
