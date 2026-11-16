package com.example;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

@Validated
@Profile("prod")
@ConfigurationProperties("audit")
public class AuditProperties {

  @NotBlank private String endpoint;
}
