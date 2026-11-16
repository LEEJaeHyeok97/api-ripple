package io.github.leejaehyeok97.configguard.extract;

import io.github.leejaehyeok97.configguard.model.RequiredKey;
import io.github.leejaehyeok97.configguard.model.Unanalyzable;
import java.util.List;

/** 코드에서 뽑은 필요한 키와, 분석하지 못한 패턴. */
public record ExtractionResult(List<RequiredKey> keys, List<Unanalyzable> unanalyzable) {

  public ExtractionResult {
    keys = List.copyOf(keys);
    unanalyzable = List.copyOf(unanalyzable);
  }
}
