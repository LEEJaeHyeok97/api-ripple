package io.github.leejaehyeok97.configguard.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RelaxedNamesTest {

  @ParameterizedTest(name = "canonical({0}) = {1}")
  @CsvSource({
    "payment.api-key,          payment.api-key",
    "payment.apiKey,           payment.api-key",
    "payment.api_key,          payment.api-key",
    "PAYMENT.API_KEY,          payment.api-key",
    "Payment.ApiKey,           payment.api-key",
    "payment.URLPath,          payment.url-path",
    "app.s3Bucket,             app.s3-bucket",
    "app.oauth2Client.id,      app.oauth2-client.id",
    "a.b[0].c,                 a.b[0].c",
    "a.b[0][1].cKey,           a.b[0][1].c-key",
    "a.map[Some.Key].v,        a.map[Some.Key].v",
    "'  spaced.key  ',         spaced.key",
    "trailing.dot.,            trailing.dot",
    "double..dot,              double.dot",
    "a.__weird__,              a.weird",
    "x.max-pool-size,          x.max-pool-size",
    "x.maxPoolSize,            x.max-pool-size",
  })
  void canonical(String input, String expected) {
    assertThat(RelaxedNames.canonical(input)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "matchKey({0}) = {1}")
  @CsvSource({
    "payment.api-key,      payment.apikey",
    "payment.apiKey,       payment.apikey",
    "payment.api_key,      payment.apikey",
    "payment.apikey,       payment.apikey",
    "a.my-list[0].x-y,     a.mylist[0].xy",
    "a.map[k-ey],          a.map[k-ey]",
  })
  void matchKey(String input, String expected) {
    assertThat(RelaxedNames.matchKey(input)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "fromEnvVar({0}) = {1}")
  @CsvSource({
    "PAYMENT_APIKEY,     payment.apikey",
    "PAYMENT_API_KEY,    payment.api.key",
    "MY_LIST_0_NAME,     my.list[0].name",
    "SERVER_PORT,        server.port",
    "DB__URL,            db.url",
    "simple,             simple",
  })
  void fromEnvVar(String input, String expected) {
    assertThat(RelaxedNames.fromEnvVar(input)).isEqualTo(expected);
  }

  @Test
  void envVarFormMatchesDashedKey() {
    assertThat(RelaxedNames.matchKey(RelaxedNames.fromEnvVar("PAYMENT_APIKEY")))
        .isEqualTo(RelaxedNames.matchKey("payment.api-key"));
  }

  @Test
  void envVarCandidatesIncludeModernAndLegacyForms() {
    assertThat(RelaxedNames.envVarCandidates("payment.api-key"))
        .containsExactly("PAYMENT_APIKEY", "PAYMENT_API_KEY");
    assertThat(RelaxedNames.envVarCandidates("my.list[0].name")).containsExactly("MY_LIST_0_NAME");
    assertThat(RelaxedNames.envVarCandidates("payment.apiKey")).contains("PAYMENT_APIKEY");
  }

  @Test
  void selfOrAncestor() {
    assertThat(RelaxedNames.isSelfOrAncestor("app.servers", "app.servers[0].host")).isTrue();
    assertThat(RelaxedNames.isSelfOrAncestor("app.servers", "app.servers.main")).isTrue();
    assertThat(RelaxedNames.isSelfOrAncestor("app.servers", "app.servers")).isTrue();
    assertThat(RelaxedNames.isSelfOrAncestor("app.server", "app.servers")).isFalse();
    assertThat(RelaxedNames.isSelfOrAncestor("app.apiKey", "app.api-key.v")).isTrue();
    assertThat(RelaxedNames.isSelfOrAncestor("", "app")).isFalse();
  }

  @Test
  void unclosedIndexIsKeptAsIs() {
    assertThat(RelaxedNames.canonical("a.b[0")).isEqualTo("a.b[0");
    assertThat(RelaxedNames.matchKey("a.b[0")).isEqualTo("a.b[0");
    assertThat(RelaxedNames.envVarCandidates("a.b[0")).containsExactly("A_B_0");
  }

  @Test
  void emptyInputs() {
    assertThat(RelaxedNames.canonical("")).isEmpty();
    assertThat(RelaxedNames.fromEnvVar("___")).isEmpty();
    assertThat(RelaxedNames.fromEnvVar("0_NAME")).isEqualTo("0.name");
  }
}
