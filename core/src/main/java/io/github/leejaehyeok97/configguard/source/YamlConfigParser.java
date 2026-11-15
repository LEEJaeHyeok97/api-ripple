package io.github.leejaehyeok97.configguard.source;

import io.github.leejaehyeok97.configguard.model.SourceLocation;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * YAML 설정 파일을 문서 단위로 평탄화한다. 라인 번호가 필요해서 객체로 바꾸지 않고 SnakeYAML 노드 트리(compose)를 쓴다. 노드 트리만 만들기 때문에 임의
 * 객체 생성 위험이 없다.
 */
final class YamlConfigParser {

  private YamlConfigParser() {}

  static List<ConfigDocument> parse(Path path, String file, @Nullable String fileProfile)
      throws IOException, ConfigSourceException {
    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(true);
    List<ConfigDocument> documents = new ArrayList<>();
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      for (Node root : new Yaml(options).composeAll(reader)) {
        DocumentBuilder document = new DocumentBuilder(file, fileProfile, line(root));
        flatten(root, "", document);
        documents.add(document.build());
      }
    } catch (MarkedYAMLException e) {
      Mark mark = e.getProblemMark();
      int line = mark == null ? 0 : mark.getLine() + 1;
      throw new ConfigSourceException(new SourceLocation(file, line), "YAML 문법 오류");
    } catch (YAMLException e) {
      throw new ConfigSourceException(new SourceLocation(file, 0), "YAML 문법 오류");
    }
    return documents;
  }

  private static void flatten(Node node, String prefix, DocumentBuilder document) {
    if (node instanceof MappingNode mapping) {
      for (NodeTuple tuple : mapping.getValue()) {
        flattenEntry(tuple, prefix, document);
      }
    } else if (node instanceof SequenceNode sequence) {
      List<Node> items = sequence.getValue();
      for (int i = 0; i < items.size(); i++) {
        Node item = items.get(i);
        String key = prefix + "[" + i + "]";
        if (item instanceof ScalarNode scalar) {
          document.add(key, line(item), scalar.getValue());
        } else {
          flatten(item, key, document);
        }
      }
    } else if (node instanceof ScalarNode scalar && !prefix.isEmpty()) {
      document.add(prefix, line(node), scalar.getValue());
    }
  }

  private static void flattenEntry(NodeTuple tuple, String prefix, DocumentBuilder document) {
    Node keyNode = tuple.getKeyNode();
    Node valueNode = tuple.getValueNode();
    if (!(keyNode instanceof ScalarNode keyScalar)) {
      return;
    }
    String rawKey = keyScalar.getValue();
    if ("<<".equals(rawKey)) {
      // 병합 키: 병합 대상 매핑의 키를 현재 위치에 펼친다.
      if (valueNode instanceof SequenceNode merged) {
        merged.getValue().forEach(n -> flatten(n, prefix, document));
      } else {
        flatten(valueNode, prefix, document);
      }
      return;
    }
    String key = join(prefix, rawKey);
    if (valueNode instanceof MappingNode child && child.getValue().isEmpty()) {
      return; // 빈 매핑은 키를 만들지 않는다 (Spring과 같다).
    }
    if (valueNode instanceof ScalarNode scalar) {
      document.add(key, line(keyNode), scalar.getValue());
    } else if (valueNode instanceof SequenceNode seq && seq.getValue().isEmpty()) {
      document.add(key, line(keyNode), null); // 빈 리스트는 키 자체를 제공한다.
    } else {
      flatten(valueNode, key, document);
    }
  }

  private static String join(String prefix, String key) {
    if (prefix.isEmpty()) {
      return key;
    }
    return key.startsWith("[") ? prefix + key : prefix + "." + key;
  }

  private static int line(Node node) {
    Mark mark = node.getStartMark();
    return mark == null ? 0 : mark.getLine() + 1;
  }
}
