package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DynamicConfig {

  private static final String KEY = "app.const-key";

  @Value("#{systemProperties['user.region']}")
  private String region;

  @Value("${" + KEY + "}")
  private String constKey;

  @Value(Constants.EXTERNAL)
  private String external;

  @Value("${${tenant}.url}")
  private String tenantUrl;

  private final Environment env;

  public DynamicConfig(Environment env) {
    this.env = env;
  }

  String lookup(String name) {
    return env.getProperty(name);
  }

  String required() {
    return env.getRequiredProperty("app.env-required");
  }

  String optional() {
    return this.env.getProperty("app.env-optional", "fallback");
  }

  String systemProperty() {
    return System.getProperty("user.home");
  }

  String fromContext(ApplicationContext context) {
    return context.getEnvironment().getProperty("app.context-key");
  }
}
