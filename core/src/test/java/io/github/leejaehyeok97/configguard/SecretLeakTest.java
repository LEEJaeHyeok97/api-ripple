package io.github.leejaehyeok97.configguard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.report.ConsoleRenderer;
import io.github.leejaehyeok97.configguard.report.JsonRenderer;
import io.github.leejaehyeok97.configguard.report.MarkdownRenderer;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/**
 * 명세 6.6 보안 규칙: 어떤 출력에도 설정 값을 포함하지 않는다. secret-leak fixture는 yml·properties·.env 값과 {@code @Value}
 * 기본값에 같은 더미 secret을 넣는다.
 */
class SecretLeakTest {

  @Test
  void fixtureReallyContainsTheSecretInEverySource() throws Exception {
    var project = Fixtures.path("secret-leak");
    for (String file :
        new String[] {
          "src/main/resources/application-prod.yml",
          "src/main/resources/application.properties",
          "deploy/prod.env",
          "src/main/java/com/example/SecretConsumer.java"
        }) {
      assertThat(Files.readString(project.resolve(file))).contains(FixtureTest.SECRET);
    }
  }

  @Test
  void noOutputContainsTheSecret() throws Exception {
    Report report = FixtureTest.analyze("secret-leak");

    assertThat(report.findings()).isNotEmpty();
    assertThat(ConsoleRenderer.render(report)).doesNotContain(FixtureTest.SECRET);
    assertThat(MarkdownRenderer.render(report)).doesNotContain(FixtureTest.SECRET);
    assertThat(JsonRenderer.render(report)).doesNotContain(FixtureTest.SECRET);
    assertThat(report.toString()).doesNotContain(FixtureTest.SECRET);
  }
}
