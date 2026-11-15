package io.github.leejaehyeok97.configguard.source;

import io.github.leejaehyeok97.configguard.model.SourceLocation;
import java.util.List;

/**
 * 설정 파일의 키 하나. 값은 보관하지 않고 값에서 뽑은 플레이스홀더 이름만 남긴다.
 *
 * @param canonicalKey 정규형 키
 * @param requiredPlaceholders 기본값 없는 플레이스홀더 이름 (원문 그대로. 예: {@code DB_URL})
 * @param referencedPlaceholders 값이 참조하는 모든 플레이스홀더 이름 (기본값 유무 무관). 미사용 키 판정에 쓴다.
 */
record ConfigEntry(
    String canonicalKey,
    SourceLocation location,
    List<String> requiredPlaceholders,
    List<String> referencedPlaceholders) {

  ConfigEntry {
    requiredPlaceholders = List.copyOf(requiredPlaceholders);
    referencedPlaceholders = List.copyOf(referencedPlaceholders);
  }
}
