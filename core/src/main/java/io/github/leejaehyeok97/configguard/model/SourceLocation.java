package io.github.leejaehyeok97.configguard.model;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;

/**
 * 키가 요구되거나 제공된 위치.
 *
 * @param file 분석 대상 프로젝트 루트 기준 상대 경로. 구분자는 항상 {@code /}.
 * @param line 1부터 시작하는 라인 번호. 라인을 알 수 없으면 0.
 */
public record SourceLocation(String file, int line) {

  public SourceLocation {
    requireNonNull(file, "file");
    if (file.isBlank()) {
      throw new IllegalArgumentException("file must not be blank");
    }
    if (line < 0) {
      throw new IllegalArgumentException("line must be >= 0: " + line);
    }
  }

  /** 경로를 뺀 파일 이름. 콘솔 리포트에 쓴다. */
  public String fileName() {
    int slash = file.lastIndexOf('/');
    return slash < 0 ? file : file.substring(slash + 1);
  }

  /** {@code projectDir} 기준 상대 경로를 {@code /} 구분자로. 프로젝트 밖의 파일은 절대 경로. */
  public static String relativePath(Path projectDir, Path file) {
    Path base = projectDir.toAbsolutePath().normalize();
    Path target = file.toAbsolutePath().normalize();
    Path shown = target.startsWith(base) ? base.relativize(target) : target;
    return shown.toString().replace(File.separatorChar, '/');
  }

  @Override
  public String toString() {
    return line > 0 ? file + ":" + line : file;
  }
}
