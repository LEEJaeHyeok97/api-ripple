package io.github.leejaehyeok97.configguard.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * 환경변수 이름 목록을 읽는다. 두 형식을 한 파일 안에서 섞어 써도 된다.
 *
 * <ul>
 *   <li>.env 형식: {@code KEY=value}. 이름만 취하고 값은 즉시 버린다.
 *   <li>이름 목록: 한 줄에 하나 ({@code DB_URL})
 * </ul>
 *
 * <p>{@code #} 주석, 빈 줄, {@code export } 접두어를 허용한다.
 */
public final class EnvFileReader {

  private EnvFileReader() {}

  public static Set<String> readNames(Path file) throws IOException {
    Set<String> names = new LinkedHashSet<>();
    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      String name = nameOf(line);
      if (name != null) {
        names.add(name);
      }
    }
    return names;
  }

  static @Nullable String nameOf(String rawLine) {
    String line = rawLine.strip();
    if (line.isEmpty() || line.startsWith("#")) {
      return null;
    }
    if (line.startsWith("export ")) {
      line = line.substring("export ".length()).strip();
    }
    int end = 0;
    while (end < line.length() && line.charAt(end) != '=' && !isSpace(line.charAt(end))) {
      end++;
    }
    // 이름 뒤(= 이후의 값, 줄 끝 주석)는 여기서 버려지고 어디에도 전달되지 않는다.
    return end == 0 ? null : line.substring(0, end);
  }

  private static boolean isSpace(char ch) {
    return Character.isWhitespace(ch);
  }
}
