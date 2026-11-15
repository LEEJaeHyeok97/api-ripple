package io.github.leejaehyeok97.configguard.normalize;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ${name:default}} 플레이스홀더 파서. Spring PropertyPlaceholderHelper와 같은 규칙으로 중첩된 {@code ${...}}를
 * 짝 맞춰 찾는다.
 *
 * <ul>
 *   <li>{@code ${a}} → a는 기본값 없음(required)
 *   <li>{@code ${a:x}}, {@code ${a:}} → a는 기본값 있음
 *   <li>{@code ${a:${b}}} → a는 기본값 있음, b는 기본값 없음 (기본값 안을 재귀적으로 분석)
 *   <li>{@code ${${x}.y}} → 이름이 동적이므로 {@code dynamic}
 * </ul>
 *
 * 기본값 문자열 자체는 결과에 담지 않는다.
 */
public final class Placeholders {

  public record Reference(String name, boolean hasDefault, boolean dynamic) {}

  private static final String PREFIX = "${";
  private static final char SUFFIX = '}';
  private static final char SEPARATOR = ':';

  private Placeholders() {}

  public static List<Reference> parse(String text) {
    List<Reference> out = new ArrayList<>();
    if (text != null) {
      collect(text, out);
    }
    return out;
  }

  /** 기본값이 없는 플레이스홀더 이름(동적 이름 제외). */
  public static List<String> requiredNames(String text) {
    List<String> names = new ArrayList<>();
    for (Reference ref : parse(text)) {
      if (!ref.hasDefault() && !ref.dynamic() && !names.contains(ref.name())) {
        names.add(ref.name());
      }
    }
    return names;
  }

  public static boolean containsPlaceholder(String text) {
    return text != null && text.contains(PREFIX);
  }

  private static void collect(String text, List<Reference> out) {
    int i = 0;
    while (true) {
      int start = text.indexOf(PREFIX, i);
      if (start < 0) {
        return;
      }
      if (start > 0 && text.charAt(start - 1) == '\\') {
        i = start + PREFIX.length();
        continue;
      }
      int end = findEnd(text, start + PREFIX.length());
      if (end < 0) {
        return;
      }
      String content = text.substring(start + PREFIX.length(), end);
      int sep = findSeparator(content);
      String name = sep < 0 ? content : content.substring(0, sep);
      boolean hasDefault = sep >= 0;
      boolean dynamic = name.contains(PREFIX);
      out.add(new Reference(name.strip(), hasDefault, dynamic));
      if (hasDefault) {
        collect(content.substring(sep + 1), out);
      }
      i = end + 1;
    }
  }

  private static int findEnd(String text, int from) {
    int depth = 0;
    for (int i = from; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '$' && i + 1 < text.length() && text.charAt(i + 1) == '{') {
        depth++;
        i++;
      } else if (ch == '{') {
        depth++;
      } else if (ch == SUFFIX) {
        if (depth == 0) {
          return i;
        }
        depth--;
      }
    }
    return -1;
  }

  private static int findSeparator(String content) {
    int depth = 0;
    for (int i = 0; i < content.length(); i++) {
      char ch = content.charAt(i);
      if (ch == '{') {
        depth++;
      } else if (ch == '}') {
        depth--;
      } else if (ch == SEPARATOR && depth == 0) {
        return i;
      }
    }
    return -1;
  }
}
