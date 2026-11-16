package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfraConfig {

  @Value("${mail.sender}")
  private String mailSender;

  @Value("${cache.ttl}")
  private String cacheTtl;

  @Value("${redis.host}")
  private String redisHost;
}
