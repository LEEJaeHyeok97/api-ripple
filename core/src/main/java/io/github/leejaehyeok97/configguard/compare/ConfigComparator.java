package io.github.leejaehyeok97.configguard.compare;

import io.github.leejaehyeok97.configguard.extract.ExtractionResult;
import io.github.leejaehyeok97.configguard.model.Finding;
import io.github.leejaehyeok97.configguard.model.Notice;
import io.github.leejaehyeok97.configguard.model.ProvidedKey;
import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.model.RequiredKey;
import io.github.leejaehyeok97.configguard.model.Requirement;
import io.github.leejaehyeok97.configguard.model.Rule;
import io.github.leejaehyeok97.configguard.model.Severity;
import io.github.leejaehyeok97.configguard.model.Summary;
import io.github.leejaehyeok97.configguard.model.Unanalyzable;
import io.github.leejaehyeok97.configguard.normalize.RelaxedNames;
import io.github.leejaehyeok97.configguard.source.ConfigSources;
import io.github.leejaehyeok97.configguard.source.EffectiveConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** 프로필마다 필요한 키와 제공되는 키를 비교한다 (명세 6.5). */
public final class ConfigComparator {

  private final ExtractionResult code;
  private final ConfigSources sources;
  private final CompareOptions options;
  private final KeyFilters filters;
  private final Collection<RequiredKey> required;

  private ConfigComparator(ExtractionResult code, ConfigSources sources, CompareOptions options) {
    this.code = code;
    this.sources = sources;
    this.options = options;
    this.filters = new KeyFilters(options.ignoreKeys(), options.extraFrameworkPrefixes());
    this.required = strongestPerKey(code.keys());
  }

  public static Report compare(
      ExtractionResult code, ConfigSources sources, CompareOptions options) {
    return new ConfigComparator(code, sources, options).run();
  }

  private Report run() {
    List<Finding> findings = new ArrayList<>();
    List<Notice> notices = new ArrayList<>();
    Map<String, ProvidedKey> unusedDefaults = new LinkedHashMap<>();
    for (String profile : options.profiles()) {
      EffectiveConfig config = sources.effective(profile);
      Set<String> env = options.envVarsByProfile().get(profile);
      if (env == null) {
        notices.add(
            new Notice(Notice.ENV_CHECK_SKIPPED, profile, "환경변수 목록 파일이 지정되지 않아 환경변수 검사를 건너뜀"));
      }
      missingKeys(profile, config, env, findings);
      if (env != null) {
        missingEnvVars(profile, config, env, findings);
      }
      unusedKeys(profile, config, findings, unusedDefaults);
    }
    for (ProvidedKey key : unusedDefaults.values()) {
      findings.add(unused(Report.ALL_PROFILES, key));
    }
    List<Unanalyzable> unanalyzable = new ArrayList<>(code.unanalyzable());
    unanalyzable.addAll(sources.unanalyzable());
    for (Unanalyzable u : unanalyzable) {
      findings.add(
          new Finding(
              Rule.UNANALYZABLE.id(),
              Severity.INFO,
              Report.ALL_PROFILES,
              u.key(),
              u.location(),
              u.message(),
              List.of()));
    }
    findings.sort(order());
    return new Report(Summary.of(options.profiles(), findings), findings, notices);
  }

  /** CG001(REQUIRED 누락), CG003(CONDITIONAL 누락). */
  private void missingKeys(
      String profile, EffectiveConfig config, @Nullable Set<String> env, List<Finding> out) {
    for (RequiredKey key : required) {
      if (key.requirement() == Requirement.OPTIONAL || filters.isIgnored(key.canonicalKey())) {
        continue;
      }
      if (config.find(key.canonicalKey()).isPresent() || providedByEnv(key.canonicalKey(), env)) {
        continue;
      }
      String file = sources.suggestedFile(profile);
      if (key.requirement() == Requirement.REQUIRED) {
        out.add(
            new Finding(
                Rule.MISSING_KEY.id(),
                Severity.ERROR,
                profile,
                key.canonicalKey(),
                key.location(),
                key.reason(),
                Suggestions.forMissingKey(file, key.canonicalKey())));
      } else {
        out.add(
            new Finding(
                Rule.CONDITIONAL_KEY.id(),
                Severity.WARNING,
                profile,
                key.canonicalKey(),
                key.location(),
                key.reason(),
                Suggestions.forConditionalKey(file, key.canonicalKey())));
      }
    }
  }

