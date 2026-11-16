package io.github.leejaehyeok97.configguard.model;

import java.util.List;

/** 검사한 프로필과 심각도별 개수. */
public record Summary(List<String> profiles, int errors, int warnings, int infos) {

  public Summary {
    profiles = List.copyOf(profiles);
  }

  public static Summary of(List<String> profiles, List<Finding> findings) {
    int errors = 0;
    int warnings = 0;
    int infos = 0;
    for (Finding finding : findings) {
      switch (finding.severity()) {
        case ERROR -> errors++;
        case WARNING -> warnings++;
        case INFO -> infos++;
      }
    }
    return new Summary(profiles, errors, warnings, infos);
  }
}
