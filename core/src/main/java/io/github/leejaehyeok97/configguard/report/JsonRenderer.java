package io.github.leejaehyeok97.configguard.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.leejaehyeok97.configguard.model.Finding;
import io.github.leejaehyeok97.configguard.model.Notice;
import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.model.Rule;
import io.github.leejaehyeok97.configguard.model.SourceLocation;
import io.github.leejaehyeok97.configguard.model.Suggestion;
import java.io.UncheckedIOException;

/**
 * 에이전트용 JSON. 형식은 {@code schema/report.schema.json}(schemaVersion {@value Report#SCHEMA_VERSION})으로
 * 고정한다. 같은 입력이면 바이트 단위로 같은 출력이 나온다 (시각·절대 경로 없음, 줄바꿈 LF).
 */
public final class JsonRenderer {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JsonRenderer() {}

  public static String render(Report report) {
    ObjectNode root = MAPPER.createObjectNode();
    root.put("schemaVersion", Report.SCHEMA_VERSION);
    ObjectNode summary = root.putObject("summary");
    ArrayNode profiles = summary.putArray("profiles");
    report.summary().profiles().forEach(profiles::add);
    summary.put("errors", report.summary().errors());
    summary.put("warnings", report.summary().warnings());
    summary.put("infos", report.summary().infos());
    ArrayNode findings = root.putArray("findings");
    for (Finding finding : report.findings()) {
      findings.add(finding(finding));
    }
    ArrayNode notices = root.putArray("notices");
    for (Notice notice : report.notices()) {
      ObjectNode node = notices.addObject();
      node.put("code", notice.code());
      node.put("profile", notice.profile());
      node.put("message", notice.message());
    }
    DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
    DefaultPrettyPrinter printer =
        new DefaultPrettyPrinter(
                Separators.createDefaultInstance()
                    .withObjectFieldValueSpacing(Separators.Spacing.AFTER))
            .withObjectIndenter(indenter)
            .withArrayIndenter(indenter);
    try {
      return MAPPER.writer(printer).writeValueAsString(root) + "\n";
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static ObjectNode finding(Finding finding) {
    ObjectNode node = MAPPER.createObjectNode();
    node.put("ruleId", finding.ruleId());
    node.put("rule", Rule.byId(finding.ruleId()).map(Rule::name).orElse(""));
    node.put("severity", finding.severity().name());
    node.put("profile", finding.profile());
    node.put("key", finding.key());
    SourceLocation location = finding.location();
    if (location == null) {
      node.putNull("location");
    } else {
      ObjectNode loc = node.putObject("location");
      loc.put("file", location.file());
      loc.put("line", location.line());
    }
    node.put("message", finding.message());
    ArrayNode suggestions = node.putArray("suggestions");
    for (Suggestion suggestion : finding.suggestions()) {
      ObjectNode s = suggestions.addObject();
      s.put("kind", suggestion.kind().name());
      if (suggestion.file() == null) {
        s.putNull("file");
      } else {
        s.put("file", suggestion.file());
      }
      s.put("target", suggestion.target());
    }
    return node;
  }
}
