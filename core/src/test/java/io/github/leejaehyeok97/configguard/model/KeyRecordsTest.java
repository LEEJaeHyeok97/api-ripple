package io.github.leejaehyeok97.configguard.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeyRecordsTest {

  private static final SourceLocation LOCATION =
      new SourceLocation("src/main/resources/application-prod.yml", 4);

  @Test
  void providedKeyCopiesEnvVarList() {
    var envVars = new ArrayList<>(List.of("DB_URL"));
    var key = new ProvidedKey("spring.datasource.url", "prod", LOCATION, envVars);

    envVars.add("OTHER");

    assertThat(key.requiredEnvVars()).containsExactly("DB_URL");
    assertThat(ProvidedKey.DEFAULT_PROFILE).isEqualTo("default");
  }

  @Test
  void requiredKeyHoldsRequirementAndReason() {
    var key = new RequiredKey("payment.api-key", Requirement.REQUIRED, LOCATION, "@Value, 기본값 없음");

    assertThat(key.requirement()).isEqualTo(Requirement.REQUIRED);
    assertThat(key.reason()).isEqualTo("@Value, 기본값 없음");
  }

  @Test
  @SuppressWarnings("NullAway")
  void rejectsNulls() {
    assertThatThrownBy(() -> new RequiredKey(null, Requirement.OPTIONAL, LOCATION, "r"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new ProvidedKey("a", "prod", LOCATION, null))
        .isInstanceOf(NullPointerException.class);
  }
}
