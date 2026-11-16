package io.github.leejaehyeok97.configguard.cli;

import io.github.leejaehyeok97.configguard.model.Rule;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** {@code config-guard rules}: 규칙 ID 목록. */
@Command(
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    name = "rules",
    description = "규칙 목록을 출력한다. 자세한 설명은 config-guard explain <ID>.",
    footer = {"", "예시:", "  config-guard rules"})
final class RulesCommand implements Callable<Integer> {

  @Spec private CommandSpec spec = CommandSpec.create();

  @Override
  public Integer call() {
    PrintWriter out = spec.commandLine().getOut();
    for (Rule rule : Rule.values()) {
      out.printf(
          "%s  %-16s %-8s %s%n",
          rule.id(), rule.name(), rule.defaultSeverity(), RuleDocs.summary(rule));
    }
    out.flush();
    return ExitCodes.OK;
  }
}
