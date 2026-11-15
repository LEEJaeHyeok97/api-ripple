package io.github.leejaehyeok97.configguard.source;

import io.github.leejaehyeok97.configguard.model.ProvidedKey;
import io.github.leejaehyeok97.configguard.model.SourceLocation;
import io.github.leejaehyeok97.configguard.model.Unanalyzable;
import io.github.leejaehyeok97.configguard.normalize.RelaxedNames;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 프로젝트의 설정 파일 전체. 프로필별 유효 키 집합({@link EffectiveConfig})을 계산한다. */
public final class ConfigSources {

  private final List<ConfigDocument> documents;

  private ConfigSources(List<ConfigDocument> documents) {
    this.documents = List.copyOf(documents);
  }

  /**
   * 설정 파일을 모두 읽는다.
   *
   * @param projectDir 리포트에 쓰는 상대 경로의 기준
   * @param resourceRoots 보통 {@code src/main/resources}
   */
  public static ConfigSources load(Path projectDir, List<Path> resourceRoots)
      throws IOException, ConfigSourceException {
    List<ConfigDocument> documents = new ArrayList<>();
    for (ConfigFileScanner.ConfigFile file : ConfigFileScanner.scan(resourceRoots)) {
      String display = relativePath(projectDir, file.path());
      documents.addAll(
          file.yaml()
              ? YamlConfigParser.parse(file.path(), display, file.profile())
              : PropertiesConfigParser.parse(file.path(), display, file.profile()));
    }
    return new ConfigSources(documents);
  }

  /** 지원하지 않는 프로필 식을 써서 건너뛴 문서들. */
  public List<Unanalyzable> unanalyzable() {
    List<Unanalyzable> result = new ArrayList<>();
    for (ConfigDocument doc : documents) {
      if (!doc.supported()) {
        result.add(
            new Unanalyzable(
                "spring.config.activate.on-profile",
                new SourceLocation(doc.file(), doc.line()),
                "지원하지 않는 프로필 식(!, &, 괄호). 이 문서는 분석에서 제외"));
      }
    }
    return result;
  }

  /**
   * 대상 프로필로 기동했을 때의 유효 키 집합. 덮어쓰기 순서는 Spring과 같다: 프로필 없는 파일의 문서(파일 안에서는 위에서 아래로) → 프로필별 파일(활성 프로필
   * 순서).
   */
  public EffectiveConfig effective(String profile) {
    List<String> active = activeProfiles(profile);
    Map<String, ProvidedKey> keys = new HashMap<>();
    Set<String> referenced = new LinkedHashSet<>();
    List<ConfigDocument> ordered = new ArrayList<>();
    documents.stream().filter(d -> d.fileProfile() == null).forEach(ordered::add);
    for (String p : active) {
      documents.stream().filter(d -> p.equals(d.fileProfile())).forEach(ordered::add);
    }
    for (ConfigDocument doc : ordered) {
      if (!doc.appliesTo(active)) {
        continue;
      }
      String source = sourceProfile(doc, active);
      for (ConfigEntry entry : doc.entries()) {
        keys.put(
            RelaxedNames.matchKey(entry.canonicalKey()),
            new ProvidedKey(
                entry.canonicalKey(), source, entry.location(), entry.requiredPlaceholders()));
        referenced.addAll(entry.referencedPlaceholders());
      }
    }
    return new EffectiveConfig(profile, active, keys, referenced);
  }

  /**
   * 대상 프로필과 1단계 include/group. include는 대상보다 먼저, group 멤버는 대상 뒤에 온다. "default"는 활성 프로필이 없을 때의
   * Spring 기본 프로필이다.
   */
  List<String> activeProfiles(String profile) {
    List<String> base = List.of(profile);
    Set<String> includes = new LinkedHashSet<>();
    Set<String> groupMembers = new LinkedHashSet<>();
    for (ConfigDocument doc : documents) {
      if (doc.appliesTo(base)) {
        includes.addAll(doc.includes());
      }
      if (doc.supported()) {
        groupMembers.addAll(doc.groups().getOrDefault(profile, List.of()));
      }
    }
    Set<String> active = new LinkedHashSet<>(includes);
    active.remove(profile);
    active.add(profile);
    active.addAll(groupMembers);
    return List.copyOf(active);
  }

  private static String sourceProfile(ConfigDocument doc, List<String> active) {
    if (doc.fileProfile() != null) {
      return doc.fileProfile();
    }
    return doc.onProfiles().stream()
        .filter(active::contains)
        .findFirst()
        .orElse(ProvidedKey.DEFAULT_PROFILE);
  }

  private static String relativePath(Path projectDir, Path file) {
    Path base = projectDir.toAbsolutePath().normalize();
    Path target = file.toAbsolutePath().normalize();
    Path shown = target.startsWith(base) ? base.relativize(target) : target;
    return shown.toString().replace(File.separatorChar, '/');
  }
}
