package io.github.leejaehyeok97.configguard.compare;

import io.github.leejaehyeok97.configguard.normalize.RelaxedNames;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** ignoreKeys(glob)와 프레임워크 prefix 판정. */
final class KeyFilters {

  /** CG004(미사용 키)에서 빼는 프레임워크 키 prefix. Spring Boot와 그 생태계가 읽는 키다. */
  static final List<String> FRAMEWORK_PREFIXES =
      List.of("spring", "server", "management", "logging", "info", "debug", "trace");

  private final List<Pattern> ignore = new ArrayList<>();
  private final List<String> frameworkPrefixes = new ArrayList<>(FRAMEWORK_PREFIXES);

  KeyFilters(List<String> ignoreGlobs, List<String> extraFrameworkPrefixes) {
    for (String glob : ignoreGlobs) {
      ignore.add(globToPattern(glob));
    }
    frameworkPrefixes.addAll(extraFrameworkPrefixes);
  }

  /** relaxed binding 기준으로 비교한다. {@code *}는 점을 포함한 임의 문자열, {@code ?}는 한 글자. */
  boolean isIgnored(String key) {
    String match = RelaxedNames.matchKey(key);
    return ignore.stream().anyMatch(p -> p.matcher(match).matches());
  }

  boolean isFrameworkKey(String key) {
    return frameworkPrefixes.stream()
        .anyMatch(prefix -> RelaxedNames.isSelfOrAncestor(prefix, key));
  }

  private static Pattern globToPattern(String glob) {
    String normalized = RelaxedNames.matchKey(glob);
    StringBuilder regex = new StringBuilder();
    for (int i = 0; i < normalized.length(); i++) {
      char ch = normalized.charAt(i);
      switch (ch) {
        case '*' -> regex.append(".*");
        case '?' -> regex.append('.');
        default -> regex.append(Pattern.quote(String.valueOf(ch)));
      }
    }
    return Pattern.compile(regex.toString());
  }
}
