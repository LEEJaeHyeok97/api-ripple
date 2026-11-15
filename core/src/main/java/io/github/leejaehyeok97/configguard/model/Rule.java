package io.github.leejaehyeok97.configguard.model;

import java.util.Optional;

/**
 * 규칙 목록. 규칙 ID는 한 번 공개하면 의미를 바꾸지 않는다 (명세 5장). 규칙마다 {@code docs/reference/rules/CGxxx.md} 문서가 있다.
 */
public enum Rule {
  MISSING_KEY("CG001", Severity.ERROR),
  MISSING_ENV_VAR("CG002", Severity.ERROR),
  CONDITIONAL_KEY("CG003", Severity.WARNING),
  UNUSED_KEY("CG004", Severity.INFO),
  UNANALYZABLE("CG005", Severity.INFO);

  private final String id;
  private final Severity defaultSeverity;

  Rule(String id, Severity defaultSeverity) {
    this.id = id;
    this.defaultSeverity = defaultSeverity;
  }

  public String id() {
    return id;
  }

  public Severity defaultSeverity() {
    return defaultSeverity;
  }

  /** {@code CG001}처럼 ID로 찾는다. 대소문자는 구분하지 않는다. */
  public static Optional<Rule> byId(String id) {
    for (Rule rule : values()) {
      if (rule.id.equalsIgnoreCase(id)) {
        return Optional.of(rule);
      }
    }
    return Optional.empty();
  }
}
