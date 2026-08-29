package com.pos.factura.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(ApiConfig apiConfig) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(apiConfig.getConnectTimeoutMs());
        factory.setReadTimeout(apiConfig.getReadTimeoutMs());
        RestTemplate restTemplate = new RestTemplate(factory);

        // La API externa de factura electrónica responde JSON con Content-Type: text/plain
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
        restTemplate.getMessageConverters().add(0, jsonConverter);

        return restTemplate;
    }
}
