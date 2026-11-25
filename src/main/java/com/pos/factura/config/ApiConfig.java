package com.pos.factura.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "factura.api")
public class ApiConfig {
    private String url;
    private String usertoken;
    private String apitoken;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 15000;
    private int maxReintentos = 6;
    private long esperaEntreReintentosMs = 1000;
    private long consultaTimeoutMs = 30000;
}
