package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

  private final Environment env;

  @Value("${notification.sender}")
  private String sender;

  public NotificationService(Environment env) {
    this.env = env;
  }

  String webhookToken() {
    return env.getRequiredProperty("notification.webhook-token");
  }
}
