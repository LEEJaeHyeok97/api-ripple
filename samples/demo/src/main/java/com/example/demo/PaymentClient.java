package com.example.demo;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentClient {

  private final String apiKey;
  private final Duration timeout;

  public PaymentClient(
      @Value("${payment.api-key}") String apiKey, @Value("${payment.timeout:5s}") Duration timeout) {
    this.apiKey = apiKey;
    this.timeout = timeout;
  }

  public boolean isConfigured() {
    return !apiKey.isBlank() && !timeout.isNegative();
  }
}
