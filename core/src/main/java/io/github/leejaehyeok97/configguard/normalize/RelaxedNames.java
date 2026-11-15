package io.github.leejaehyeok97.configguard.normalize;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Spring Boot relaxed binding 규칙에 따른 키 정규화.
 *
 * <ul>
 *   <li>정규형(canonical): 소문자 + kebab-case + 점 구분. {@code payment.apiKey} → {@code payment.api-key}
 *   <li>비교 키(match key): 정규형에서 대시를 제거한 형태. Spring이 키를 비교할 때 대시를 무시하는 것과 같다.
 *   <li>환경변수: {@code _}는 {@code .}, 숫자 세그먼트는 리스트 인덱스. {@code MY_LIST_0_NAME} → {@code
 *       my.list[0].name}
 * </ul>
 *
 * 리스트/맵 인덱스({@code [0]}, {@code [key]}) 안의 내용은 그대로 둔다.
 */
public final class RelaxedNames {

  private RelaxedNames() {}

  /** 설정 파일 키, @Value 플레이스홀더 이름, 필드 이름 경로를 정규형으로 바꾼다. */
  public static String canonical(String key) {
    List<String> elements = splitElements(key.strip());
    StringBuilder out = new StringBuilder();
    for (String element : elements) {
      if (element.isEmpty()) {
        continue;
      }
      if (element.startsWith("[")) {
        out.append(element);
        continue;
      }
      if (!out.isEmpty()) {
        out.append('.');
      }
      out.append(dashed(element));
    }
    return out.toString();
  }

  /** 비교용 키. 정규형으로 바꾼 뒤 인덱스 밖의 대시를 제거한다. */
  public static String matchKey(String key) {
    String canonical = canonical(key);
    StringBuilder out = new StringBuilder(canonical.length());
    boolean inIndex = false;
    for (int i = 0; i < canonical.length(); i++) {
      char ch = canonical.charAt(i);
      if (ch == '[') {
        inIndex = true;
      } else if (ch == ']') {
        inIndex = false;
      }
      if (ch == '-' && !inIndex) {
        continue;
      }
      out.append(ch);
    }
    return out.toString();
  }

  /** {@code _}로 나눈 비어 있지 않은 조각들. {@code DB__URL} → [DB, URL]. */
  private static List<String> envSegments(String envName) {
    List<String> segments = new ArrayList<>();
    int start = 0;
    for (int i = 0; i <= envName.length(); i++) {
      if (i == envName.length() || envName.charAt(i) == '_') {
        if (i > start) {
          segments.add(envName.substring(start, i));
        }
        start = i + 1;
      }
    }
    return segments;
  }

  /** 환경변수 이름을 설정 키 정규형으로 바꾼다. {@code PAYMENT_APIKEY} → {@code payment.apikey}. */
  public static String fromEnvVar(String envName) {
    StringBuilder out = new StringBuilder();
    for (String segment : envSegments(envName.strip())) {
      String lower = segment.toLowerCase(Locale.ROOT);
      if (isNumber(lower) && !out.isEmpty()) {
        out.append('[').append(lower).append(']');
      } else {
        if (!out.isEmpty()) {
          out.append('.');
        }
        out.append(lower);
      }
    }
    return out.toString();
  }

  /**
   * 설정 키를 제공할 수 있는 환경변수 이름 후보. Spring의 SystemEnvironmentPropertyMapper와 같이 대시를 제거한 이름({@code
   * PAYMENT_APIKEY})과 대시를 밑줄로 바꾼 레거시 이름({@code PAYMENT_API_KEY})을 돌려준다.
   */
  public static Set<String> envVarCandidates(String key) {
    String canonical = canonical(key);
    Set<String> result = new LinkedHashSet<>();
    result.add(toEnv(canonical, false));
    result.add(toEnv(canonical, true));
    return result;
  }

  /** {@code ancestor}가 {@code key}와 같거나 그 상위 키인지 (비교 키 기준). */
  public static boolean isSelfOrAncestor(String ancestor, String key) {
    String a = matchKey(ancestor);
    String k = matchKey(key);
    if (a.isEmpty()) {
      return false;
    }
    return k.equals(a) || k.startsWith(a + ".") || k.startsWith(a + "[");
  }

  private static String toEnv(String canonical, boolean legacy) {
    StringBuilder out = new StringBuilder();
    for (String element : splitElements(canonical)) {
      if (element.isEmpty()) {
        continue;
      }
      String name = element;
      if (name.startsWith("[")) {
        name = name.substring(1, name.endsWith("]") ? name.length() - 1 : name.length());
      }
      name = legacy ? name.replace('-', '_') : name.replace("-", "");
      if (!out.isEmpty()) {
        out.append('_');
      }
      out.append(name.replace('.', '_').toUpperCase(Locale.ROOT));
    }
    return out.toString();
  }

  /** {@code a.b[0].c} → [a, b, [0], c]. 인덱스 안의 점은 나누지 않는다. */
  static List<String> splitElements(String key) {
    List<String> elements = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < key.length(); i++) {
      char ch = key.charAt(i);
      if (ch == '[') {
        elements.add(current.toString());
        current.setLength(0);
        int close = key.indexOf(']', i);
        if (close < 0) {
          close = key.length() - 1;
        }
        elements.add(key.substring(i, close + 1));
        i = close;
      } else if (ch == '.') {
        elements.add(current.toString());
        current.setLength(0);
      } else {
        current.append(ch);
      }
    }
    elements.add(current.toString());
    return elements;
  }

  /** 원소 하나를 kebab-case 소문자로. camelCase는 대시로 나누고, 밑줄은 대시로 바꾼다. */
  private static String dashed(String element) {
    boolean hasLower = element.chars().anyMatch(Character::isLowerCase);
    StringBuilder out = new StringBuilder(element.length() + 4);
    for (int i = 0; i < element.length(); i++) {
      char ch = element.charAt(i);
      if (ch == '_' || ch == '-' || Character.isWhitespace(ch)) {
        appendDash(out);
        continue;
      }
      if (hasLower && Character.isUpperCase(ch) && i > 0) {
        char prev = element.charAt(i - 1);
        boolean nextLower =
            i + 1 < element.length() && Character.isLowerCase(element.charAt(i + 1));
        if (Character.isLowerCase(prev)
            || Character.isDigit(prev)
            || (Character.isUpperCase(prev) && nextLower)) {
          appendDash(out);
        }
      }
      out.append(Character.toLowerCase(ch));
    }
    int end = out.length();
    while (end > 0 && out.charAt(end - 1) == '-') {
      end--;
    }
    int start = 0;
    while (start < end && out.charAt(start) == '-') {
      start++;
    }
    return out.substring(start, end);
  }

  private static void appendDash(StringBuilder out) {
    if (!out.isEmpty() && out.charAt(out.length() - 1) != '-') {
      out.append('-');
    }
  }

  private static boolean isNumber(String s) {
    if (s.isEmpty()) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
