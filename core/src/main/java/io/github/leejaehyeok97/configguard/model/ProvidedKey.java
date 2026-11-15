package io.github.leejaehyeok97.configguard.model;

import static java.util.Objects.requireNonNull;

import java.util.List;

/**
 * 설정 파일이 제공하는 키. 값 자체는 보관하지 않는다.
 *
 * @param profile {@code default}, {@code dev}, {@code prod} ...
 * @param requiredEnvVars 값 안의 {@code ${ENV}} 중 기본값이 없는 것의 이름
 */
public record ProvidedKey(
    String canonicalKey, String profile, SourceLocation location, List<String> requiredEnvVars) {

  public static final String DEFAULT_PROFILE = "default";

  public ProvidedKey {
    requireNonNull(canonicalKey, "canonicalKey");
    requireNonNull(profile, "profile");
    requireNonNull(location, "location");
    requiredEnvVars = List.copyOf(requiredEnvVars);
  }
}
