package io.github.leejaehyeok97.configguard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.leejaehyeok97.configguard.model.Finding;
import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.model.Severity;
import io.github.leejaehyeok97.configguard.report.ConsoleRenderer;
import io.github.leejaehyeok97.configguard.report.JsonRenderer;
import io.github.leejaehyeok97.configguard.report.MarkdownRenderer;
import io.github.leejaehyeok97.configguard.report.ReportSchemaAssert;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 명세 6.15의 fixture 8개. 각 fixture의 {@code expected.txt}와 Finding 목록이 <b>정확히</b> 같아야 한다. 빠진 것은 미탐, 남는
 * 것은 오탐이다.
 */
class FixtureTest {

  static final String SECRET = "SUPER_SECRET_VALUE_123";

  static Report analyze(String fixture) throws Exception {
    Path project = Fixtures.path(fixture);
    Properties props = new Properties();
    try (Reader reader =
        Files.newBufferedReader(project.resolve("fixture.properties"), StandardCharsets.UTF_8)) {
      props.load(reader);
    }
    List<String> profiles = Arrays.asList(props.getProperty("profiles").split(",", -1));
    Map<String, Path> envFiles = new LinkedHashMap<>();
    for (String profile : profiles) {
      String envFile = props.getProperty("env." + profile);
      if (envFile != null) {
        envFiles.put(profile, project.resolve(envFile));
      }
    }
    return ConfigGuard.analyze(
        AnalysisRequest.standard(project, profiles, envFiles, List.of(), List.of()));
  }

  private static List<String> expected(String fixture) throws Exception {
    return Files.readAllLines(Fixtures.path(fixture).resolve("expected.txt")).stream()
        .map(String::strip)
        .filter(l -> !l.isEmpty() && !l.startsWith("#"))
        .sorted()
        .toList();
  }

  private static String line(Finding f) {
    return f.profile()
        + " "
        + f.severity()
        + " "
        + f.ruleId()
        + " "
        + (f.key().isEmpty() ? "-" : f.key());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "value-basic",
        "config-properties",
        "conditional",
        "profiles",
        "env-placeholder",
        "relaxed-binding",
        "unanalyzable",
        "secret-leak"
      })
  void findingsMatchExpectedExactly(String fixture) throws Exception {
    Report report = analyze(fixture);

    assertThat(report.findings().stream().map(FixtureTest::line).sorted().toList())
        .isEqualTo(expected(fixture));
    assertThat(report.findings())
        .filteredOn(f -> f.severity() == Severity.ERROR)
        .allSatisfy(f -> assertThat(f.suggestions()).isNotEmpty());
    ReportSchemaAssert.assertValid(JsonRenderer.render(report));
    for (String output :
        List.of(
            ConsoleRenderer.render(report),
            MarkdownRenderer.render(report),
            JsonRenderer.render(report))) {
      assertThat(output).doesNotContain(SECRET);
    }
  }
}
