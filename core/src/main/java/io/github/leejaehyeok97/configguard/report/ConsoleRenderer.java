package io.github.leejaehyeok97.configguard.report;

import io.github.leejaehyeok97.configguard.model.Finding;
import io.github.leejaehyeok97.configguard.model.Notice;
import io.github.leejaehyeok97.configguard.model.Report;
import java.util.ArrayList;
import java.util.List;

/**
 * 사람이 터미널에서 읽는 형식 (명세 6.6).
 *
 * <pre>
 * config-guard: prod
 *   ERROR  payment.api-key       PaymentClient.java:23  @Value, 기본값 없음
 * 2 errors, 1 warning, 1 info
 * </pre>
 */
public final class ConsoleRenderer {

  private ConsoleRenderer() {}

  public static String render(Report report) {
    StringBuilder out = new StringBuilder();
    for (String profile : Sections.profiles(report)) {
      List<Finding> findings = Sections.findings(report, profile);
      List<Notice> notices = Sections.notices(report, profile);
      out.append("config-guard: ").append(Labels.profileTitle(profile)).append('\n');
      if (findings.isEmpty() && notices.isEmpty()) {
        out.append("  문제 없음\n");
      }
      int keyWidth = findings.stream().mapToInt(f -> Labels.key(f).length()).max().orElse(0) + 4;
      for (Finding finding : findings) {
        List<String> columns = new ArrayList<>();
        columns.add(pad(Labels.severity(finding.severity()), 5));
        columns.add(pad(Labels.key(finding), keyWidth - 2));
        columns.add(finding.location() == null ? "-" : shortLocation(finding));
        columns.add(finding.message());
        out.append("  ").append(String.join("  ", columns)).append('\n');
      }
      for (Notice notice : notices) {
        out.append("  NOTE   ").append(notice.message()).append('\n');
      }
    }
    out.append(Labels.summary(report.summary())).append('\n');
    return out.toString();
  }

  private static String shortLocation(Finding finding) {
    var location = finding.location();
    if (location == null) {
      return "-";
    }
    return location.line() > 0 ? location.fileName() + ":" + location.line() : location.fileName();
  }

  private static String pad(String text, int width) {
    StringBuilder padded = new StringBuilder(text);
    while (padded.length() < width) {
      padded.append(' ');
    }
    return padded.toString();
  }
}
