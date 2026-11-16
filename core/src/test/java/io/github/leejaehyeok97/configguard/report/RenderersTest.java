package io.github.leejaehyeok97.configguard.report;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.leejaehyeok97.configguard.model.Finding;
import io.github.leejaehyeok97.configguard.model.Notice;
import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.model.Severity;
import io.github.leejaehyeok97.configguard.model.SourceLocation;
import io.github.leejaehyeok97.configguard.model.Suggestion;
import io.github.leejaehyeok97.configguard.model.SuggestionKind;
import io.github.leejaehyeok97.configguard.model.Summary;
import java.util.List;
import org.junit.jupiter.api.Test;

class RenderersTest {

  private static final String PROD_FILE = "src/main/resources/application-prod.yml";

  /** 명세 6.6의 콘솔 예시와 같은 Finding. */
  private static Report specExample() {
    List<Finding> findings =
        List.of(
            new Finding(
                "CG001",
                Severity.ERROR,
                "prod",
                "payment.api-key",
                new SourceLocation("src/main/java/com/example/PaymentClient.java", 23),
                "@Value, 기본값 없음",
                List.of(new Suggestion(SuggestionKind.ADD_YAML_KEY, PROD_FILE, "payment.api-key"))),
            new Finding(
                "CG002",
                Severity.ERROR,
                "prod",
                "DB_URL",
                new SourceLocation(PROD_FILE, 4),
                "spring.datasource.url이 요구",
                List.of(new Suggestion(SuggestionKind.ADD_ENV_VAR, null, "DB_URL"))),
            new Finding(
                "CG003",
                Severity.WARNING,
                "prod",
                "alarm.push.fcm-key",
                new SourceLocation("src/main/java/com/example/FcmProperties.java", 12),
                "@ConditionalOnProperty(alarm.push.enabled)",
                List.of()),
            new Finding(
                "CG004",
                Severity.INFO,
                "prod",
                "legacy.s3.bucket",
                new SourceLocation(PROD_FILE, 41),
                "사용되지 않는 키",
                List.of()));
    return new Report(Summary.of(List.of("prod"), findings), findings, List.of());
  }

  @Test
  void consoleMatchesSpecExample() {
    assertThat(ConsoleRenderer.render(specExample()))
        .isEqualTo(
            """
            config-guard: prod
              ERROR  payment.api-key       PaymentClient.java:23  @Value, 기본값 없음
              ERROR  env DB_URL            application-prod.yml:4  spring.datasource.url이 요구
              WARN   alarm.push.fcm-key    FcmProperties.java:12  @ConditionalOnProperty(alarm.push.enabled)
              INFO   legacy.s3.bucket      application-prod.yml:41  사용되지 않는 키
            2 errors, 1 warning, 1 info
            """);
  }

  @Test
  void consoleShowsCleanProfilesNoticesAndGlobalSection() {
    List<Finding> findings =
        List.of(
            new Finding("CG005", Severity.INFO, "*", "", null, "SpEL, 분석 불가", List.of()),
            new Finding(
                "CG005",
                Severity.INFO,
                "*",
                "",
                new SourceLocation("Broken.java", 0),
                "파싱 실패",
                List.of()));
    Report report =
        new Report(
            Summary.of(List.of("dev", "prod"), findings),
            findings,
            List.of(new Notice(Notice.ENV_CHECK_SKIPPED, "prod", "검사 건너뜀")));

    assertThat(ConsoleRenderer.render(report))
        .isEqualTo(
            """
            config-guard: dev
              문제 없음
            config-guard: prod
              NOTE   검사 건너뜀
            config-guard: 모든 프로필 공통
              INFO   -    -  SpEL, 분석 불가
              INFO   -    Broken.java  파싱 실패
            0 errors, 0 warnings, 2 info
            """);
  }

  @Test
  void markdownHasMarkerFoldsInfoAndMarksCleanProfiles() {
    Report example = specExample();
    Report clean = new Report(Summary.of(List.of("dev"), List.of()), List.of(), List.of());

    String markdown = MarkdownRenderer.render(example);

    assertThat(markdown).startsWith(MarkdownRenderer.MARKER + "\n");
    assertThat(markdown).contains("❌ prod: ERROR 2건");
    assertThat(markdown).contains("| ERROR | CG002 | `env DB_URL` | `" + PROD_FILE + ":4` |");
    assertThat(markdown).contains("<details>\n<summary>INFO 1건</summary>");
    assertThat(markdown.indexOf("legacy.s3.bucket")).isGreaterThan(markdown.indexOf("<details>"));
    assertThat(MarkdownRenderer.render(clean)).contains("✅ dev: 문제 없음");
  }

  @Test
  void markdownEscapesTableBreakingCharacters() {
    List<Finding> findings =
        List.of(new Finding("CG005", Severity.INFO, "*", "", null, "a|b\nc", List.of()));
    Report report = new Report(Summary.of(List.of("prod"), findings), findings, List.of());

    assertThat(MarkdownRenderer.render(report)).contains("| a\\|b c |").contains("| - |");
  }

  @Test
  void jsonIsStableAndValid() throws Exception {
    String json = JsonRenderer.render(specExample());

    assertThat(json).isEqualTo(JsonRenderer.render(specExample()));
    assertThat(json).startsWith("{\n  \"schemaVersion\": \"1\",").endsWith("}\n");
    assertThat(json).contains("\"file\": null").doesNotContain("\r");
    ReportSchemaAssert.assertValid(json);
  }

  @Test
  void schemaRejectsErrorWithoutSuggestionAndUnknownFields() throws Exception {
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var node =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.readTree(JsonRenderer.render(specExample()));
    ((com.fasterxml.jackson.databind.node.ObjectNode) node.get("findings").get(0))
        .putArray("suggestions");
    node.put("extra", true);

    assertThat(ReportSchemaAssert.errors(node))
        .anyMatch(e -> e.contains("/findings/0/suggestions"))
        .anyMatch(e -> e.contains("extra"));
  }

  @Test
  void schemaRejectsFileOnEnvVarSuggestionAndUnknownRule() throws Exception {
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var node =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            mapper.readTree(JsonRenderer.render(specExample()));
    var finding = (com.fasterxml.jackson.databind.node.ObjectNode) node.get("findings").get(1);
    ((com.fasterxml.jackson.databind.node.ObjectNode) finding.get("suggestions").get(0))
        .put("file", "a.yml");
    finding.put("ruleId", "CG999");

    assertThat(ReportSchemaAssert.errors(node))
        .anyMatch(e -> e.contains("/findings/1/suggestions/0/file"))
        .anyMatch(e -> e.contains("/findings/1/ruleId"));
  }
}
