package io.github.leejaehyeok97.configguard.extract;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import io.github.leejaehyeok97.configguard.model.Requirement;
import io.github.leejaehyeok97.configguard.model.SourceLocation;
import io.github.leejaehyeok97.configguard.model.Unanalyzable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** 코드가 요구하는 설정 키를 뽑는다. 진입점. */
public final class KeyExtractor {

  private KeyExtractor() {}

  /**
   * Java 소스 루트(보통 {@code src/main/java})를 분석한다. {@code src/test}는 넘기지 않는다.
   *
   * @param projectDir 리포트에 쓰는 상대 경로의 기준
   */
  public static ExtractionResult extract(Path projectDir, List<Path> javaRoots) throws IOException {
    List<SourceFile> files = new ArrayList<>();
    List<Unanalyzable> parseFailures = new ArrayList<>();
    JavaParser parser =
        new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    for (Path path : javaFiles(javaRoots)) {
      String display = SourceLocation.relativePath(projectDir, path);
      ParseResult<CompilationUnit> result =
          parser.parse(Files.readString(path, StandardCharsets.UTF_8));
      if (result.isSuccessful() && result.getResult().isPresent()) {
        files.add(new SourceFile(result.getResult().get(), display));
      } else {
        // 파서 메시지는 코드 조각을 인용할 수 있으므로 쓰지 않는다.
        parseFailures.add(
            new Unanalyzable("", new SourceLocation(display, 0), "Java 파싱 실패, 이 파일은 분석에서 제외"));
      }
    }
    KeyCollector out = new KeyCollector();
    TypeIndex types = new TypeIndex(files);
    for (SourceFile file : files) {
      ValueExtractor.extract(file, out);
      EnvironmentExtractor.extract(file, out);
      ConfigurationPropertiesExtractor.extract(file, types, out);
      conditionKeys(file, out);
    }
    ExtractionResult result = out.result();
    List<Unanalyzable> unanalyzable = new ArrayList<>(parseFailures);
    unanalyzable.addAll(result.unanalyzable());
    return new ExtractionResult(result.keys(), unanalyzable);
  }

  /** {@code @ConditionalOnProperty}가 읽는 키도 "코드가 쓰는 키"다. 미사용 키 판정에서 빠지도록 OPTIONAL로 기록한다. */
  private static void conditionKeys(SourceFile file, KeyCollector out) {
    for (AnnotationExpr annotation : file.unit().findAll(AnnotationExpr.class)) {
      if (Annotations.is(annotation, Conditions.CONDITIONAL_ON_PROPERTY)) {
        for (String key : Conditions.conditionKeys(annotation)) {
          out.require(file, annotation, key, Requirement.OPTIONAL, "@ConditionalOnProperty");
        }
      }
    }
  }

  private static List<Path> javaFiles(List<Path> roots) throws IOException {
    List<Path> files = new ArrayList<>();
    for (Path root : roots) {
      if (!Files.isDirectory(root)) {
        continue;
      }
      try (Stream<Path> walk = Files.walk(root)) {
        walk.filter(p -> p.toString().endsWith(".java") && Files.isRegularFile(p))
            .sorted()
            .forEach(files::add);
      }
    }
    return files;
  }
}
