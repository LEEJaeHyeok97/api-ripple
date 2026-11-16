package io.github.leejaehyeok97.configguard.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** JSON 리포트가 {@code schema/report.schema.json}(JSON Schema 2020-12)을 통과하는지 검사한다. */
public final class ReportSchemaAssert {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ReportSchemaAssert() {}

  public static void assertValid(String json) throws IOException {
    assertThat(errors(MAPPER.readTree(json))).isEmpty();
  }

  /** 스키마 위반 목록. 비어 있으면 통과. 각 항목은 위치와 메시지를 담는다. */
  public static List<String> errors(JsonNode report) throws IOException {
    return schema().validate(report).stream().map(Error::toString).toList();
  }

  private static Schema schema() throws IOException {
    String schemaPath = System.getProperty("configguard.reportSchema");
    assertThat(schemaPath).as("system property configguard.reportSchema").isNotNull();
    JsonNode schema = MAPPER.readTree(Path.of(schemaPath).toFile());
    return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(schema);
  }
}
