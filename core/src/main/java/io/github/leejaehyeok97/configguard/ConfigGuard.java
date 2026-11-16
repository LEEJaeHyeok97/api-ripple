package io.github.leejaehyeok97.configguard;

import io.github.leejaehyeok97.configguard.compare.CompareOptions;
import io.github.leejaehyeok97.configguard.compare.ConfigComparator;
import io.github.leejaehyeok97.configguard.extract.ExtractionResult;
import io.github.leejaehyeok97.configguard.extract.KeyExtractor;
import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.source.ConfigSourceException;
import io.github.leejaehyeok97.configguard.source.ConfigSources;
import io.github.leejaehyeok97.configguard.source.EnvFileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** core의 진입점. 추출 → 소스 수집 → 비교를 한 번에 한다. CLI와 Gradle 플러그인이 이것만 부른다. */
public final class ConfigGuard {

  private ConfigGuard() {}

  public static Report analyze(AnalysisRequest request) throws IOException, ConfigSourceException {
    ExtractionResult code = KeyExtractor.extract(request.projectDir(), request.javaRoots());
    ConfigSources sources = ConfigSources.load(request.projectDir(), request.resourceRoots());
    Map<String, Set<String>> envVars = new LinkedHashMap<>();
    for (var entry : request.envFiles().entrySet()) {
      envVars.put(entry.getKey(), EnvFileReader.readNames(entry.getValue()));
    }
    CompareOptions options =
        new CompareOptions(
            request.profiles(), envVars, request.ignoreKeys(), request.extraFrameworkPrefixes());
    return ConfigComparator.compare(code, sources, options);
  }
}
