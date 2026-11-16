package io.github.leejaehyeok97.configguard.extract;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import io.github.leejaehyeok97.configguard.model.Requirement;
import io.github.leejaehyeok97.configguard.normalize.Placeholders;
import java.util.Optional;

/** {@code @Value("${...}")}에서 키를 뽑는다 (명세 6.1). */
final class ValueExtractor {

  private ValueExtractor() {}

  static void extract(SourceFile file, KeyCollector out) {
    if (importsLombokValue(file)) {
      return; // lombok.Value는 불변 클래스 어노테이션이라 설정과 무관하다.
    }
    for (AnnotationExpr annotation : file.unit().findAll(AnnotationExpr.class)) {
      if (!Annotations.is(annotation, "Value")) {
        continue;
      }
      Optional<Expression> argument = Annotations.attribute(annotation, "value");
      if (argument.isEmpty()) {
        continue;
      }
      Optional<String> text = ConstantResolver.resolve(argument.get());
      if (text.isEmpty()) {
        out.unanalyzable(file, annotation, "", "@Value: 동적 키, 분석 불가");
        continue;
      }
      if (text.get().contains("#{")) {
        out.unanalyzable(file, annotation, "", "@Value: SpEL(#{...}), 분석 불가");
        continue;
      }
      for (Placeholders.Reference ref : Placeholders.parse(text.get())) {
        if (ref.dynamic()) {
          out.unanalyzable(file, annotation, "", "@Value: 키 이름 안에 플레이스홀더(동적 키), 분석 불가");
        } else if (ref.hasDefault()) {
          out.require(file, annotation, ref.name(), Requirement.OPTIONAL, "@Value, 기본값 있음");
        } else {
          out.require(file, annotation, ref.name(), Requirement.REQUIRED, "@Value, 기본값 없음");
        }
      }
    }
  }

  private static boolean importsLombokValue(SourceFile file) {
    for (ImportDeclaration declaration : file.unit().getImports()) {
      if (declaration.getNameAsString().equals("lombok.Value")) {
        return true;
      }
    }
    return false;
  }
}
