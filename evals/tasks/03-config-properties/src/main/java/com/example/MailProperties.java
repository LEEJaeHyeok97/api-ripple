package com.example;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("mail")
public class MailProperties {

  @NotBlank private String host;

  @NotBlank private String password;

  private int port = 25;
}
