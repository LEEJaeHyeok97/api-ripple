package io.github.leejaehyeok97.configguard.source;

import io.github.leejaehyeok97.configguard.normalize.RelaxedNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * 문서 활성화를 정하는 구조 키. 이 키들의 값은 프로필 이름이라서 읽는다. 다른 키의 값은 플레이스홀더 추출 외에는 쓰지 않는다.
 *
 * <ul>
 *   <li>{@code spring.config.activate.on-profile}
 *   <li>{@code spring.profiles.include}
 *   <li>{@code spring.profiles.group.<이름>}
 * </ul>
 */
final class StructuralKeys {

  private static final String ON_PROFILE =
      RelaxedNames.matchKey("spring.config.activate.on-profile");
  private static final String INCLUDE = RelaxedNames.matchKey("spring.profiles.include");
  private static final String GROUP_PREFIX = "spring.profiles.group.";

  private StructuralKeys() {}

  /** 문서 하나의 구조 키 값을 모은다. */
  static final class Collector {
    final List<String> onProfiles = new ArrayList<>();
    final List<String> includes = new ArrayList<>();
    final Map<String, List<String>> groups = new LinkedHashMap<>();
    boolean supported = true;

    /** 평탄화된 키(정규형)와 그 값. 구조 키가 아니면 아무것도 하지 않는다. */
    void accept(String canonicalKey, @Nullable String value) {
      if (value == null) {
        return;
      }
      String base = stripIndex(canonicalKey);
      String match = RelaxedNames.matchKey(base);
      if (match.equals(ON_PROFILE)) {
        acceptProfileExpression(value);
      } else if (match.equals(INCLUDE)) {
        includes.addAll(splitList(value, ','));
      } else if (base.startsWith(GROUP_PREFIX) && base.length() > GROUP_PREFIX.length()) {
        String group = base.substring(GROUP_PREFIX.length());
        groups.computeIfAbsent(group, g -> new ArrayList<>()).addAll(splitList(value, ','));
      }
    }

    private void acceptProfileExpression(String value) {
      // "prod", "prod,dev", "prod | dev"는 OR로 본다. 부정(!)과 AND(&), 괄호는 지원하지 않는다.
      for (String profile : splitList(value, ',', '|')) {
        if (profile.indexOf('!') >= 0
            || profile.indexOf('&') >= 0
            || profile.indexOf('(') >= 0
            || profile.indexOf(')') >= 0) {
          supported = false;
        } else {
          onProfiles.add(profile);
        }
      }
    }
  }

  /** 구분자로 나누고 공백을 없앤 뒤 빈 조각을 버린다. */
  static List<String> splitList(String value, char... separators) {
    List<String> parts = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (int i = 0; i <= value.length(); i++) {
      boolean end = i == value.length() || isSeparator(value.charAt(i), separators);
      if (end) {
        String part = current.toString().strip();
        if (!part.isEmpty()) {
          parts.add(part);
        }
        current.setLength(0);
      } else {
        current.append(value.charAt(i));
      }
    }
    return parts;
  }

  private static boolean isSeparator(char ch, char[] separators) {
    for (char separator : separators) {
      if (ch == separator) {
        return true;
      }
    }
    return false;
  }

  private static String stripIndex(String key) {
    int bracket = key.indexOf('[');
    return bracket < 0 ? key : key.substring(0, bracket);
  }
}
