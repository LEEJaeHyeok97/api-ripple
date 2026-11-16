package io.github.leejaehyeok97.configguard.cli;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** {@code config-guard} 최상위 명령. */
@Command(
    name = "config-guard",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    description = "Spring Boot 애플리케이션의 프로필별 설정 누락을 배포 전에 찾는다. 설정 값은 읽거나 출력하지 않는다.",
    subcommands = {
      CheckCommand.class,
      ExplainCommand.class,
      RulesCommand.class,
      CommandLine.HelpCommand.class
    },
    footer = {
      "",
      "예시:",
      "  config-guard check --profile prod",
      "  config-guard check --profile prod --format json   # 에이전트용",
      "  config-guard explain CG001",
      "",
      "종료 코드: 0 문제 없음, 1 fail-on 기준을 넘는 Finding 있음, 2 사용법 오류, 3 내부 오류"
    })
public final class Main implements Runnable {

  @CommandLine.Spec
  private CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.create();

  public static void main(String[] args) {
    PrintWriter out =
        new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    PrintWriter err =
        new PrintWriter(new OutputStreamWriter(System.err, StandardCharsets.UTF_8), true);
    System.exit(run(args, out, err));
  }

  /** 테스트와 다른 JVM 도구가 프로세스를 끝내지 않고 부를 수 있는 진입점. */
  public static int run(String[] args, PrintWriter out, PrintWriter err) {
    CommandLine cli = new CommandLine(new Main());
    cli.setOut(out);
    cli.setErr(err);
    cli.setCaseInsensitiveEnumValuesAllowed(true);
    cli.setExitCodeExceptionMapper(e -> ExitCodes.INTERNAL_ERROR);
    cli.setExecutionExceptionHandler(
        (exception, commandLine, parseResult) -> {
          commandLine.getErr().println("config-guard: 분석 중 오류: " + exception.getMessage());
          commandLine.getErr().flush();
          return ExitCodes.INTERNAL_ERROR;
        });
    cli.setParameterExceptionHandler(
        (exception, args1) -> {
          CommandLine command = exception.getCommandLine();
          command.getErr().println("config-guard: " + exception.getMessage());
          command.getErr().println("도움말: " + command.getCommandSpec().qualifiedName() + " --help");
          command.getErr().flush();
          return ExitCodes.USAGE;
        });
    return cli.execute(args);
  }

  @Override
  public void run() {
    throw new CommandLine.ParameterException(spec.commandLine(), "명령을 지정한다: check, explain, rules");
  }
}
