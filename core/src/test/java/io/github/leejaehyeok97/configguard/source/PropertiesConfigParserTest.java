package io.github.leejaehyeok97.configguard.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropertiesConfigParserTest {

  @TempDir Path dir;

  private List<ConfigDocument> parse(String content) throws Exception {
    Path file = dir.resolve("application-prod.properties");
    Files.writeString(file, content);
    return PropertiesConfigParser.parse(file, "application-prod.properties", "prod");
  }

  @Test
  void readsKeysWithAllSeparatorsAndLineNumbers() throws Exception {
    var doc =
        parse(
                """
                # comment
                ! also comment

                a.equals=1
                a.colon:2
                a.space 3
                  a.indented = 4
                a.empty=
                a.continued=first \\
                    second
                a.after=5
                """)
            .get(0);

    assertThat(doc.fileProfile()).isEqualTo("prod");
    assertThat(doc.entries())
        .extracting(ConfigEntry::canonicalKey)
        .containsExactly(
            "a.equals", "a.colon", "a.space", "a.indented", "a.empty", "a.continued", "a.after");
    assertThat(doc.entries())
        .extracting(e -> e.location().line())
        .containsExactly(4, 5, 6, 7, 8, 9, 11);
  }

  @Test
  void unescapesKeysAndValues() throws Exception {
    var doc =
        parse(
                """
                key\\ with\\:colon=x
                a.url=${DB\\u005fURL}
                a.tab=\\t\\n\\r\\f\\q
                a.bad=\\uZZZZ
                a.short=\\u12
                a.trailing=x\\\\
                """)
            .get(0);

    assertThat(doc.entries())
        .extracting(ConfigEntry::canonicalKey)
        .startsWith("key-with:colon", "a.url");
    assertThat(doc.entries().get(1).requiredPlaceholders()).containsExactly("DB_URL");
    assertThat(doc.entries()).hasSize(6);
  }

  @Test
  void splitsDocumentsOnSpringSeparator() throws Exception {
    var docs =
        parse(
            """
            a=1
            #---
            spring.config.activate.on-profile=eu
            b=2
            !---
            c=3
            """);

    assertThat(docs).hasSize(3);
    assertThat(docs.get(1).onProfiles()).containsExactly("eu");
    assertThat(docs.get(1).line()).isEqualTo(3);
    assertThat(docs.get(2).entries()).extracting(ConfigEntry::canonicalKey).containsExactly("c");
  }

  @Test
  void unescapeLeavesPlainTextAlone() {
    assertThat(PropertiesConfigParser.unescape("plain")).isEqualTo("plain");
    assertThat(PropertiesConfigParser.unescape("end\\")).isEqualTo("end\\");
  }
}
