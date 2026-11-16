package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "alarm.push", name = "enabled", havingValue = "true")
public class FcmSender {

  private final String fcmKey;

  public FcmSender(@Value("${alarm.push.fcm-key}") String fcmKey) {
    this.fcmKey = fcmKey;
  }

  public boolean isConfigured() {
    return !fcmKey.isBlank();
  }
}
