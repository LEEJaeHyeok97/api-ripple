package io.github.leejaehyeok97.configguard.cli;

import io.github.leejaehyeok97.configguard.AnalysisRequest;
import io.github.leejaehyeok97.configguard.ConfigGuard;
import io.github.leejaehyeok97.configguard.model.Report;
import io.github.leejaehyeok97.configguard.report.ConsoleRenderer;
import io.github.leejaehyeok97.configguard.report.JsonRenderer;
import io.github.leejaehyeok97.configguard.report.MarkdownRenderer;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code config-guard check}: 프로필별 설정 누락을 검사한다. */
@Command(
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    name = "check",
    description = "코드가 요구하는 설정 키와 프로필별로 제공되는 키를 비교한다.",
    sortOptions = false,
    footer = {
      "",
      "예시:",
      "  config-guard check --profile prod",
      "  config-guard check ./my-service --profile dev --profile prod \\",
      "      --env-file prod=deploy/prod.env.keys --format json",
      "  config-guard check --profile prod --fail-on warning --ignore 'legacy.*'",
      "",
      "종료 코드: 0 문제 없음, 1 fail-on 기준을 넘는 Finding 있음, 2 사용법 오류, 3 내부 오류",
      "--format json이면 stdout에는 JSON만 나온다. 진행 로그는 stderr로 간다."
    })
final class CheckCommand implements Callable<Integer> {

  enum Format {
    console,
    markdown,
    json
  }

  enum FailOn {
    error,
    warning,
    never
  }

  @Spec private CommandSpec spec = CommandSpec.create();

  @Parameters(
      index = "0",
      arity = "0..1",
      paramLabel = "PATH",
      description = {
        "프로젝트 디렉터리. src/main/java, src/main/resources를 읽는다. 기본: 현재 디렉터리",
        "  예: config-guard check ./my-service --profile prod"
      })
  private Path project = Path.of(".");

  @Option(
      names = {"-p", "--profile"},
      required = true,
      paramLabel = "PROFILE",
      description = {"검사할 프로필. 여러 번 지정한다.", "  예: --profile dev --profile prod"})
  private List<String> profiles = new ArrayList<>();

  @Option(
      names = "--env-file",
      paramLabel = "PROFILE=FILE",
      description = {
        "프로필의 환경변수 이름 목록 파일(.env 또는 한 줄에 이름 하나). 값은 읽지 않는다.",
        "지정하지 않은 프로필은 환경변수 검사를 건너뛴다. 상대 경로는 현재 디렉터리 기준.",
        "  예: --env-file prod=deploy/prod.env.keys"
      })
  private Map<String, Path> envFiles = new LinkedHashMap<>();

  @Option(
      names = "--format",
      paramLabel = "FORMAT",
      description = {
        "출력 형식: ${COMPLETION-CANDIDATES}. 기본: ${DEFAULT-VALUE}",
        "  예: --format json   (에이전트용. 스키마: schema/report.schema.json)"
      })
  private Format format = Format.console;

  @Option(
      names = "--fail-on",
      paramLabel = "LEVEL",
      description = {
        "종료 코드 1을 낼 심각도: ${COMPLETION-CANDIDATES}. 기본: ${DEFAULT-VALUE}",
        "  예: --fail-on warning   (CG003 조건부 키도 실패로 본다)"
      })
  private FailOn failOn = FailOn.error;

  @Option(
      names = "--ignore",
      paramLabel = "GLOB",
      description = {"모든 판정에서 뺄 키. 여러 번 지정한다. * 는 점을 포함한 임의 문자열.", "  예: --ignore 'legacy.*'"})
  private List<String> ignore = new ArrayList<>();

  @Option(
      names = "--framework-prefix",
      paramLabel = "PREFIX",
      description = {
        "CG004(미사용 키)에서 뺄 prefix 추가분. spring, server, management, logging 등은 기본 제외.",
        "  예: --framework-prefix resilience4j"
      })
  private List<String> frameworkPrefixes = new ArrayList<>();

  @Override
  public Integer call() throws Exception {
    if (!Files.isDirectory(project)) {
      throw new ParameterException(spec.commandLine(), "프로젝트 디렉터리가 없다: " + project);
    }
    for (var entry : envFiles.entrySet()) {
      if (!profiles.contains(entry.getKey())) {
        throw new ParameterException(
            spec.commandLine(), "--env-file의 프로필이 --profile에 없다: " + entry.getKey());
      }
      if (!Files.isRegularFile(entry.getValue())) {
        throw new ParameterException(spec.commandLine(), "환경변수 목록 파일이 없다: " + entry.getValue());
      }
    }
    PrintWriter err = spec.commandLine().getErr();
    err.println(
        "config-guard: "
            + project.toAbsolutePath().normalize()
            + " 분석 중 (프로필: "
            + String.join(", ", profiles)
            + ")");
    err.flush();
    Report report =
        ConfigGuard.analyze(
            AnalysisRequest.standard(project, profiles, envFiles, ignore, frameworkPrefixes));
    PrintWriter out = spec.commandLine().getOut();
    out.print(
        switch (format) {
          case console -> ConsoleRenderer.render(report);
          case markdown -> MarkdownRenderer.render(report);
          case json -> JsonRenderer.render(report);
        });
    out.flush();
    return exitCode(report);
  }

  private int exitCode(Report report) {
    int errors = report.summary().errors();
    int warnings = report.summary().warnings();
    boolean fail =
        switch (failOn) {
          case error -> errors > 0;
          case warning -> errors + warnings > 0;
          case never -> false;
        };
    return fail ? ExitCodes.FINDINGS : ExitCodes.OK;
  }
}
