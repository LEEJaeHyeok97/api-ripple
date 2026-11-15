package io.github.leejaehyeok97.configguard.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.leejaehyeok97.configguard.Fixtures;
import io.github.leejaehyeok97.configguard.model.ProvidedKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigSourcesTest {

  private static ConfigSources load(String fixture) throws Exception {
    Path project = Fixtures.path(fixture);
    return ConfigSources.load(project, List.of(project.resolve("src/main/resources")));
  }

  @Test
  void prodMergesDefaultProfileDocumentsAndGroupMembers() throws Exception {
    EffectiveConfig prod = load("profiles").effective("prod");

    assertThat(prod.activeProfiles()).containsExactly("prod", "prod-db", "prod-mq");
    assertThat(prod.find("app.feature.timeout"))
        .get()
        .satisfies(
            k -> {
              assertThat(k.profile()).isEqualTo("prod");
              assertThat(k.location().toString())
                  .isEqualTo("src/main/resources/application.yml:16");
            });
    assertThat(prod.find("app.region")).isPresent();
    assertThat(prod.find("payment.apiKey"))
        .get()
        .extracting(ProvidedKey::requiredEnvVars)
        .isEqualTo(List.of("PAYMENT_API_KEY"));
    assertThat(prod.find("db.pool.max-size"))
        .get()
        .extracting(ProvidedKey::profile)
        .isEqualTo("prod-db");
    assertThat(prod.find("servers[1].host")).isPresent();
    assertThat(prod.containsSelfOrDescendant("servers")).isTrue();
    assertThat(prod.find("app.debug-mode")).isEmpty();
    assertThat(prod.find("app.never-analyzed")).isEmpty();
  }

  @Test
  void configDirectoryOverridesRootLocation() throws Exception {
    EffectiveConfig prod = load("profiles").effective("prod");

    assertThat(prod.find("app.name"))
        .get()
        .satisfies(
            k -> {
              assertThat(k.profile()).isEqualTo(ProvidedKey.DEFAULT_PROFILE);
              assertThat(k.location().file())
                  .isEqualTo("src/main/resources/config/application.properties");
            });
  }

  @Test
  void devFollowsIncludeOneLevelAndProfileFileOverridesDefault() throws Exception {
    EffectiveConfig dev = load("profiles").effective("dev");

    assertThat(dev.activeProfiles()).containsExactly("common", "dev");
    assertThat(dev.find("app.feature.timeout"))
        .get()
        .extracting(k -> k.location().file())
        .isEqualTo("src/main/resources/application-dev.properties");
    assertThat(dev.find("app.debug-mode")).get().extracting(ProvidedKey::profile).isEqualTo("dev");
    assertThat(dev.find("common.tracing")).isPresent();
    assertThat(dev.find("payment.api-key")).isEmpty();
  }

  @Test
  void defaultProfileSeesOnlyUnconditionalDocuments() throws Exception {
    EffectiveConfig defaults = load("profiles").effective("default");

    assertThat(defaults.find("app.feature.timeout"))
        .get()
        .extracting(k -> k.location().line())
        .isEqualTo(4);
    assertThat(defaults.find("app.region")).isEmpty();
  }

  @Test
  void reportsUnsupportedProfileExpressionAsUnanalyzable() throws Exception {
    var unanalyzable = load("profiles").unanalyzable();

    assertThat(unanalyzable).hasSize(1);
    assertThat(unanalyzable.get(0).location().toString())
        .isEqualTo("src/main/resources/application.yml:26");
  }

  @Test
  void tracksPlaceholderEnvVarsPerKey() throws Exception {
    EffectiveConfig config = load("env-placeholder").effective("prod");

    assertThat(config.find("spring.datasource.url").orElseThrow().requiredEnvVars())
        .containsExactly("DB_URL");
    assertThat(config.find("spring.datasource.username").orElseThrow().requiredEnvVars()).isEmpty();
    assertThat(config.find("mail.sender").orElseThrow().requiredEnvVars())
        .containsExactly("MAIL_DOMAIN");
    assertThat(config.find("cache.ttl").orElseThrow().requiredEnvVars())
        .containsExactly("DEFAULT_TTL");
    assertThat(config.referencedPlaceholders())
        .containsExactlyInAnyOrder(
            "DB_URL", "DB_USER", "REDIS_HOST", "MAIL_DOMAIN", "CACHE_TTL", "DEFAULT_TTL");
  }

  @Test
  void missingResourceDirectoryYieldsNoKeys(@TempDir Path dir) throws Exception {
    var sources = ConfigSources.load(dir, List.of(dir.resolve("src/main/resources")));

    assertThat(sources.effective("prod").keys()).isEmpty();
    assertThat(sources.unanalyzable()).isEmpty();
  }

  @Test
  void yamlErrorIsReportedWithRelativePath(@TempDir Path dir) throws Exception {
    Path resources = Files.createDirectories(dir.resolve("src/main/resources"));
    Files.writeString(resources.resolve("application.yml"), "a: [\n");
    Files.writeString(resources.resolve("notes.yml"), "ignored: true\n");

    assertThatThrownBy(() -> ConfigSources.load(dir, List.of(resources)))
        .isInstanceOf(ConfigSourceException.class)
        .hasMessageStartingWith("src/main/resources/application.yml:");
  }
}
