package io.github.leejaehyeok97.configguard.gradle;

import io.github.leejaehyeok97.configguard.AnalysisRequest;
import io.github.leejaehyeok97.configguard.ConfigGuard;
import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.report.ConsoleRenderer;
import io.github.leejaehyeok97.configguard.report.JsonRenderer;
import io.github.leejaehyeok97.configguard.report.MarkdownRenderer;
import io.github.leejaehyeok97.configguard.source.ConfigSourceException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * 프로필별 설정 누락을 검사하고 {@code build/reports/config-guard/}에 report.md, report.json을 쓴다. ERROR가 있고
 * failOnError면 실패한다. 리포트는 실패하기 전에 먼저 쓴다 (GitHub Action이 실패해도 코멘트를 달 수 있게).
 *
 * <p>입력·출력을 정확히 선언해 증분 빌드와 빌드 캐시가 동작한다. Project를 참조하지 않아 configuration cache와 호환된다.
 */
@CacheableTask
public abstract class ConfigGuardCheckTask extends DefaultTask {

  /** 분석할 Java 소스 ({@code src/main/java/**}/*.java). */
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  public abstract ConfigurableFileCollection getJavaSources();

  /** 설정 파일 ({@code src/main/resources/application*}, {@code config/application*}). */
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  public abstract ConfigurableFileCollection getConfigFiles();

  /** 환경변수 이름 목록 파일들 (내용 변화 감지용). */
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getEnvFileInputs();

  /** 프로필 → 환경변수 목록 파일의 프로젝트 기준 상대 경로 (어느 파일이 어느 프로필인지 바뀌면 다시 실행). */
  @Input
  public abstract MapProperty<String, String> getEnvFileProfiles();

  @Internal
  public abstract MapProperty<String, File> getEnvFiles();

  @Internal
  public abstract DirectoryProperty getProjectDirectory();

  @Internal
  public abstract DirectoryProperty getJavaSourceDirectory();

  @Internal
  public abstract DirectoryProperty getResourceDirectory();

  @Input
  public abstract ListProperty<String> getProfiles();

  @Input
  public abstract Property<Boolean> getFailOnError();

  @Input
  public abstract Property<Boolean> getFailOnWarning();

  @Input
  public abstract ListProperty<String> getIgnoreKeys();

  @Input
  public abstract ListProperty<String> getExtraFrameworkPrefixes();

  @OutputDirectory
  public abstract DirectoryProperty getReportDirectory();

  @TaskAction
  public void check() throws IOException {
    List<String> profiles = getProfiles().get();
    if (profiles.isEmpty()) {
      throw new GradleException(
          "config-guard: 검사할 프로필이 없다. configGuard { profiles.set(listOf(\"prod\")) }를 지정한다.");
    }
    Map<String, Path> envFiles = new LinkedHashMap<>();
    getEnvFiles().get().forEach((profile, file) -> envFiles.put(profile, file.toPath()));
    Report report;
    try {
      report =
          ConfigGuard.analyze(
              new AnalysisRequest(
                  getProjectDirectory().get().getAsFile().toPath(),
                  List.of(getJavaSourceDirectory().get().getAsFile().toPath()),
                  List.of(getResourceDirectory().get().getAsFile().toPath()),
                  profiles,
                  envFiles,
                  getIgnoreKeys().get(),
                  getExtraFrameworkPrefixes().get()));
    } catch (ConfigSourceException e) {
      throw new GradleException("config-guard: 설정 파일을 읽을 수 없다: " + e.getMessage(), e);
    }
    Path reportDir = getReportDirectory().get().getAsFile().toPath();
    Files.createDirectories(reportDir);
    Path markdown = reportDir.resolve("report.md");
    Files.writeString(markdown, MarkdownRenderer.render(report), StandardCharsets.UTF_8);
    Files.writeString(
        reportDir.resolve("report.json"), JsonRenderer.render(report), StandardCharsets.UTF_8);
    getLogger().lifecycle(ConsoleRenderer.render(report));

    int errors = report.summary().errors();
    int warnings = report.summary().warnings();
    if (errors > 0 && getFailOnError().get()) {
      throw new GradleException("config-guard: ERROR " + errors + "건. 리포트: " + markdown.toUri());
    }
    if (warnings > 0 && getFailOnWarning().get()) {
      throw new GradleException(
          "config-guard: WARNING " + warnings + "건 (failOnWarning). 리포트: " + markdown.toUri());
    }
  }
}
