package io.github.leejaehyeok97.configguard.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.leejaehyeok97.configguard.AnalysisRequest;
import io.github.leejaehyeok97.configguard.ConfigGuard;
import io.github.leejaehyeok97.configguard.report.JsonRenderer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

  private record Result(int exitCode, String out, String err) {}

  private static Path fixture(String name) {
    return Path.of(System.getProperty("configguard.fixtures")).resolve(name);
  }

  private static Result run(String... args) {
    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();
    int code = Main.run(args, new PrintWriter(out), new PrintWriter(err));
    return new Result(code, out.toString(), err.toString());
  }

  @Test
  void checkPrintsConsoleReportAndFailsOnError() {
    Result result = run("check", fixture("value-basic").toString(), "--profile", "prod");

    assertThat(result.exitCode()).isEqualTo(ExitCodes.FINDINGS);
    assertThat(result.out())
        .startsWith("config-guard: prod\n")
        .contains("ERROR  payment.api-key")
        .endsWith("1 error, 0 warnings, 0 info\n");
    assertThat(result.err()).contains("분석 중 (프로필: prod)");
  }

  @Test
  void jsonGoesToStdoutAloneAndEqualsCoreOutput() throws Exception {
    Path project = fixture("env-placeholder");
    Path prodEnv = project.resolve("deploy/prod.env.keys");

    Result result =
        run(
            "check",
            project.toString(),
            "-p",
            "dev",
            "-p",
            "prod",
            "--env-file",
            "prod=" + prodEnv,
            "--format",
            "JSON");

    String expected =
        JsonRenderer.render(
            ConfigGuard.analyze(
                AnalysisRequest.standard(
                    project,
                    List.of("dev", "prod"),
                    Map.of("prod", prodEnv),
                    List.of(),
                    List.of())));
    assertThat(result.exitCode()).isEqualTo(ExitCodes.FINDINGS);
    assertThat(result.out()).isEqualTo(expected);
    assertThat(new ObjectMapper().readTree(result.out()).get("schemaVersion").asText())
        .isEqualTo("1");
    assertThat(result.err()).doesNotContain("{");
  }

  @Test
  void failOnControlsExitCode() {
    String conditional = fixture("conditional").toString();

    assertThat(run("check", conditional, "-p", "prod").exitCode()).isEqualTo(ExitCodes.OK);
    assertThat(run("check", conditional, "-p", "prod", "--fail-on", "warning").exitCode())
        .isEqualTo(ExitCodes.FINDINGS);
    assertThat(
            run("check", fixture("value-basic").toString(), "-p", "prod", "--fail-on", "never")
                .exitCode())
        .isEqualTo(ExitCodes.OK);
  }

  @Test
  void ignoreAndMarkdownFormat() {
    Result result =
        run(
            "check",
            fixture("value-basic").toString(),
            "-p",
            "prod",
            "--ignore",
            "payment.*",
            "--framework-prefix",
            "payment",
            "--format",
            "markdown");

    assertThat(result.exitCode()).isEqualTo(ExitCodes.OK);
    assertThat(result.out()).startsWith("<!-- config-guard-report -->").contains("✅ prod: 문제 없음");
  }

  @Test
  void usageErrorsExitWithTwo(@TempDir Path dir) throws Exception {
    String project = fixture("value-basic").toString();
    Path env = Files.writeString(dir.resolve("prod.env"), "A=1\n");

    assertThat(run("check", project).exitCode()).isEqualTo(ExitCodes.USAGE);
    assertThat(run("check", dir.resolve("nope").toString(), "-p", "prod").exitCode())
        .isEqualTo(ExitCodes.USAGE);
    assertThat(run("check", project, "-p", "prod", "--format", "xml").exitCode())
        .isEqualTo(ExitCodes.USAGE);
    assertThat(run("check", project, "-p", "prod", "--env-file", "dev=" + env).exitCode())
        .isEqualTo(ExitCodes.USAGE);
    assertThat(
            run("check", project, "-p", "prod", "--env-file", "prod=" + dir.resolve("x"))
                .exitCode())
        .isEqualTo(ExitCodes.USAGE);
    Result noCommand = run();
    assertThat(noCommand.exitCode()).isEqualTo(ExitCodes.USAGE);
    assertThat(noCommand.err()).contains("check, explain, rules");
  }

  @Test
  void brokenYamlIsInternalErrorWithoutQuotingContent(@TempDir Path dir) throws Exception {
    Path resources = Files.createDirectories(dir.resolve("src/main/resources"));
    Files.writeString(resources.resolve("application.yml"), "a: [SUPER_SECRET_VALUE_123\n");

    Result result = run("check", dir.toString(), "-p", "prod");

    assertThat(result.exitCode()).isEqualTo(ExitCodes.INTERNAL_ERROR);
    assertThat(result.err()).contains("application.yml:").doesNotContain("SUPER_SECRET_VALUE_123");
    assertThat(result.out()).isEmpty();
  }

  @Test
  void explainPrintsRuleDocument() throws Exception {
    Result result = run("explain", "cg001");

    assertThat(result.exitCode()).isEqualTo(ExitCodes.OK);
    Path doc =
        Path.of(System.getProperty("configguard.fixtures"))
            .resolve("../../../../../docs/reference/rules/CG001.md")
            .normalize();
    assertThat(result.out()).isEqualTo(Files.readString(doc));
    assertThat(run("explain", "CG999").exitCode()).isEqualTo(ExitCodes.USAGE);
  }

  @Test
  void rulesListsEveryRuleWithSummary() {
    Result result = run("rules");

    assertThat(result.exitCode()).isEqualTo(ExitCodes.OK);
    assertThat(result.out().lines())
        .hasSize(5)
        .anySatisfy(l -> assertThat(l).startsWith("CG001  MISSING_KEY").contains("ERROR"))
        .allSatisfy(l -> assertThat(l.length()).isGreaterThan(35));
  }

  @Test
  void versionAndHelpIncludeExamples() {
    assertThat(run("--version").out()).startsWith("config-guard ");
    Result help = run("check", "--help");
    assertThat(help.exitCode()).isEqualTo(ExitCodes.OK);
    assertThat(help.out())
        .contains("예: --env-file prod=deploy/prod.env.keys")
        .contains("예: --format json")
        .contains("종료 코드: 0 문제 없음");
  }
}