  /** CG002: 값의 ${ENV}가 환경변수 목록에도, 다른 설정 키로도 없다. */
  private void missingEnvVars(
      String profile, EffectiveConfig config, Set<String> env, List<Finding> out) {
    for (ProvidedKey provided : sortedKeys(config)) {
      if (filters.isIgnored(provided.canonicalKey())) {
        continue;
      }
      for (String name : provided.requiredEnvVars()) {
        boolean satisfied = env.contains(name) || config.find(name).isPresent();
        if (!satisfied) {
          out.add(
              new Finding(
                  Rule.MISSING_ENV_VAR.id(),
                  Severity.ERROR,
                  profile,
                  name,
                  provided.location(),
                  provided.canonicalKey() + "이 요구",
                  Suggestions.forMissingEnvVar(name)));
        }
      }
    }
  }

  /** CG004. 기본 파일의 키는 프로필과 무관하므로 모아 두었다가 공통({@code *})으로 한 번만 보고한다. */
  private void unusedKeys(
      String profile,
      EffectiveConfig config,
      List<Finding> out,
      Map<String, ProvidedKey> unusedDefaults) {
    Set<String> referenced = new HashSet<>();
    config.referencedPlaceholders().forEach(n -> referenced.add(RelaxedNames.matchKey(n)));
    for (ProvidedKey provided : sortedKeys(config)) {
      String key = provided.canonicalKey();
      if (filters.isIgnored(key) || filters.isFrameworkKey(key) || isUsed(key, referenced)) {
        continue;
      }
      if (provided.profile().equals(ProvidedKey.DEFAULT_PROFILE)) {
        unusedDefaults.putIfAbsent(provided.location().toString(), provided);
      } else {
        out.add(unused(profile, provided));
      }
    }
  }

  private boolean isUsed(String providedKey, Set<String> referencedPlaceholders) {
    if (referencedPlaceholders.contains(RelaxedNames.matchKey(providedKey))) {
      return true;
    }
    // 필요한 키가 제공된 키 자신이거나 그 상위(Map/List prefix, 중첩 객체)면 쓰이는 키다.
    return code.keys().stream()
        .anyMatch(r -> RelaxedNames.isSelfOrAncestor(r.canonicalKey(), providedKey));
  }

  private static Finding unused(String profile, ProvidedKey key) {
    return new Finding(
        Rule.UNUSED_KEY.id(),
        Severity.INFO,
        profile,
        key.canonicalKey(),
        key.location(),
        "사용되지 않는 키",
        List.of());
  }

  private static boolean providedByEnv(String key, @Nullable Set<String> env) {
    if (env == null) {
      return false;
    }
    return RelaxedNames.envVarCandidates(key).stream().anyMatch(env::contains);
  }

  private static List<ProvidedKey> sortedKeys(EffectiveConfig config) {
    return config.keys().values().stream()
        .sorted(Comparator.comparing(ProvidedKey::canonicalKey))
        .toList();
  }

  /** 같은 키를 여러 곳에서 요구하면 가장 강한 요구(REQUIRED > CONDITIONAL > OPTIONAL)의 첫 위치만 남긴다. */
  private static Collection<RequiredKey> strongestPerKey(List<RequiredKey> keys) {
    Map<String, RequiredKey> strongest = new LinkedHashMap<>();
    for (RequiredKey key : keys) {
      strongest.merge(
          RelaxedNames.matchKey(key.canonicalKey()),
          key,
          (a, b) -> strength(b.requirement()) > strength(a.requirement()) ? b : a);
    }
    return strongest.values();
  }

  private static int strength(Requirement requirement) {
    return switch (requirement) {
      case REQUIRED -> 2;
      case CONDITIONAL -> 1;
      case OPTIONAL -> 0;
    };
  }

  private Comparator<Finding> order() {
    List<String> profiles = new ArrayList<>(options.profiles());
    profiles.add(Report.ALL_PROFILES);
    return Comparator.comparingInt((Finding f) -> profiles.indexOf(f.profile()))
        .thenComparing(Finding::severity)
        .thenComparing(Finding::ruleId)
        .thenComparing(Finding::key)
        .thenComparing(f -> f.location() == null ? "" : f.location().toString());
  }
}
