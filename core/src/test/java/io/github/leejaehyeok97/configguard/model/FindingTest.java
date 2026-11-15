package io.github.leejaehyeok97.configguard.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindingTest {

  private static final SourceLocation LOCATION =
      new SourceLocation("src/main/java/com/example/PaymentClient.java", 23);

  @Test
  void errorFindingRequiresSuggestion() {
    assertThatThrownBy(
            () ->
                new Finding(
                    "CG001",
                    Severity.ERROR,
                    "prod",
                    "payment.api-key",
                    LOCATION,
                    "@Value, 기본값 없음",
                    List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("payment.api-key");
  }

  @Test
  void infoFindingMayHaveNoSuggestionAndNoLocation() {
    var finding = new Finding("CG005", Severity.INFO, "prod", "", null, "SpEL, 분석 불가", List.of());

    assertThat(finding.location()).isNull();
    assertThat(finding.suggestions()).isEmpty();
  }

  @Test
  void suggestionsAreDefensivelyCopied() {
    var suggestions = new ArrayList<Suggestion>();
    suggestions.add(
        new Suggestion(
            SuggestionKind.ADD_YAML_KEY,
            "src/main/resources/application-prod.yml",
            "payment.api-key"));
    var finding =
        new Finding(
            "CG001",
            Severity.ERROR,
            "prod",
            "payment.api-key",
            LOCATION,
            "@Value, 기본값 없음",
            suggestions);

    suggestions.clear();

    assertThat(finding.suggestions()).hasSize(1);
  }

  @Test
  void rejectsUnknownRuleId() {
    assertThatThrownBy(() -> new Finding("CG999", Severity.INFO, "prod", "a", null, "m", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
