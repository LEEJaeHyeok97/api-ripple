package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

class PaymentClientTest {

  // src/test는 config-guard가 분석하지 않는다. 이 키가 보고되면 버그다.
  @Value("${test.only.key}")
  private String testOnlyKey;

  @Test
  void configuredWithKey() {
    assertThat(new PaymentClient("key", Duration.ofSeconds(1)).isConfigured()).isTrue();
  }
}
