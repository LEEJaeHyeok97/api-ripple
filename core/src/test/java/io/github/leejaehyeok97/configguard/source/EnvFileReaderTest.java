package io.github.leejaehyeok97.configguard.source;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.leejaehyeok97.configguard.Fixtures;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvFileReaderTest {

  @Test
  void readsNameListFormat() throws Exception {
    Path file = Fixtures.path("env-placeholder").resolve("deploy/prod.env.keys");

    assertThat(EnvFileReader.readNames(file)).containsExactly("DB_URL", "MAIL_DOMAIN");
  }

  @Test
  void readsDotenvFormatAndDropsValues() throws Exception {
    Path file = Fixtures.path("env-placeholder").resolve("deploy/dev.env");

    assertThat(EnvFileReader.readNames(file))
        .containsExactly("DB_URL", "MAIL_DOMAIN", "DEFAULT_TTL");
  }

  @Test
  void mixedFormatsCommentsAndSecretValues(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("prod.env");
    Files.writeString(
        file,
        """
        # comment
        API_KEY=SUPER_SECRET_VALUE_123
          SPACED = value
        NAME_ONLY # trailing comment
        export EXPORTED=1
        =no-name

        """);

    var names = EnvFileReader.readNames(file);

    assertThat(names).containsExactly("API_KEY", "SPACED", "NAME_ONLY", "EXPORTED");
    assertThat(names.toString()).doesNotContain("SUPER_SECRET_VALUE_123");
  }
}
