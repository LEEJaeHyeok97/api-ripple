package io.github.leejaehyeok97.configguard.extract;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.github.leejaehyeok97.configguard.model.Requirement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@code @ConfigurationProperties} 클래스의 필드·생성자 파라미터를 키로 바꾼다 (명세 6.1).
 *
 * <ul>
 *   <li>record, {@code @ConstructorBinding} 생성자, 파라미터 있는 유일한 생성자 → 생성자 바인딩 (Spring Boot 3)
 *   <li>그 밖 → JavaBean 바인딩 (static이 아닌 필드)
 *   <li>REQUIRED: 클래스에 {@code @Validated} + 멤버에 {@code @NotNull/@NotBlank/@NotEmpty} + 기본값 없음
 *   <li>중첩 타입은 재귀. 중첩 쪽 검증은 필드에 {@code @Valid}가 있을 때만 이어진다.
 *   <li>Map·List·Set·배열은 하위 키를 열거하지 않고 prefix만 OPTIONAL로 기록한다.
 * </ul>
 */
final class ConfigurationPropertiesExtractor {

  private static final Set<String> NOT_NULL_ANNOTATIONS = Set.of("NotNull", "NotBlank", "NotEmpty");
  private static final Set<String> CONTAINER_TYPES =
      Set.of(
          "Map",
          "HashMap",
          "LinkedHashMap",
          "TreeMap",
          "SortedMap",
          "Properties",
          "List",
          "ArrayList",
          "LinkedList",
          "Set",
          "HashSet",
          "LinkedHashSet",
          "TreeSet",
          "SortedSet",
          "Collection",
          "Iterable");

  /** 바인딩되는 멤버 하나 (필드 또는 생성자 파라미터). */
  private record Member(
      Node node, NodeWithAnnotations<?> annotations, String name, Type type, boolean hasDefault) {}

  private final SourceFile file;
  private final TypeIndex types;
  private final KeyCollector out;

  private ConfigurationPropertiesExtractor(SourceFile file, TypeIndex types, KeyCollector out) {
    this.file = file;
    this.types = types;
    this.out = out;
  }

  static void extract(SourceFile file, TypeIndex types, KeyCollector out) {
    var extractor = new ConfigurationPropertiesExtractor(file, types, out);
    for (TypeDeclaration<?> type : file.unit().findAll(TypeDeclaration.class)) {
      Annotations.find(type, "ConfigurationProperties").ifPresent(a -> extractor.root(type, a));
    }
  }

  private void root(TypeDeclaration<?> type, AnnotationExpr annotation) {
    Optional<Expression> prefixExpr =
        Annotations.attribute(annotation, "prefix")
            .or(() -> Annotations.attribute(annotation, "value"));
    Optional<String> prefix =
        prefixExpr.isEmpty() ? Optional.of("") : prefixExpr.flatMap(ConstantResolver::resolve);
    if (prefix.isEmpty()) {
      out.unanalyzable(file, annotation, "", "@ConfigurationProperties: 동적 prefix, 분석 불가");
      return;
    }
    bind(type, prefix.get(), Annotations.has(type, "Validated"), new HashSet<>());
  }

  private void bind(
      TypeDeclaration<?> type, String prefix, boolean validated, Set<TypeDeclaration<?>> visiting) {
    if (!visiting.add(type)) {
      return; // 순환 참조
    }
    for (Member member : members(type)) {
      String key = prefix.isEmpty() ? member.name() : prefix + "." + member.name();
      Optional<TypeDeclaration<?>> nested = nestedType(member);
      if (isContainer(member.type())) {
        out.require(file, member.node(), key, Requirement.OPTIONAL, "Map/List, 하위 키 미열거");
      } else if (nested.isPresent()) {
        boolean cascade = validated && Annotations.has(member.annotations(), "Valid");
        bind(nested.get(), key, cascade, visiting);
      } else {
        leaf(member, key, validated);
      }
    }
    visiting.remove(type);
  }

  private void leaf(Member member, String key, boolean validated) {
    Optional<String> notNull =
        member.annotations().getAnnotations().stream()
            .map(AnnotationExpr::getNameAsString)
            .map(n -> n.substring(n.lastIndexOf('.') + 1))
            .filter(NOT_NULL_ANNOTATIONS::contains)
            .findFirst();
    if (validated && notNull.isPresent() && !member.hasDefault()) {
      out.require(file, member.node(), key, Requirement.REQUIRED, "@Validated + @" + notNull.get());
    } else if (member.hasDefault()) {
      out.require(
          file, member.node(), key, Requirement.OPTIONAL, "@ConfigurationProperties, 기본값 있음");
    } else {
      out.require(
          file, member.node(), key, Requirement.OPTIONAL, "@ConfigurationProperties, 없으면 null 바인딩");
    }
  }

  private List<Member> members(TypeDeclaration<?> type) {
    if (type instanceof RecordDeclaration record) {
      return parameters(record.getParameters());
    }
    if (!(type instanceof ClassOrInterfaceDeclaration clazz) || clazz.isInterface()) {
      return List.of();
    }
    Optional<ConstructorDeclaration> binding = bindingConstructor(clazz);
    if (binding.isPresent()) {
      return parameters(binding.get().getParameters());
    }
    List<Member> members = new ArrayList<>();
    for (FieldDeclaration field : clazz.getFields()) {
      if (field.isStatic()) {
        continue;
      }
      for (VariableDeclarator v : field.getVariables()) {
        members.add(
            new Member(v, field, v.getNameAsString(), v.getType(), v.getInitializer().isPresent()));
      }
    }
    return members;
  }

  private static List<Member> parameters(List<Parameter> parameters) {
    List<Member> members = new ArrayList<>();
    for (Parameter p : parameters) {
      members.add(
          new Member(p, p, p.getNameAsString(), p.getType(), Annotations.has(p, "DefaultValue")));
    }
    return members;
  }

  /** {@code @ConstructorBinding} 생성자, 또는 {@code @Autowired}가 없는 유일한 파라미터 있는 생성자. */
  private static Optional<ConstructorDeclaration> bindingConstructor(
      ClassOrInterfaceDeclaration clazz) {
    List<ConstructorDeclaration> constructors = clazz.getConstructors();
    for (ConstructorDeclaration c : constructors) {
      if (Annotations.has(c, "ConstructorBinding")) {
        return Optional.of(c);
      }
    }
    if (constructors.size() == 1
        && !constructors.get(0).getParameters().isEmpty()
        && !Annotations.has(constructors.get(0), "Autowired")) {
      return Optional.of(constructors.get(0));
    }
    return Optional.empty();
  }

  private Optional<TypeDeclaration<?>> nestedType(Member member) {
    if (member.type() instanceof ClassOrInterfaceType c && c.getTypeArguments().isEmpty()) {
      return types.resolve(member.node(), c.getNameAsString());
    }
    return Optional.empty();
  }

  private static boolean isContainer(Type type) {
    return type instanceof ArrayType
        || (type instanceof ClassOrInterfaceType c
            && CONTAINER_TYPES.contains(c.getNameAsString()));
  }
}
