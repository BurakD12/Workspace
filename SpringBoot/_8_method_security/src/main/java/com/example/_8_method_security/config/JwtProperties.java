package com.example._8_method_security.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "app.security.jwt")
@Getter
@Setter
public class JwtProperties {
    private String secret = "pp0gXRqpg69kqphvhFq5XK2JB4lxe8noQUoDG4RAslaDXvP/3idY/I+PV5M1EmNB";
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(7);
}
