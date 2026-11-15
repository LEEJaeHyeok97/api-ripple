package io.github.leejaehyeok97.configguard.source;

import io.github.leejaehyeok97.configguard.model.ProvidedKey;
import io.github.leejaehyeok97.configguard.normalize.RelaxedNames;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 한 프로필로 기동했을 때 실제로 보이는 설정 키 집합 (default ∪ 해당 프로필 ∪ include/group 1단계).
 *
 * @param profile 대상 프로필
 * @param activeProfiles 병합에 쓴 프로필 목록 (include, 대상, group 순)
 * @param keys 비교 키({@link RelaxedNames#matchKey}) → 마지막으로 덮어쓴 {@link ProvidedKey}
 * @param referencedPlaceholders 유효한 값들이 참조하는 플레이스홀더 이름 전체 (기본값 유무 무관)
 */
public record EffectiveConfig(
    String profile,
    List<String> activeProfiles,
    Map<String, ProvidedKey> keys,
    Set<String> referencedPlaceholders) {

  public EffectiveConfig {
    activeProfiles = List.copyOf(activeProfiles);
    keys = Map.copyOf(keys);
    referencedPlaceholders = Set.copyOf(referencedPlaceholders);
  }

  /** relaxed binding 규칙으로 키를 찾는다. */
  public Optional<ProvidedKey> find(String key) {
    return Optional.ofNullable(keys.get(RelaxedNames.matchKey(key)));
  }

  /** 키 자체 또는 그 하위 키(맵·리스트 원소)가 있는지. */
  public boolean containsSelfOrDescendant(String key) {
    return keys.values().stream()
        .anyMatch(p -> RelaxedNames.isSelfOrAncestor(key, p.canonicalKey()));
  }
}
