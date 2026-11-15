package io.github.leejaehyeok97.configguard.model;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

/**
 * 기계가 바로 적용할 수 있는 수정 제안.
 *
 * @param file 수정할 파일 (프로젝트 루트 기준 상대 경로). {@link SuggestionKind#ADD_ENV_VAR}면 null.
 * @param target 추가할 키 이름 또는 환경변수 이름. 값은 넣지 않는다.
 */
public record Suggestion(SuggestionKind kind, @Nullable String file, String target) {

  public Suggestion {
    requireNonNull(kind, "kind");
    requireNonNull(target, "target");
    if (target.isBlank()) {
      throw new IllegalArgumentException("target must not be blank");
    }
    if (kind == SuggestionKind.ADD_ENV_VAR && file != null) {
      throw new IllegalArgumentException("ADD_ENV_VAR suggestion must not have a file");
    }
    if ((kind == SuggestionKind.ADD_YAML_KEY || kind == SuggestionKind.ADD_DEFAULT_VALUE)
        && file == null) {
      throw new IllegalArgumentException(kind + " suggestion requires a file");
    }
  }
}
