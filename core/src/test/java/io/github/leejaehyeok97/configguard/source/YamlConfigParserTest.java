package io.github.leejaehyeok97.configguard.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlConfigParserTest {

  @TempDir Path dir;

  private List<ConfigDocument> parse(String yaml) throws Exception {
    Path file = dir.resolve("application.yml");
    Files.writeString(file, yaml);
    return YamlConfigParser.parse(file, "application.yml", null);
  }

  private static List<String> keys(ConfigDocument doc) {
    return doc.entries().stream().map(ConfigEntry::canonicalKey).toList();
  }

  @Test
  void flattensNestedMappingsAndListsWithLineNumbers() throws Exception {
    var docs =
        parse(
            """
            payment:
              apiKey: x
              servers:
                - host: a
                  port: 1
                - b
            "flat.key": 1
            """);

    assertThat(docs).hasSize(1);
    assertThat(keys(docs.get(0)))
        .containsExactly(
            "payment.api-key",
            "payment.servers[0].host",
            "payment.servers[0].port",
            "payment.servers[1]",
            "flat.key");
    assertThat(docs.get(0).entries().get(0).location().line()).isEqualTo(2);
    assertThat(docs.get(0).entries().get(3).location().line()).isEqualTo(6);
  }

  @Test
  void splitsDocumentsAndReadsActivation() throws Exception {
    var docs =
        parse(
            """
            a: 1
            ---
            spring.config.activate.on-profile: prod | staging
            b: 2
            ---
            spring:
              config:
                activate:
                  on-profile: [dev, local]
              profiles:
                include: common, metrics
                group:
                  dev: dev-db
            c: 3
            """);

    assertThat(docs).hasSize(3);
    assertThat(docs.get(0).onProfiles()).isEmpty();
    assertThat(docs.get(1).onProfiles()).containsExactly("prod", "staging");
    assertThat(docs.get(1).line()).isEqualTo(3);
    assertThat(docs.get(2).onProfiles()).containsExactly("dev", "local");
    assertThat(docs.get(2).includes()).containsExactly("common", "metrics");
    assertThat(docs.get(2).groups()).containsEntry("dev", List.of("dev-db"));
  }

  @Test
  void unsupportedProfileExpressionMarksDocument() throws Exception {
    var docs =
        parse(
            """
            spring.config.activate.on-profile: "prod & cloud"
            a: 1
            """);

    assertThat(docs.get(0).supported()).isFalse();
  }

  @Test
  void handlesMergeKeysEmptyCollectionsAndIndexedKeys() throws Exception {
    var docs =
        parse(
            """
            base: &base
              timeout: 1
            service:
              <<: *base
              retries: 2
            empty-map: {}
            empty-list: []
            map:
              "[my.key]": v
            multi:
              <<: [*base]
            """);

    assertThat(keys(docs.get(0)))
        .containsExactly(
            "base.timeout",
            "service.timeout",
            "service.retries",
            "empty-list",
            "map[my.key]",
            "multi.timeout");
  }

  @Test
  void recordsPlaceholderNamesButNotValues() throws Exception {
    var docs =
        parse(
            """
            spring.datasource.url: ${DB_URL}
            mail.sender: noreply-${MAIL_DOMAIN:example.test}
            secret: SUPER_SECRET_VALUE_123
            """);

    var entries = docs.get(0).entries();
    assertThat(entries.get(0).requiredPlaceholders()).containsExactly("DB_URL");
    assertThat(entries.get(1).requiredPlaceholders()).isEmpty();
    assertThat(entries.get(1).referencedPlaceholders()).containsExactly("MAIL_DOMAIN");
    assertThat(docs.toString()).doesNotContain("SUPER_SECRET_VALUE_123", "example.test");
  }

  @Test
  void syntaxErrorReportsLocationWithoutQuotingContent() throws Exception {
    assertThatThrownBy(() -> parse("a: 1\nb: [SUPER_SECRET_VALUE_123\n"))
        .isInstanceOf(ConfigSourceException.class)
        .hasMessageStartingWith("application.yml:")
        .hasMessageNotContaining("SUPER_SECRET_VALUE_123")
        .satisfies(e -> assertThat(((ConfigSourceException) e).location().line()).isPositive());
  }

  @Test
  void emptyFileHasNoDocuments() throws Exception {
    assertThat(parse("")).isEmpty();
    assertThat(parse("# only a comment\n")).isEmpty();
  }
}
