package com.pos.factura.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "factura.emisor")
public class EmisorConfig {
    private String cuit;
    private String puntoVenta;
    private String razonSocial;
}
