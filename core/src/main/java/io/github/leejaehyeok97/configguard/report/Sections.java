package io.github.leejaehyeok97.configguard.report;

import io.github.leejaehyeok97.configguard.model.Finding;
import io.github.leejaehyeok97.configguard.model.Notice;
import io.github.leejaehyeok97.configguard.model.Report;
import java.util.ArrayList;
import java.util.List;

/** 리포트를 프로필 섹션으로 나눈다. 지정한 프로필 순서, 그다음 공통({@code *}) 섹션(내용이 있을 때만). */
final class Sections {

  private Sections() {}

  static List<String> profiles(Report report) {
    List<String> profiles = new ArrayList<>(report.summary().profiles());
    if (report.findings().stream().anyMatch(f -> f.profile().equals(Report.ALL_PROFILES))) {
      profiles.add(Report.ALL_PROFILES);
    }
    return profiles;
  }

  static List<Finding> findings(Report report, String profile) {
    return report.findings().stream().filter(f -> f.profile().equals(profile)).toList();
  }

  static List<Notice> notices(Report report, String profile) {
    return report.notices().stream().filter(n -> n.profile().equals(profile)).toList();
  }
}
