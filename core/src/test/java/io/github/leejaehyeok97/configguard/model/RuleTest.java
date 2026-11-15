package io.github.leejaehyeok97.configguard.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuleTest {

  @Test
  void ruleIdsAndSeveritiesMatchSpec() {
    // 규칙 ID는 공개 후 의미를 바꾸지 않는다. 이 테스트가 깨지면 호환성이 깨진 것이다.
    assertThat(Rule.values())
        .extracting(Rule::id, Rule::defaultSeverity)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("CG001", Severity.ERROR),
            org.assertj.core.groups.Tuple.tuple("CG002", Severity.ERROR),
            org.assertj.core.groups.Tuple.tuple("CG003", Severity.WARNING),
            org.assertj.core.groups.Tuple.tuple("CG004", Severity.INFO),
            org.assertj.core.groups.Tuple.tuple("CG005", Severity.INFO));
  }

  @Test
  void findsRuleById() {
    assertThat(Rule.byId("CG002")).contains(Rule.MISSING_ENV_VAR);
    assertThat(Rule.byId("cg004")).contains(Rule.UNUSED_KEY);
    assertThat(Rule.byId("CG999")).isEmpty();
  }
}
