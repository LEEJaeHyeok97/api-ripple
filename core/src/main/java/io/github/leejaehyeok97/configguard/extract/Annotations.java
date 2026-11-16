package io.github.leejaehyeok97.configguard.extract;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import java.util.Optional;

/** 어노테이션 이름 비교와 속성 읽기. 이름은 단순 이름 또는 정규 이름 모두 받는다. */
final class Annotations {

  private Annotations() {}

  static boolean is(AnnotationExpr annotation, String simpleName) {
    String name = annotation.getNameAsString();
    return name.equals(simpleName) || name.endsWith("." + simpleName);
  }

  static Optional<AnnotationExpr> find(NodeWithAnnotations<?> node, String simpleName) {
    return node.getAnnotations().stream().filter(a -> is(a, simpleName)).findFirst();
  }

  static boolean has(NodeWithAnnotations<?> node, String simpleName) {
    return find(node, simpleName).isPresent();
  }

  /** {@code @A(x)}의 x, 또는 {@code @A(attribute = x)}의 x. */
  static Optional<Expression> attribute(AnnotationExpr annotation, String attribute) {
    if (annotation instanceof SingleMemberAnnotationExpr single) {
      return attribute.equals("value") ? Optional.of(single.getMemberValue()) : Optional.empty();
    }
    if (annotation instanceof NormalAnnotationExpr normal) {
      for (MemberValuePair pair : normal.getPairs()) {
        if (pair.getNameAsString().equals(attribute)) {
          return Optional.of(pair.getValue());
        }
      }
    }
    return Optional.empty();
  }
}
