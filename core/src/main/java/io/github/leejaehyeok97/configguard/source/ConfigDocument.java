package io.github.leejaehyeok97.configguard.source;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * 설정 파일 안의 문서 하나. yml은 {@code ---}, properties는 {@code #---}로 나뉜다.
 *
 * @param file 프로젝트 루트 기준 상대 경로
 * @param fileProfile 파일 이름의 프로필. {@code application-prod.yml}이면 {@code prod}, {@code
 *     application.yml}이면 null.
 * @param line 문서가 시작하는 라인
 * @param onProfiles {@code spring.config.activate.on-profile} 값. 비어 있으면 조건 없이 활성.
 * @param includes {@code spring.profiles.include} 값
 * @param groups {@code spring.profiles.group.<이름>} 값
 * @param supported 지원하지 않는 프로필 식({@code !}, {@code &}, 괄호)을 쓰면 false. 이 문서는 어느 프로필에도 넣지 않는다.
 */
record ConfigDocument(
    String file,
    @Nullable String fileProfile,
    int line,
    List<String> onProfiles,
    List<String> includes,
    Map<String, List<String>> groups,
    List<ConfigEntry> entries,
    boolean supported) {

  /** 활성 프로필 목록이 {@code active}일 때 이 문서가 적용되는지. Spring ConfigData 활성화 규칙과 같다. */
  boolean appliesTo(List<String> active) {
    if (!supported) {
      return false;
    }
    if (fileProfile != null && !active.contains(fileProfile)) {
      return false;
    }
    return onProfiles.isEmpty() || onProfiles.stream().anyMatch(active::contains);
  }

  /** 프로필 조건이 전혀 없는 기본 문서인지. */
  boolean isUnconditional() {
    return supported && fileProfile == null && onProfiles.isEmpty();
  }
}
