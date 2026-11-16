package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentClient {

  @Value("${payment.api-key}")
  private String apiKey;

  @Value("${payment.timeout:30}")
  private int timeout;

  @Value("${payment.region:}")
  private String region;

  @Value("${payment.url:${payment.fallback-url}}")
  private String url;

  @Value("${payment.retry:${payment.default-retry:3}}")
  private int retry;

  @Value("https://${payment.host}/v1/${payment.version:v1}")
  private String endpoint;

  @Value("plain text without placeholder")
  private String literal;

  public PaymentClient(@Value("${payment.merchantId}") String merchantId) {}
}
