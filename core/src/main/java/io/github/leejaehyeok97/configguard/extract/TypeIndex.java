package io.github.leejaehyeok97.configguard.extract;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 분석 대상 소스에 선언된 타입을 단순 이름으로 찾는다. 중첩 프로퍼티 클래스를 따라갈 때 쓴다. */
final class TypeIndex {

  private final Map<String, List<TypeDeclaration<?>>> bySimpleName = new HashMap<>();

  TypeIndex(List<SourceFile> files) {
    for (SourceFile file : files) {
      for (TypeDeclaration<?> type : file.unit().findAll(TypeDeclaration.class)) {
        bySimpleName.computeIfAbsent(type.getNameAsString(), k -> new ArrayList<>()).add(type);
      }
    }
  }

  /**
   * {@code from} 위치에서 보이는 타입을 찾는다. 바깥 타입의 중첩 타입 → 같은 파일 → 전체에서 유일한 이름 순서. 열거형은 값 하나로 바인딩되므로 제외한다.
   */
  Optional<TypeDeclaration<?>> resolve(Node from, String simpleName) {
    List<TypeDeclaration<?>> candidates = bySimpleName.getOrDefault(simpleName, List.of());
    if (candidates.isEmpty()) {
      return Optional.empty();
    }
    for (TypeDeclaration<?> candidate : candidates) {
      if (isVisibleFrom(candidate, from)) {
        return bindable(candidate);
      }
    }
    return candidates.size() == 1 ? bindable(candidates.get(0)) : Optional.empty();
  }

  private static boolean isVisibleFrom(TypeDeclaration<?> candidate, Node from) {
    return candidate.findCompilationUnit().equals(from.findCompilationUnit());
  }

  private static Optional<TypeDeclaration<?>> bindable(TypeDeclaration<?> type) {
    return type instanceof EnumDeclaration ? Optional.empty() : Optional.of(type);
  }
}
