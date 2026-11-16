package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "alarm.push", name = "enabled", havingValue = "true")
public class FcmSender {

  @Value("${alarm.push.fcm-key}")
  private String fcmKey;
}
