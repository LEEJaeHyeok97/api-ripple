package io.github.leejaehyeok97.configguard.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * .properties 파서. 라인 번호가 필요해서 {@link java.util.Properties} 대신 직접 파싱한다. 문법(주석, 구분자, 줄 이어짐, 이스케이프)은
 * {@code java.util.Properties}를, 문서 구분({@code #---}, {@code !---})은 Spring Boot를 따른다.
 */
final class PropertiesConfigParser {

  private PropertiesConfigParser() {}

  static List<ConfigDocument> parse(Path path, String file, @Nullable String fileProfile)
      throws IOException {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    List<ConfigDocument> documents = new ArrayList<>();
    DocumentBuilder document = new DocumentBuilder(file, fileProfile, 1);
    int i = 0;
    while (i < lines.size()) {
      String line = lines.get(i);
      int lineNumber = ++i;
      String trimmed = line.strip();
      if (trimmed.equals("#---") || trimmed.equals("!---")) {
        documents.add(document.build());
        document = new DocumentBuilder(file, fileProfile, lineNumber + 1);
        continue;
      }
      if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
        continue;
      }
      StringBuilder logical = new StringBuilder(stripLeading(line));
      while (endsWithContinuation(logical) && i < lines.size()) {
        logical.setLength(logical.length() - 1);
        logical.append(stripLeading(lines.get(i++)));
      }
      String text = logical.toString();
      int keyEnd = keyEnd(text);
      document.add(
          unescape(text.substring(0, keyEnd)), lineNumber, unescape(valueOf(text, keyEnd)));
    }
    documents.add(document.build());
    return documents;
  }

  private static boolean endsWithContinuation(CharSequence s) {
    int backslashes = 0;
    for (int j = s.length() - 1; j >= 0 && s.charAt(j) == '\\'; j--) {
      backslashes++;
    }
    return backslashes % 2 == 1;
  }

  private static boolean isBlank(char ch) {
    return ch == ' ' || ch == '\t' || ch == '\f';
  }

  private static String stripLeading(String s) {
    int j = 0;
    while (j < s.length() && isBlank(s.charAt(j))) {
      j++;
    }
    return s.substring(j);
  }

  /** 키는 이스케이프되지 않은 첫 '=', ':', 공백에서 끝난다. */
  private static int keyEnd(String line) {
    for (int j = 0; j < line.length(); j++) {
      char ch = line.charAt(j);
      if (ch == '\\') {
        j++;
      } else if (ch == '=' || ch == ':' || isBlank(ch)) {
        return j;
      }
    }
    return line.length();
  }

  private static String valueOf(String line, int keyEnd) {
    int v = keyEnd;
    while (v < line.length() && isBlank(line.charAt(v))) {
      v++;
    }
    if (v < line.length() && (line.charAt(v) == '=' || line.charAt(v) == ':')) {
      v++;
    }
    while (v < line.length() && isBlank(line.charAt(v))) {
      v++;
    }
    return line.substring(v);
  }

  static String unescape(String s) {
    if (s.indexOf('\\') < 0) {
      return s;
    }
    StringBuilder out = new StringBuilder(s.length());
    for (int j = 0; j < s.length(); j++) {
      char ch = s.charAt(j);
      if (ch != '\\' || j + 1 >= s.length()) {
        out.append(ch);
        continue;
      }
      char next = s.charAt(++j);
      switch (next) {
        case 't' -> out.append('\t');
        case 'n' -> out.append('\n');
        case 'r' -> out.append('\r');
        case 'f' -> out.append('\f');
        case 'u' -> j = appendUnicode(s, j, out);
        default -> out.append(next);
      }
    }
    return out.toString();
  }

  /** {@code \}{@code uXXXX}를 해석한다. 잘못된 형식이면 'u'를 그대로 둔다. 마지막으로 읽은 위치를 돌려준다. */
  private static int appendUnicode(String s, int uIndex, StringBuilder out) {
    if (uIndex + 4 < s.length()) {
      try {
        out.append((char) Integer.parseInt(s.substring(uIndex + 1, uIndex + 5), 16));
        return uIndex + 4;
      } catch (NumberFormatException e) {
        // 아래로 진행
      }
    }
    out.append('u');
    return uIndex;
  }
}
