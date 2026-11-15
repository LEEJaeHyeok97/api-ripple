package io.github.leejaehyeok97.configguard.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SuggestionTest {

  @Test
  void envVarSuggestionHasNoFile() {
    var suggestion = new Suggestion(SuggestionKind.ADD_ENV_VAR, null, "PAYMENT_APIKEY");

    assertThat(suggestion.file()).isNull();
    assertThatThrownBy(() -> new Suggestion(SuggestionKind.ADD_ENV_VAR, "a.yml", "DB_URL"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fileBasedSuggestionsRequireFile() {
    assertThatThrownBy(() -> new Suggestion(SuggestionKind.ADD_YAML_KEY, null, "payment.api-key"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Suggestion(SuggestionKind.ADD_DEFAULT_VALUE, null, "a.b"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new Suggestion(SuggestionKind.IGNORE_KEY, null, "legacy.*").file()).isNull();
  }

  @Test
  void rejectsBlankTarget() {
    assertThatThrownBy(() -> new Suggestion(SuggestionKind.IGNORE_KEY, null, " "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
