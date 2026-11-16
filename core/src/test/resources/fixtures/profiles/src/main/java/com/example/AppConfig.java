package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {

  @Value("${app.name}")
  private String name;

  @Value("${app.feature.timeout}")
  private int timeout;

  @Value("${app.region}")
  private String region;

  @Value("${payment.api-key}")
  private String paymentApiKey;

  @Value("${db.pool.max-size}")
  private int poolSize;

  @Value("${common.tracing}")
  private boolean tracing;
}
