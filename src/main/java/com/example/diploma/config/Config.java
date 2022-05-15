package com.example.diploma.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class Config {
    Long maxTimeTravel;
    Integer smoothingFactor;
    String osmPath;
}
