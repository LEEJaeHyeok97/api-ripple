package io.github.leejaehyeok97.configguard.model;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 검사 결과 하나.
 *
 * @param ruleId {@link Rule#id()}. 예: {@code CG001}
 * @param key 관련 키. CG002(MISSING_ENV_VAR)는 환경변수 이름.
 * @param location 위치를 특정할 수 없으면 null.
 * @param message 사람이 읽는 설명. 설정 값은 넣지 않는다.
 * @param suggestions 수정 제안. ERROR는 최소 1개를 가져야 한다 (명세 6.10).
 */
public record Finding(
    String ruleId,
    Severity severity,
    String profile,
    String key,
    @Nullable SourceLocation location,
    String message,
    List<Suggestion> suggestions) {

  public Finding {
    requireNonNull(ruleId, "ruleId");
    if (Rule.byId(ruleId).isEmpty()) {
      throw new IllegalArgumentException("unknown rule id: " + ruleId);
    }
    requireNonNull(severity, "severity");
    requireNonNull(profile, "profile");
    requireNonNull(key, "key");
    requireNonNull(message, "message");
    suggestions = List.copyOf(suggestions);
    if (severity == Severity.ERROR && suggestions.isEmpty()) {
      throw new IllegalArgumentException("ERROR finding requires at least one suggestion: " + key);
    }
  }
}
