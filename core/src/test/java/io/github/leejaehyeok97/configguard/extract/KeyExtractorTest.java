package io.github.leejaehyeok97.configguard.extract;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.leejaehyeok97.configguard.Fixtures;
import io.github.leejaehyeok97.configguard.model.RequiredKey;
import io.github.leejaehyeok97.configguard.model.Unanalyzable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeyExtractorTest {

  private static ExtractionResult extract(String fixture) throws Exception {
    Path project = Fixtures.path(fixture);
    return KeyExtractor.extract(project, List.of(project.resolve("src/main/java")));
  }

  /** "키 요구수준" 목록. 같은 키가 여러 번 나오면 모두 남긴다. */
  private static List<String> keys(ExtractionResult result) {
    return result.keys().stream().map(k -> k.canonicalKey() + " " + k.requirement()).toList();
  }

  private static RequiredKey key(ExtractionResult result, String canonicalKey) {
    return result.keys().stream()
        .filter(k -> k.canonicalKey().equals(canonicalKey))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void valueBasic() throws Exception {
    ExtractionResult result = extract("value-basic");

    assertThat(keys(result))
        .containsExactlyInAnyOrder(
            "payment.api-key REQUIRED",
            "payment.timeout OPTIONAL",
            "payment.region OPTIONAL",
            "payment.url OPTIONAL",
            "payment.fallback-url REQUIRED",
            "payment.retry OPTIONAL",
            "payment.default-retry OPTIONAL",
            "payment.host REQUIRED",
            "payment.version OPTIONAL",
            "payment.merchant-id REQUIRED");
    RequiredKey apiKey = key(result, "payment.api-key");
    assertThat(apiKey.reason()).isEqualTo("@Value, 기본값 없음");
    assertThat(apiKey.location().toString())
        .isEqualTo("src/main/java/com/example/PaymentClient.java:9");
    assertThat(result.unanalyzable()).isEmpty();
  }

  @Test
  void configurationProperties() throws Exception {
    ExtractionResult result = extract("config-properties");

    assertThat(keys(result))
        .containsExactlyInAnyOrder(
            "app.mail.sender-address REQUIRED",
            "app.mail.timeout OPTIONAL",
            "app.mail.reply-to OPTIONAL",
            "app.mail.max-retries OPTIONAL",
            "app.mail.smtp.host OPTIONAL",
            "app.mail.smtp.port OPTIONAL",
            "app.mail.auth.username REQUIRED",
            "app.mail.headers OPTIONAL",
            "app.mail.recipients OPTIONAL",
            "app.storage.bucket-name REQUIRED",
            "app.storage.region OPTIONAL",
            "app.storage.timeout OPTIONAL",
            "app.queue.name OPTIONAL");
    assertThat(key(result, "app.mail.sender-address").reason()).isEqualTo("@Validated + @NotBlank");
    assertThat(key(result, "app.mail.auth.username").reason()).isEqualTo("@Validated + @NotEmpty");
    assertThat(key(result, "app.storage.region").reason())
        .isEqualTo("@ConfigurationProperties, 기본값 있음");
  }

  @Test
  void conditionalDemotesRequiredKeys() throws Exception {
    ExtractionResult result = extract("conditional");

    assertThat(keys(result))
        .containsExactlyInAnyOrder(
            "alarm.push.fcm-key CONDITIONAL",
            "alarm.push.enabled OPTIONAL",
            "storage.s3.bucket CONDITIONAL",
            "storage.local.path OPTIONAL",
            "audit.endpoint CONDITIONAL");
    assertThat(key(result, "alarm.push.fcm-key").reason())
        .isEqualTo("@ConditionalOnProperty(alarm.push.enabled)");
    assertThat(key(result, "storage.s3.bucket").reason()).isEqualTo("@Profile(prod)");
    assertThat(key(result, "audit.endpoint").reason()).isEqualTo("@Profile(prod)");
  }

  @Test
  void containerTypesRecordOnlyThePrefix() throws Exception {
    ExtractionResult result = extract("profiles");

    assertThat(keys(result))
        .contains("servers OPTIONAL", "app.region REQUIRED", "common.tracing REQUIRED")
        .noneMatch(k -> k.startsWith("servers."));
  }

  @Test
  void relaxedNamesAreCanonicalized() throws Exception {
    ExtractionResult result = extract("relaxed-binding");

    assertThat(keys(result))
        .containsExactlyInAnyOrder(
            "payment.api-key REQUIRED",
            "external.service-url REQUIRED",
            "mail.sender-address REQUIRED",
            "feature.flag-name REQUIRED",
            "app.oauth.client-secret REQUIRED");
  }

  @Test
  void unanalyzablePatternsAreReportedNotGuessed() throws Exception {
    ExtractionResult result = extract("unanalyzable");

    assertThat(keys(result))
        .containsExactlyInAnyOrder(
            "app.const-key REQUIRED",
            "app.env-required REQUIRED",
            "app.env-optional OPTIONAL",
            "app.context-key OPTIONAL");
    assertThat(result.unanalyzable())
        .extracting(u -> u.location().fileName() + " " + u.message())
        .containsExactlyInAnyOrder(
            "Broken.java Java 파싱 실패, 이 파일은 분석에서 제외",
            "DynamicConfig.java @Value: SpEL(#{...}), 분석 불가",
            "DynamicConfig.java @Value: 동적 키, 분석 불가",
            "DynamicConfig.java @Value: 키 이름 안에 플레이스홀더(동적 키), 분석 불가",
            "DynamicConfig.java Environment.getProperty: 동적 키, 분석 불가");
    assertThat(result.unanalyzable()).extracting(Unanalyzable::key).containsOnly("");
  }

  @Test
  void secretDefaultValuesAreNotKept() throws Exception {
    ExtractionResult result = extract("secret-leak");

    assertThat(result.toString()).doesNotContain("SUPER_SECRET_VALUE_123");
    assertThat(keys(result))
        .containsExactlyInAnyOrder(
            "db.password REQUIRED", "api.token OPTIONAL", "signing.key REQUIRED");
  }

  @Test
  void resolvesConstantsInSameFileOnly(@TempDir Path dir) throws Exception {
    Path src = Files.createDirectories(dir.resolve("src/main/java/x"));
    Files.writeString(
        src.resolve("Keys.java"),
        """
        package x;
        interface Keys {
          String PREFIX = "svc";
          String URL = PREFIX + ".url";
        }
        class Client {
          static final String TIMEOUT = "${" + Keys.PREFIX + ".timeout:5}";
          static final String SELF = SELF + "x";
          @Value(TIMEOUT) int timeout;
          @Value("${" + Keys.URL + "}") String url;
          @Value(SELF) String loop;
          @Value(value = ("${svc.name}")) String name;
          @Value(value = \"""
              ${svc.block}\""") String block;
          void read(org.springframework.core.env.Environment e, java.util.Properties p) {
            e.getProperty("svc.number." + 1);
            p.getProperty("not.spring");
            var unknown = e;
            unknown.getProperty("unknown.type");
            Runnable r = () -> e.getRequiredProperty("svc.lambda");
          }
        }
        """);

    ExtractionResult result = KeyExtractor.extract(dir, List.of(dir.resolve("src/main/java")));

    assertThat(keys(result))
        .containsExactlyInAnyOrder(
            "svc.timeout OPTIONAL",
            "svc.url REQUIRED",
            "svc.name REQUIRED",
            "svc.block REQUIRED",
            "svc.number.1 OPTIONAL",
            "svc.lambda REQUIRED");
    assertThat(result.unanalyzable()).hasSize(1);
  }

  @Test
  void bindingEdgeCases(@TempDir Path dir) throws Exception {
    Path src = Files.createDirectories(dir.resolve("src/main/java/x"));
    Files.writeString(
        src.resolve("Props.java"),
        """
        package x;
        @ConfigurationProperties(PREFIX)
        class Props {
          static final String PREFIX = "svc";
          private Mode mode;
          private Node node;
          private String[] tags;
          @ConstructorBinding
          Props(Mode mode, @NotNull Node node) {}
          Props() {}
        }
        enum Mode { A, B }
        class Node {
          private Node next;
          private String value;
        }
        @ConfigurationProperties(prefix = PREFIX + "." + DYNAMIC)
        class Dynamic { private String a; }
        @ConfigurationProperties("svc.autowired")
        class Autowired {
          private String a;
          @Autowired Autowired(Object dependency) {}
        }
        @ConfigurationProperties("svc.iface")
        interface Iface {}
        """);

    ExtractionResult result = KeyExtractor.extract(dir, List.of(dir.resolve("src/main/java")));

    assertThat(keys(result))
        .containsExactlyInAnyOrder(
            "svc.mode OPTIONAL", "svc.node.value OPTIONAL", "svc.autowired.a OPTIONAL");
    assertThat(result.unanalyzable())
        .extracting(Unanalyzable::message)
        .containsExactly("@ConfigurationProperties: 동적 prefix, 분석 불가");
  }

  @Test
  void missingRootIsIgnored(@TempDir Path dir) throws Exception {
    ExtractionResult result = KeyExtractor.extract(dir, List.of(dir.resolve("nope")));

    assertThat(result.keys()).isEmpty();
    assertThat(result.unanalyzable()).isEmpty();
  }
}
