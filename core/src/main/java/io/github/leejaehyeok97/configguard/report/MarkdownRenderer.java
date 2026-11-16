package io.github.leejaehyeok97.configguard.report;

import io.github.leejaehyeok97.configguard.model.Finding;
import io.github.leejaehyeok97.configguard.model.Notice;
import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.model.Severity;
import java.util.List;

/**
 * PR 코멘트용 Markdown (명세 6.6). 첫 줄의 숨은 마커로 GitHub Action이 기존 코멘트를 찾아 갱신한다. ERROR가 없는 프로필은 {@code ✅
 * prod: 문제 없음}, INFO는 {@code <details>}로 접는다.
 */
public final class MarkdownRenderer {

  /** Action이 기존 코멘트를 찾는 마커. 바꾸면 이전 코멘트를 갱신하지 못한다. */
  public static final String MARKER = "<!-- config-guard-report -->";

  private MarkdownRenderer() {}

  public static String render(Report report) {
    StringBuilder out = new StringBuilder();
    out.append(MARKER).append('\n');
    out.append("## config-guard\n\n");
    for (String profile : Sections.profiles(report)) {
      section(out, profile, Sections.findings(report, profile), Sections.notices(report, profile));
    }
    out.append("**").append(Labels.summary(report.summary())).append("**\n");
    return out.toString();
  }

  private static void section(
      StringBuilder out, String profile, List<Finding> findings, List<Notice> notices) {
    String title = Labels.profileTitle(profile);
    out.append("### ").append(title).append("\n\n");
    List<Finding> problems = findings.stream().filter(f -> f.severity() != Severity.INFO).toList();
    List<Finding> infos = findings.stream().filter(f -> f.severity() == Severity.INFO).toList();
    long errors = problems.stream().filter(f -> f.severity() == Severity.ERROR).count();
    if (!profile.equals(Report.ALL_PROFILES)) {
      out.append(errors == 0 ? "✅ " + title + ": 문제 없음" : "❌ " + title + ": ERROR " + errors + "건")
          .append("\n\n");
    }
    if (!problems.isEmpty()) {
      table(out, problems);
    }
    if (!infos.isEmpty()) {
      out.append("<details>\n<summary>INFO ").append(infos.size()).append("건</summary>\n\n");
      table(out, infos);
      out.append("</details>\n\n");
    }
    for (Notice notice : notices) {
      out.append("> ℹ️ ").append(escape(notice.message())).append("\n\n");
    }
  }

  private static void table(StringBuilder out, List<Finding> findings) {
    out.append("| 심각도 | 규칙 | 키 | 위치 | 설명 |\n");
    out.append("|---|---|---|---|---|\n");
    for (Finding f : findings) {
      out.append("| ")
          .append(Labels.severity(f.severity()))
          .append(" | ")
          .append(f.ruleId())
          .append(" | `")
          .append(escape(Labels.key(f)))
          .append("` | ")
          .append(f.location() == null ? "-" : "`" + escape(f.location().toString()) + "`")
          .append(" | ")
          .append(escape(f.message()))
          .append(" |\n");
    }
    out.append('\n');
  }

  private static String escape(String text) {
    return text.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
  }
}
