package com.example;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.oauth")
public class OAuthProperties {

  @NotBlank private String clientSecret;
}
