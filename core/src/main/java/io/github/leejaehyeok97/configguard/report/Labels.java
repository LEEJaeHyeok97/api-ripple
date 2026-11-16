package io.github.leejaehyeok97.configguard.report;

import io.github.leejaehyeok97.configguard.model.Finding;
import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.model.Rule;
import io.github.leejaehyeok97.configguard.model.Severity;
import io.github.leejaehyeok97.configguard.model.Summary;

/** 렌더러들이 공유하는 표시 규칙. */
final class Labels {

  private Labels() {}

  static String severity(Severity severity) {
    return switch (severity) {
      case ERROR -> "ERROR";
      case WARNING -> "WARN";
      case INFO -> "INFO";
    };
  }

  /** CG002는 키 자리에 환경변수 이름이 오므로 앞에 {@code env}를 붙인다. */
  static String key(Finding finding) {
    if (finding.ruleId().equals(Rule.MISSING_ENV_VAR.id())) {
      return "env " + finding.key();
    }
    return finding.key().isEmpty() ? "-" : finding.key();
  }

  static String profileTitle(String profile) {
    return profile.equals(Report.ALL_PROFILES) ? "모든 프로필 공통" : profile;
  }

  /** {@code 2 errors, 1 warning, 1 info} */
  static String summary(Summary summary) {
    return plural(summary.errors(), "error")
        + ", "
        + plural(summary.warnings(), "warning")
        + ", "
        + summary.infos()
        + " info";
  }

  private static String plural(int count, String word) {
    return count + " " + word + (count == 1 ? "" : "s");
  }
}
