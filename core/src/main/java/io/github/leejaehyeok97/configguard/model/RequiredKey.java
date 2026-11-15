package io.github.leejaehyeok97.configguard.model;

import static java.util.Objects.requireNonNull;

/**
 * 코드가 요구하는 설정 키.
 *
 * @param canonicalKey 정규화된 키. 예: {@code payment.api-key}
 * @param reason 사람이 읽는 근거. 예: {@code @Value, 기본값 없음}. 설정 값은 넣지 않는다.
 */
public record RequiredKey(
    String canonicalKey, Requirement requirement, SourceLocation location, String reason) {

  public RequiredKey {
    requireNonNull(canonicalKey, "canonicalKey");
    requireNonNull(requirement, "requirement");
    requireNonNull(location, "location");
    requireNonNull(reason, "reason");
  }
}
