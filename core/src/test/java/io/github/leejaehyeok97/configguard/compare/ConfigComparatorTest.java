package io.github.leejaehyeok97.configguard.compare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.leejaehyeok97.configguard.extract.ExtractionResult;
import io.github.leejaehyeok97.configguard.model.Finding;
import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.model.RequiredKey;
import io.github.leejaehyeok97.configguard.model.Requirement;
import io.github.leejaehyeok97.configguard.model.SourceLocation;
import io.github.leejaehyeok97.configguard.model.SuggestionKind;
import io.github.leejaehyeok97.configguard.source.ConfigSources;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigComparatorTest {

  @TempDir Path dir;
  private ConfigSources sources;

  @BeforeEach
  void setUp() throws Exception {
    Path resources = Files.createDirectories(dir.resolve("src/main/resources"));
    Files.writeString(
        resources.resolve("application.yml"),
        """
        base.url: https://example.test
        app.link: ${base.url}/x
        resilience4j.retry.max: 3
        legacy.flag: true
        spring.application.name: demo
        """);
    sources = ConfigSources.load(dir, List.of(resources));
  }

  private static RequiredKey key(String name, Requirement requirement) {
    return new RequiredKey(name, requirement, new SourceLocation("A.java", 1), "test");
  }

  private Report compare(List<RequiredKey> keys, CompareOptions options) {
    return ConfigComparator.compare(new ExtractionResult(keys, List.of()), sources, options);
  }

  private static List<String> lines(Report report) {
    return report.findings().stream()
        .map(f -> f.profile() + " " + f.ruleId() + " " + f.key())
        .toList();
  }

  @Test
  void ignoreGlobsAndExtraFrameworkPrefixes() {
    var options =
        new CompareOptions(
            List.of("prod"),
            Map.of(),
            List.of("missing.*", "legacy.?lag"),
            List.of("resilience4j"));

    Report report =
        compare(
            List.of(
                key("missing.one", Requirement.REQUIRED), key("app.link", Requirement.OPTIONAL)),
            options);

    assertThat(lines(report)).isEmpty();
  }

  @Test
  void placeholderReferencesCountAsUsageAndEnvVarsProvideKeys() {
    var options =
        new CompareOptions(
            List.of("prod"), Map.of("prod", Set.of("PAYMENT_APIKEY")), List.of(), List.of());

    Report report =
        compare(
            List.of(
                key("payment.api-key", Requirement.REQUIRED),
                key("app.link", Requirement.OPTIONAL)),
            options);

    assertThat(lines(report))
        .containsExactly("* CG004 legacy.flag", "* CG004 resilience4j.retry.max");
    assertThat(report.notices()).isEmpty();
  }

  @Test
  void strongestRequirementWinsAndSuggestionsPointToProfileFile() {
    var options =
        new CompareOptions(
            List.of("staging"),
            Map.of(),
            List.of("base.*", "app.*", "legacy.*", "resilience4j.*"),
            List.of());

    Report report =
        compare(
            List.of(
                key("feature.x", Requirement.OPTIONAL),
                key("feature.X", Requirement.CONDITIONAL),
                key("feature.x", Requirement.REQUIRED)),
            options);

    assertThat(report.findings()).hasSize(1);
    Finding finding = report.findings().get(0);
    assertThat(finding.ruleId()).isEqualTo("CG001");
    assertThat(finding.suggestions())
        .extracting(s -> s.kind() + " " + s.file() + " " + s.target())
        .containsExactly(
            SuggestionKind.ADD_YAML_KEY + " src/main/resources/application-staging.yml feature.x",
            SuggestionKind.ADD_ENV_VAR + " null FEATURE_X");
    assertThat(report.notices()).singleElement().extracting(n -> n.profile()).isEqualTo("staging");
  }

  @Test
  void suggestedFileForDefaultProfileIsTheBaseFile() {
    assertThat(sources.suggestedFile("default")).isEqualTo("src/main/resources/application.yml");
  }

  @Test
  void requiresAtLeastOneProfile() {
    assertThatThrownBy(() -> new CompareOptions(List.of(), Map.of(), List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
