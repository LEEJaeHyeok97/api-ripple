package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretConsumer {

  @Value("${db.password}")
  private String dbPassword;

  @Value("${api.token:SUPER_SECRET_VALUE_123}")
  private String apiToken;

  @Value("${signing.key}")
  private String signingKey;
}
