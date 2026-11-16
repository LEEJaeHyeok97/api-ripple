package io.github.leejaehyeok97.configguard.extract;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * 키 표현식을 문자열로 해석한다. 문자열 리터럴, 리터럴끼리의 {@code +}, 그리고 <b>같은 파일 안의</b> {@code static final} 상수까지만 해석한다
 * (명세 6.1). 그 밖에는 빈 값을 돌려주고, 호출한 쪽이 "동적 키"로 보고한다.
 */
final class ConstantResolver {

  private ConstantResolver() {}

  static Optional<String> resolve(Expression expression) {
    return resolve(expression, new HashSet<>());
  }

  /** 문자열 하나 또는 문자열 배열({@code {"a", "b"}})을 해석한다. 하나라도 못 하면 빈 값. */
  static Optional<List<String>> resolveAll(Expression expression) {
    if (expression instanceof ArrayInitializerExpr array) {
      List<String> values = new ArrayList<>();
      for (Expression item : array.getValues()) {
        Optional<String> value = resolve(item);
        if (value.isEmpty()) {
          return Optional.empty();
        }
        values.add(value.get());
      }
      return Optional.of(values);
    }
    return resolve(expression).map(List::of);
  }

  private static Optional<String> resolve(Expression expression, Set<String> visiting) {
    if (expression instanceof StringLiteralExpr literal) {
      return Optional.of(literal.asString());
    }
    if (expression instanceof TextBlockLiteralExpr block) {
      return Optional.of(block.asString());
    }
    if (expression instanceof IntegerLiteralExpr number) {
      return Optional.of(number.getValue());
    }
    if (expression instanceof EnclosedExpr enclosed) {
      return resolve(enclosed.getInner(), visiting);
    }
    if (expression instanceof BinaryExpr binary
        && binary.getOperator() == BinaryExpr.Operator.PLUS) {
      Optional<String> left = resolve(binary.getLeft(), visiting);
      Optional<String> right = resolve(binary.getRight(), visiting);
      return left.isPresent() && right.isPresent()
          ? Optional.of(left.get() + right.get())
          : Optional.empty();
    }
    if (expression instanceof NameExpr name) {
      return constant(expression, null, name.getNameAsString(), visiting);
    }
    if (expression instanceof FieldAccessExpr access
        && access.getScope() instanceof NameExpr type) {
      return constant(expression, type.getNameAsString(), access.getNameAsString(), visiting);
    }
    return Optional.empty();
  }

  /** 같은 파일 안에서 상수를 찾는다. 가까운 바깥 타입부터 본다. */
  private static Optional<String> constant(
      Node from, @Nullable String typeName, String fieldName, Set<String> visiting) {
    Optional<CompilationUnit> unit = from.findCompilationUnit();
    if (unit.isEmpty() || !visiting.add(typeName + "#" + fieldName)) {
      return Optional.empty();
    }
    List<TypeDeclaration<?>> candidates = new ArrayList<>();
    if (typeName == null) {
      from.walk(
          Node.TreeTraversal.PARENTS,
          n -> {
            if (n instanceof TypeDeclaration<?> t) {
              candidates.add(t);
            }
          });
    }
    unit.get()
        .findAll(TypeDeclaration.class)
        .forEach(
            t -> {
              if (typeName == null || t.getNameAsString().equals(typeName)) {
                candidates.add(t);
              }
            });
    for (TypeDeclaration<?> type : candidates) {
      for (FieldDeclaration field : type.getFields()) {
        if (!isConstant(type, field)) {
          continue;
        }
        for (VariableDeclarator variable : field.getVariables()) {
          if (variable.getNameAsString().equals(fieldName)
              && variable.getInitializer().isPresent()) {
            return resolve(variable.getInitializer().get(), visiting);
          }
        }
      }
    }
    return Optional.empty();
  }

  private static boolean isConstant(TypeDeclaration<?> type, FieldDeclaration field) {
    boolean inInterface = type instanceof ClassOrInterfaceDeclaration c && c.isInterface();
    return inInterface || (field.isStatic() && field.isFinal());
  }
}
