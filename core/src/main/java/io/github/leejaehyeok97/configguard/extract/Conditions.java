package io.github.leejaehyeok97.configguard.extract;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import io.github.leejaehyeok97.configguard.normalize.RelaxedNames;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code @ConditionalOnProperty}, {@code @Profile} 조건. 키를 요구하는 지점을 감싸는 메서드·생성자·클래스(바깥 클래스 포함)에 붙어
 * 있으면 REQUIRED를 CONDITIONAL로 강등한다 (명세 6.1).
 */
final class Conditions {

  static final String CONDITIONAL_ON_PROPERTY = "ConditionalOnProperty";
  static final String PROFILE = "Profile";

  private Conditions() {}

  /** 노드를 감싸는 선언들의 조건을 바깥쪽 순서로 설명한다. 조건이 없으면 빈 값. */
  static Optional<String> describe(Node node) {
    List<String> descriptions = new ArrayList<>();
    node.walk(
        Node.TreeTraversal.PARENTS,
        parent -> {
          if (parent instanceof NodeWithAnnotations<?> annotated) {
            for (AnnotationExpr annotation : annotated.getAnnotations()) {
              describe(annotation).ifPresent(d -> descriptions.add(0, d));
            }
          }
        });
    if (node instanceof NodeWithAnnotations<?> self) {
      for (AnnotationExpr annotation : self.getAnnotations()) {
        describe(annotation).ifPresent(descriptions::add);
      }
    }
    return descriptions.isEmpty() ? Optional.empty() : Optional.of(String.join(", ", descriptions));
  }

  private static Optional<String> describe(AnnotationExpr annotation) {
    if (Annotations.is(annotation, CONDITIONAL_ON_PROPERTY)) {
      List<String> keys = conditionKeys(annotation);
      return Optional.of(
          "@ConditionalOnProperty" + (keys.isEmpty() ? "" : "(" + String.join(", ", keys) + ")"));
    }
    if (Annotations.is(annotation, PROFILE)) {
      List<String> profiles =
          Annotations.attribute(annotation, "value")
              .flatMap(ConstantResolver::resolveAll)
              .orElse(List.of());
      return Optional.of(
          "@Profile" + (profiles.isEmpty() ? "" : "(" + String.join(", ", profiles) + ")"));
    }
    return Optional.empty();
  }

  /** {@code @ConditionalOnProperty(prefix = "a", name = "b")} → [a.b]. 해석할 수 없으면 빈 목록. */
  static List<String> conditionKeys(AnnotationExpr annotation) {
    String prefix =
        Annotations.attribute(annotation, "prefix").flatMap(ConstantResolver::resolve).orElse("");
    List<String> names =
        Annotations.attribute(annotation, "name")
            .or(() -> Annotations.attribute(annotation, "value"))
            .flatMap(ConstantResolver::resolveAll)
            .orElse(List.of());
    List<String> keys = new ArrayList<>();
    for (String name : names) {
      String key = prefix.isBlank() ? name : prefix.strip() + "." + name;
      keys.add(RelaxedNames.canonical(key));
    }
    return keys;
  }
}
