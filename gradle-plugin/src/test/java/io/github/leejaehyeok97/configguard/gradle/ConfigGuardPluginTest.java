package io.github.leejaehyeok97.configguard.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.leejaehyeok97.configguard.cli.Main;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Gradle TestKit으로 실제 빌드를 돌린다. 프로젝트는 core의 fixture를 복사해 만든다. */
class ConfigGuardPluginTest {

  private static final String TASK = ":" + ConfigGuardPlugin.TASK_NAME;

  @TempDir Path project;

  private void fixture(String name, String configGuardBlock) throws IOException {
    Path source = Path.of(System.getProperty("configguard.fixtures")).resolve(name);
    try (Stream<Path> files = Files.walk(source)) {
      for (Path file : files.toList()) {
        Path target = project.resolve(source.relativize(file).toString());
        if (Files.isDirectory(file)) {
          Files.createDirectories(target);
        } else {
          Files.copy(file, target);
        }
      }
    }
    Files.writeString(project.resolve("settings.gradle.kts"), "rootProject.name = \"demo\"\n");
    Files.writeString(
        project.resolve("build.gradle.kts"),
        """
        plugins {
            base
            id("io.github.leejaehyeok97.config-guard")
        }

        configGuard {
        %s
        }
        """
            .formatted(configGuardBlock));
  }

  private GradleRunner runner(String... args) {
    String[] all = Stream.concat(Stream.of(args), Stream.of("--stacktrace")).toArray(String[]::new);
    return GradleRunner.create()
        .withProjectDir(project.toFile())
        .withPluginClasspath()
        .withArguments(all);
  }

  @Test
  void failsOnErrorAfterWritingReports() throws Exception {
    fixture("value-basic", "profiles.set(listOf(\"prod\"))");

    BuildResult result = runner(TASK).buildAndFail();

    assertThat(result.task(TASK).getOutcome()).isEqualTo(TaskOutcome.FAILED);
    assertThat(result.getOutput())
        .contains("ERROR  payment.api-key")
        .contains("config-guard: ERROR 1건");
    Path reports = project.resolve("build/reports/config-guard");
    assertThat(Files.readString(reports.resolve("report.md")))
        .startsWith("<!-- config-guard-report -->");
    assertThat(Files.readString(reports.resolve("report.json"))).contains("\"ruleId\": \"CG001\"");
  }

  @Test
  void failOnErrorFalsePassesAndSecondRunIsUpToDate() throws Exception {
    fixture("value-basic", "profiles.set(listOf(\"prod\"))\nfailOnError.set(false)");

    BuildResult first = runner(TASK).build();
    BuildResult second = runner(TASK).build();

    assertThat(first.task(TASK).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(second.task(TASK).getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);

    Files.writeString(
        project.resolve("src/main/resources/application-prod.yml"), "payment:\n  api-key: x\n");
    assertThat(runner(TASK).build().task(TASK).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
  }

  @Test
  void warningsFailOnlyWithFailOnWarning() throws Exception {
    fixture("conditional", "profiles.set(listOf(\"prod\"))");
    assertThat(runner(TASK).build().task(TASK).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

    Files.writeString(
        project.resolve("build.gradle.kts"),
        Files.readString(project.resolve("build.gradle.kts"))
            .replace("profiles.set", "failOnWarning.set(true)\nprofiles.set"));
    BuildResult result = runner(TASK).buildAndFail();
    assertThat(result.getOutput()).contains("WARNING 3건 (failOnWarning)");
  }

  @Test
  void isNotWiredIntoCheckAndIsInVerificationGroup() throws Exception {
    fixture("value-basic", "profiles.set(listOf(\"prod\"))");

    BuildResult tasks = runner("tasks", "--group", "verification").build();
    BuildResult check = runner("check").build();

    assertThat(tasks.getOutput()).contains("configGuardCheck - 프로필별 Spring Boot 설정 누락을 검사한다");
    assertThat(check.task(TASK)).isNull();
  }

  @Test
  void configurationCacheIsReused() throws Exception {
    fixture("value-basic", "profiles.set(listOf(\"prod\"))\nfailOnError.set(false)");

    runner(TASK, "--configuration-cache").build();
    BuildResult second = runner(TASK, "--configuration-cache", "--rerun-tasks").build();

    assertThat(second.getOutput()).contains("Configuration cache entry reused");
    assertThat(second.task(TASK).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
  }

  @Test
  void missingProfilesFailsWithHint() throws Exception {
    fixture("value-basic", "");

    assertThat(runner(TASK).buildAndFail().getOutput()).contains("검사할 프로필이 없다");
  }

  /** 명세 6.9: Gradle 플러그인과 CLI는 같은 입력에 같은 결과를 낸다. */
  @Test
  void jsonReportIsIdenticalToCli() throws Exception {
    fixture(
        "env-placeholder",
        """
        profiles.set(listOf("dev", "prod", "staging"))
        envFiles.put("dev", file("deploy/dev.env"))
        envFiles.put("prod", file("deploy/prod.env.keys"))
        ignoreKeys.set(listOf("redis.*"))
        extraFrameworkPrefixes.set(listOf("mail"))
        failOnError.set(false)
        """);

    runner(TASK).build();
    StringWriter cliOut = new StringWriter();
    int exit =
        Main.run(
            new String[] {
              "check",
              project.toString(),
              "-p",
              "dev",
              "-p",
              "prod",
              "-p",
              "staging",
              "--env-file",
              "dev=" + project.resolve("deploy/dev.env"),
              "--env-file",
              "prod=" + project.resolve("deploy/prod.env.keys"),
              "--ignore",
              "redis.*",
              "--framework-prefix",
              "mail",
              "--format",
              "json"
            },
            new PrintWriter(cliOut),
            new PrintWriter(new StringWriter()));

    assertThat(exit).isEqualTo(1);
    assertThat(Files.readString(project.resolve("build/reports/config-guard/report.json")))
        .isEqualTo(cliOut.toString());
  }
}
