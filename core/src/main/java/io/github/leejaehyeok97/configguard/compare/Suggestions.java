package io.github.leejaehyeok97.configguard.compare;

import io.github.leejaehyeok97.configguard.model.Suggestion;
import io.github.leejaehyeok97.configguard.model.SuggestionKind;
import io.github.leejaehyeok97.configguard.normalize.RelaxedNames;
import java.util.List;

/** 규칙별 수정 제안. 모든 ERROR는 최소 1개를 가진다 (명세 6.10). 값은 넣지 않고 위치와 이름만 준다. */
final class Suggestions {

  private Suggestions() {}

  /** CG001: 프로필 설정 파일에 키를 넣거나, 배포 환경에 환경변수로 준다. */
  static List<Suggestion> forMissingKey(String file, String key) {
    String envName = RelaxedNames.envVarCandidates(key).iterator().next();
    return List.of(
        new Suggestion(SuggestionKind.ADD_YAML_KEY, file, key),
        new Suggestion(SuggestionKind.ADD_ENV_VAR, null, envName));
  }

  /** CG003: 조건이 켜지는 환경이라면 키를 넣는다. */
  static List<Suggestion> forConditionalKey(String file, String key) {
    return List.of(new Suggestion(SuggestionKind.ADD_YAML_KEY, file, key));
  }

  /** CG002: 배포 환경(과 환경변수 목록 파일)에 환경변수를 추가한다. */
  static List<Suggestion> forMissingEnvVar(String name) {
    return List.of(new Suggestion(SuggestionKind.ADD_ENV_VAR, null, name));
  }
}
