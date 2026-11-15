package io.github.leejaehyeok97.configguard.model;

import static java.util.Objects.requireNonNull;

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

  @Override
  public String toString() {
    return line > 0 ? file + ":" + line : file;
  }
}
