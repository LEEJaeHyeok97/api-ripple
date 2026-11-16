package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExternalClient {

  @Value("${payment.api-key}")
  private String paymentApiKey;

  @Value("${external.service-url}")
  private String serviceUrl;

  @Value("${mail.sender-address}")
  private String senderAddress;

  @Value("${feature.flagName}")
  private String flagName;
}
