package io.github.leejaehyeok97.configguard.extract;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.github.leejaehyeok97.configguard.model.Requirement;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@code environment.getProperty("k")}(OPTIONAL), {@code getRequiredProperty("k")}(REQUIRED)를 찾는다.
 * 호출 대상이 Spring {@code Environment} 계열 타입으로 선언된 변수·필드이거나 {@code getEnvironment()} 호출일 때만 인정한다.
 * {@code System.getProperty}, {@code Properties.getProperty}와 구분하기 위해서다.
 */
final class EnvironmentExtractor {

  private static final Set<String> ENVIRONMENT_TYPES =
      Set.of(
          "Environment",
          "ConfigurableEnvironment",
          "StandardEnvironment",
          "AbstractEnvironment",
          "PropertyResolver",
          "ConfigurablePropertyResolver",
          "StandardServletEnvironment",
          "StandardReactiveWebEnvironment");

  private EnvironmentExtractor() {}

  static void extract(SourceFile file, KeyCollector out) {
    for (MethodCallExpr call : file.unit().findAll(MethodCallExpr.class)) {
      String method = call.getNameAsString();
      boolean required = method.equals("getRequiredProperty");
      if ((!required && !method.equals("getProperty")) || call.getArguments().isEmpty()) {
        continue;
      }
      if (call.getScope().isEmpty() || !isEnvironment(call.getScope().get())) {
        continue;
      }
      Optional<String> key = ConstantResolver.resolve(call.getArgument(0));
      if (key.isEmpty()) {
        out.unanalyzable(file, call, "", "Environment." + method + ": 동적 키, 분석 불가");
      } else if (required) {
        out.require(file, call, key.get(), Requirement.REQUIRED, "Environment.getRequiredProperty");
      } else {
        out.require(file, call, key.get(), Requirement.OPTIONAL, "Environment.getProperty");
      }
    }
  }

  private static boolean isEnvironment(Expression scope) {
    if (scope instanceof MethodCallExpr call) {
      return call.getNameAsString().equals("getEnvironment");
    }
    if (scope instanceof NameExpr name) {
      return declaredType(scope, name.getNameAsString())
          .map(EnvironmentExtractor::isEnvironmentType)
          .orElse(false);
    }
    if (scope instanceof FieldAccessExpr access && access.getScope() instanceof ThisExpr) {
      return declaredType(scope, access.getNameAsString())
          .map(EnvironmentExtractor::isEnvironmentType)
          .orElse(false);
    }
    return false;
  }

  private static boolean isEnvironmentType(Type type) {
    return type instanceof ClassOrInterfaceType c
        && ENVIRONMENT_TYPES.contains(c.getNameAsString());
  }

  /** 가장 가까운 선언부터 찾는다: 메서드·생성자·람다 파라미터, 지역 변수, 필드, record 컴포넌트. */
  private static Optional<Type> declaredType(Node from, String name) {
    AtomicReference<Type> found = new AtomicReference<>();
    from.walk(
        Node.TreeTraversal.PARENTS,
        parent -> {
          if (found.get() != null) {
            return;
          }
          if (parent instanceof NodeWithParameters<?> withParams) {
            for (Parameter p : withParams.getParameters()) {
              if (p.getNameAsString().equals(name)) {
                found.set(p.getType());
                return;
              }
            }
          }
          if (parent instanceof TypeDeclaration<?> type) {
            for (FieldDeclaration field : type.getFields()) {
              for (VariableDeclarator v : field.getVariables()) {
                if (v.getNameAsString().equals(name)) {
                  found.set(v.getType());
                  return;
                }
              }
            }
          }
          if (parent instanceof RecordDeclaration record) {
            for (Parameter p : record.getParameters()) {
              if (p.getNameAsString().equals(name)) {
                found.set(p.getType());
                return;
              }
            }
          }
          if (!(parent instanceof TypeDeclaration<?>)) {
            for (VariableDeclarator v : parent.findAll(VariableDeclarator.class)) {
              if (v.getNameAsString().equals(name)
                  && v.getParentNode().filter(p -> !(p instanceof FieldDeclaration)).isPresent()) {
                found.set(v.getType());
                return;
              }
            }
          }
        });
    return Optional.ofNullable(found.get());
  }
}
