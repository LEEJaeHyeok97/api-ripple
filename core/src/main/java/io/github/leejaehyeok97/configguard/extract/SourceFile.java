package io.github.leejaehyeok97.configguard.extract;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.github.leejaehyeok97.configguard.model.SourceLocation;

/** 파싱된 Java 파일 하나와 리포트용 상대 경로. */
record SourceFile(CompilationUnit unit, String path) {

  SourceLocation locationOf(Node node) {
    return new SourceLocation(path, node.getBegin().map(p -> p.line).orElse(0));
  }
}
