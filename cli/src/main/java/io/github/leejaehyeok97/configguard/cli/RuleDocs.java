package io.github.leejaehyeok97.configguard.cli;

import io.github.leejaehyeok97.configguard.model.Rule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 규칙 문서. 출처는 {@code docs/reference/rules/*.md} 하나이고 빌드 때 jar에 들어간다 (명세 6.10). 문서와 CLI 설명이 어긋날 수 없다.
 */
final class RuleDocs {

  private RuleDocs() {}

  static Optional<String> markdown(Rule rule) {
    try (InputStream in = RuleDocs.class.getResourceAsStream("rules/" + rule.id() + ".md")) {
      if (in == null) {
        return Optional.empty();
      }
      return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /** "## 무엇을 잡나" 아래 첫 문단의 첫 줄. 규칙 목록 명령이 쓴다. */
  static String summary(Rule rule) {
    String doc = markdown(rule).orElse("");
    int section = doc.indexOf("## 무엇을 잡나");
    if (section < 0) {
      return "";
    }
    for (String line : doc.substring(section).split("\n", -1)) {
      String trimmed = line.strip();
      if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
        return trimmed.replace("**", "");
      }
    }
    return "";
  }
}
