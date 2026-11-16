package com.example;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

  private static final String IGNORED = "static fields are not bound";

  @NotBlank private String senderAddress;

  private int timeout = 30;

  private String replyTo;

  @NotNull private Integer maxRetries = 3;

  private Smtp smtp = new Smtp();

  @Valid private Auth auth;

  private Map<String, String> headers;

  private List<String> recipients;

  public static class Smtp {
    @NotBlank private String host;
    private int port = 25;
  }

  public static class Auth {
    @NotEmpty private String username;
  }
}
