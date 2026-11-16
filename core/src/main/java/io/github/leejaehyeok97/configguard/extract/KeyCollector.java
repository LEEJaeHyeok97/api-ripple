package io.github.leejaehyeok97.configguard.extract;

import com.github.javaparser.ast.Node;
import io.github.leejaehyeok97.configguard.model.RequiredKey;
import io.github.leejaehyeok97.configguard.model.Requirement;
import io.github.leejaehyeok97.configguard.model.Unanalyzable;
import io.github.leejaehyeok97.configguard.normalize.RelaxedNames;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 추출기들이 결과를 모으는 곳. 조건부 강등을 한 곳에서 처리한다. */
final class KeyCollector {

  private final List<RequiredKey> keys = new ArrayList<>();
  private final List<Unanalyzable> unanalyzable = new ArrayList<>();

  /** 키를 기록한다. REQUIRED인데 {@code at}을 감싸는 선언에 조건이 있으면 CONDITIONAL로 강등하고 이유를 조건 설명으로 바꾼다. */
  void require(SourceFile file, Node at, String rawKey, Requirement requirement, String reason) {
    String key = RelaxedNames.canonical(rawKey);
    if (key.isEmpty()) {
      return;
    }
    Requirement effective = requirement;
    String why = reason;
    if (requirement == Requirement.REQUIRED) {
      Optional<String> condition = Conditions.describe(at);
      if (condition.isPresent()) {
        effective = Requirement.CONDITIONAL;
        why = condition.get();
      }
    }
    keys.add(new RequiredKey(key, effective, file.locationOf(at), why));
  }

  void unanalyzable(SourceFile file, Node at, String key, String message) {
    unanalyzable.add(new Unanalyzable(key, file.locationOf(at), message));
  }

  ExtractionResult result() {
    return new ExtractionResult(keys, unanalyzable);
  }
}
