package io.github.leejaehyeok97.configguard;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 분석 입력. CLI와 Gradle 플러그인이 같은 값을 넘기면 같은 결과가 나온다.
 *
 * @param projectDir 리포트 경로의 기준
 * @param javaRoots 보통 {@code src/main/java}
 * @param resourceRoots 보통 {@code src/main/resources}
 * @param profiles 검사할 프로필
 * @param envFiles 프로필 → 환경변수 이름 목록 파일
 * @param ignoreKeys 모든 판정에서 뺄 키 glob
 * @param extraFrameworkPrefixes 미사용 키 판정에서 뺄 prefix 추가분
 */
public record AnalysisRequest(
    Path projectDir,
    List<Path> javaRoots,
    List<Path> resourceRoots,
    List<String> profiles,
    Map<String, Path> envFiles,
    List<String> ignoreKeys,
    List<String> extraFrameworkPrefixes) {

  public AnalysisRequest {
    javaRoots = List.copyOf(javaRoots);
    resourceRoots = List.copyOf(resourceRoots);
    profiles = List.copyOf(profiles);
    envFiles = Map.copyOf(envFiles);
    ignoreKeys = List.copyOf(ignoreKeys);
    extraFrameworkPrefixes = List.copyOf(extraFrameworkPrefixes);
  }

  /** 표준 디렉터리({@code src/main/java}, {@code src/main/resources})를 쓰는 요청. */
  public static AnalysisRequest standard(
      Path projectDir,
      List<String> profiles,
      Map<String, Path> envFiles,
      List<String> ignoreKeys,
      List<String> extraFrameworkPrefixes) {
    return new AnalysisRequest(
        projectDir,
        List.of(projectDir.resolve("src/main/java")),
        List.of(projectDir.resolve("src/main/resources")),
        profiles,
        envFiles,
        ignoreKeys,
        extraFrameworkPrefixes);
  }
}
