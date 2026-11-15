package io.github.leejaehyeok97.configguard.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SourceLocationTest {

  @Test
  void fileNameDropsDirectories() {
    var location = new SourceLocation("src/main/java/com/example/PaymentClient.java", 23);

    assertThat(location.fileName()).isEqualTo("PaymentClient.java");
    assertThat(location).hasToString("src/main/java/com/example/PaymentClient.java:23");
  }

  @Test
  void fileWithoutDirectoryAndUnknownLine() {
    var location = new SourceLocation("application.yml", 0);

    assertThat(location.fileName()).isEqualTo("application.yml");
    assertThat(location).hasToString("application.yml");
  }

  @Test
  void rejectsInvalidValues() {
    assertThatThrownBy(() -> new SourceLocation(" ", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SourceLocation("a.yml", -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
