package io.github.leejaehyeok97.configguard.compare;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 비교 설정.
 *
 * @param profiles 검사할 프로필 (순서대로 보고한다)
 * @param envVarsByProfile 프로필 → 환경변수 이름 목록. 키가 없는 프로필은 환경변수 검사를 건너뛴다.
 * @param ignoreKeys 모든 판정에서 뺄 키 glob ({@code legacy.*})
 * @param extraFrameworkPrefixes CG004 판정에서 뺄 prefix 추가분 ({@code resilience4j})
 */
public record CompareOptions(
    List<String> profiles,
    Map<String, Set<String>> envVarsByProfile,
    List<String> ignoreKeys,
    List<String> extraFrameworkPrefixes) {

  public CompareOptions {
    if (profiles.isEmpty()) {
      throw new IllegalArgumentException("at least one profile is required");
    }
    profiles = List.copyOf(profiles);
    envVarsByProfile = Map.copyOf(envVarsByProfile);
    ignoreKeys = List.copyOf(ignoreKeys);
    extraFrameworkPrefixes = List.copyOf(extraFrameworkPrefixes);
  }
}
