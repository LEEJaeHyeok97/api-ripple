package io.github.leejaehyeok97.configguard.model;

import static java.util.Objects.requireNonNull;

/**
 * 정적으로 분석할 수 없어 건너뛴 패턴 (SpEL, 동적 키, 지원하지 않는 프로필 식 등). 비교 단계에서 CG005 INFO가 된다.
 *
 * @param key 관련 키를 알 수 있으면 그 키, 모르면 빈 문자열.
 * @param message 사람이 읽는 설명. 설정 값은 넣지 않는다.
 */
public record Unanalyzable(String key, SourceLocation location, String message) {

  public Unanalyzable {
    requireNonNull(key, "key");
    requireNonNull(location, "location");
    requireNonNull(message, "message");
  }
}
