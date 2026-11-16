package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.sms.enabled", havingValue = "true")
public class SmsSender {

  @Value("${notification.sms.api-key}")
  private String apiKey;
}
