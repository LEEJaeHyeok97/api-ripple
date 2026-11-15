package io.github.leejaehyeok97.configguard.source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * 리소스 디렉터리에서 {@code application*.yml|yaml|properties}를 찾는다. Spring Boot 기본 위치인 {@code classpath:/}와
 * {@code classpath:/config/}를 본다.
 *
 * <p>순서는 Spring의 우선순위와 같다. 뒤에 오는 파일이 앞의 값을 덮어쓴다: 루트 → config/, 같은 위치에서는 yml → yaml → properties.
 */
final class ConfigFileScanner {

  private static final Pattern NAME =
      Pattern.compile("application(?:-([^.]+))?\\.(yml|yaml|properties)");
  private static final List<String> EXTENSION_ORDER = List.of("yml", "yaml", "properties");

  /**
   * @param profile 파일 이름의 프로필. {@code application.yml}이면 null.
   */
  record ConfigFile(Path path, @Nullable String profile, boolean yaml) {}

  private ConfigFileScanner() {}

  static List<ConfigFile> scan(List<Path> resourceRoots) throws IOException {
    List<ConfigFile> files = new ArrayList<>();
    for (Path root : resourceRoots) {
      files.addAll(scanDirectory(root));
      files.addAll(scanDirectory(root.resolve("config")));
    }
    return files;
  }

  private static List<ConfigFile> scanDirectory(Path dir) throws IOException {
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    List<ConfigFile> files = new ArrayList<>();
    try (Stream<Path> children = Files.list(dir)) {
      for (Path path : children.sorted().toList()) {
        Matcher m = NAME.matcher(path.getFileName().toString());
        if (Files.isRegularFile(path) && m.matches()) {
          files.add(new ConfigFile(path, m.group(1), !m.group(2).equals("properties")));
        }
      }
    }
    files.sort(
        Comparator.comparingInt(
            (ConfigFile f) ->
                EXTENSION_ORDER.indexOf(extension(f.path().getFileName().toString()))));
    return files;
  }

  private static String extension(String name) {
    return name.substring(name.lastIndexOf('.') + 1);
  }
}
