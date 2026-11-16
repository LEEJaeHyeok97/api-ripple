package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentClient {

  @Value("${payment.api-key}")
  private String apiKey;

  @Value("${payment.base-url}")
  private String baseUrl;
}
