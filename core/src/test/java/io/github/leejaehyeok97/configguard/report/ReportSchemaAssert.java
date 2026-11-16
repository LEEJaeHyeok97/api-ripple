package io.github.leejaehyeok97.configguard.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;

/** JSON 리포트가 {@code schema/report.schema.json}을 통과하는지 검사한다. */
public final class ReportSchemaAssert {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ReportSchemaAssert() {}

  public static void assertValid(String json) throws IOException {
    String schemaPath = System.getProperty("configguard.reportSchema");
    assertThat(schemaPath).as("system property configguard.reportSchema").isNotNull();
    JsonNode schema = MAPPER.readTree(Path.of(schemaPath).toFile());
    assertThat(new MiniJsonSchema(schema).validate(MAPPER.readTree(json))).isEmpty();
  }
}
