package io.github.leejaehyeok97.configguard.report;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 테스트 전용 JSON Schema 검증기. {@code schema/report.schema.json}이 쓰는 키워드만 지원한다: type, const, enum,
 * required, properties, additionalProperties(false), items, minItems, minimum, minLength, $ref(로컬),
 * allOf, if/then.
 *
 * <p>명세 3장 라이브러리 표에 JSON Schema 검증기가 없어 직접 구현했다. 스키마에 다른 키워드를 쓰면 {@link IllegalStateException}으로
 * 실패시켜, 검증이 조용히 약해지지 않게 한다 (docs/decisions.md).
 */
final class MiniJsonSchema {

  private static final List<String> SUPPORTED =
      List.of(
          "$schema",
          "$id",
          "title",
          "description",
          "$defs",
          "type",
          "const",
          "enum",
          "required",
          "properties",
          "additionalProperties",
          "items",
          "minItems",
          "minimum",
          "minLength",
          "$ref",
          "allOf",
          "if",
          "then");

  private final JsonNode root;

  MiniJsonSchema(JsonNode root) {
    this.root = root;
  }

  /** 위반 목록. 비어 있으면 통과. */
  List<String> validate(JsonNode instance) {
    List<String> errors = new ArrayList<>();
    validate(root, instance, "$", errors);
    return errors;
  }

  private void validate(JsonNode schema, JsonNode value, String path, List<String> errors) {
    for (Iterator<String> it = schema.fieldNames(); it.hasNext(); ) {
      String keyword = it.next();
      if (!SUPPORTED.contains(keyword)) {
        throw new IllegalStateException("unsupported schema keyword: " + keyword);
      }
    }
    if (schema.has("$ref")) {
      validate(resolve(schema.get("$ref").asText()), value, path, errors);
    }
    if (schema.has("type") && !typeMatches(schema.get("type"), value)) {
      errors.add(
          path + ": expected type " + schema.get("type") + " but was " + value.getNodeType());
      return;
    }
    if (schema.has("const") && !schema.get("const").equals(value)) {
      errors.add(path + ": expected const " + schema.get("const"));
    }
    if (schema.has("enum") && !contains(schema.get("enum"), value)) {
      errors.add(path + ": " + value + " not in enum " + schema.get("enum"));
    }
    if (value.isObject()) {
      object(schema, value, path, errors);
    }
    if (value.isArray()) {
      if (schema.has("minItems") && value.size() < schema.get("minItems").asInt()) {
        errors.add(path + ": expected at least " + schema.get("minItems") + " items");
      }
      if (schema.has("items")) {
        for (int i = 0; i < value.size(); i++) {
          validate(schema.get("items"), value.get(i), path + "[" + i + "]", errors);
        }
      }
    }
    if (value.isNumber()
        && schema.has("minimum")
        && value.asLong() < schema.get("minimum").asLong()) {
      errors.add(path + ": below minimum " + schema.get("minimum"));
    }
    if (value.isTextual()
        && schema.has("minLength")
        && value.asText().length() < schema.get("minLength").asInt()) {
      errors.add(path + ": shorter than " + schema.get("minLength"));
    }
    if (schema.has("allOf")) {
      for (JsonNode sub : schema.get("allOf")) {
        validate(sub, value, path, errors);
      }
    }
    if (schema.has("if") && validate(schema.get("if"), value).isEmpty() && schema.has("then")) {
      validate(schema.get("then"), value, path, errors);
    }
  }

  private List<String> validate(JsonNode schema, JsonNode value) {
    List<String> errors = new ArrayList<>();
    validate(schema, value, "$", errors);
    return errors;
  }

  private void object(JsonNode schema, JsonNode value, String path, List<String> errors) {
    if (schema.has("required")) {
      for (JsonNode name : schema.get("required")) {
        if (!value.has(name.asText())) {
          errors.add(path + ": missing required " + name.asText());
        }
      }
    }
    JsonNode properties = schema.get("properties");
    for (Map.Entry<String, JsonNode> field : value.properties()) {
      if (properties != null && properties.has(field.getKey())) {
        validate(
            properties.get(field.getKey()), field.getValue(), path + "." + field.getKey(), errors);
      } else if (schema.has("additionalProperties")
          && !schema.get("additionalProperties").asBoolean()) {
        errors.add(path + ": unexpected property " + field.getKey());
      }
    }
  }

  private JsonNode resolve(String ref) {
    if (!ref.startsWith("#/")) {
      throw new IllegalStateException("only local $ref is supported: " + ref);
    }
    JsonNode node = root;
    for (String part : ref.substring(2).split("/", -1)) {
      node = node.get(part);
      if (node == null) {
        throw new IllegalStateException("unresolvable $ref: " + ref);
      }
    }
    return node;
  }

  private static boolean typeMatches(JsonNode type, JsonNode value) {
    if (type.isArray()) {
      for (JsonNode t : type) {
        if (typeMatches(t, value)) {
          return true;
        }
      }
      return false;
    }
    return switch (type.asText()) {
      case "object" -> value.isObject();
      case "array" -> value.isArray();
      case "string" -> value.isTextual();
      case "integer" -> value.isIntegralNumber();
      case "number" -> value.isNumber();
      case "boolean" -> value.isBoolean();
      case "null" -> value.isNull();
      default -> throw new IllegalStateException("unknown type " + type);
    };
  }

  private static boolean contains(JsonNode array, JsonNode value) {
    for (JsonNode item : array) {
      if (item.equals(value)) {
        return true;
      }
    }
    return false;
  }
}
