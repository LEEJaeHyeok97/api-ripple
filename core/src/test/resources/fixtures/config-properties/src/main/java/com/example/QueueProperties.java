package com.example;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.queue")
public class QueueProperties {

  private final String name;

  public QueueProperties(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
