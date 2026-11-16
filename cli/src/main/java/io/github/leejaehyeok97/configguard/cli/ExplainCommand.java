package io.github.leejaehyeok97.configguard.cli;

import io.github.leejaehyeok97.configguard.model.Rule;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** {@code config-guard explain CG001}: 규칙 문서(docs/reference/rules)를 그대로 출력한다. */
@Command(
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    name = "explain",
    description = "규칙 설명, 예시, 고치는 법을 출력한다. 내용은 docs/reference/rules/<ID>.md와 같다.",
    footer = {"", "예시:", "  config-guard explain CG001", "  config-guard rules   # 규칙 ID 목록"})
final class ExplainCommand implements Callable<Integer> {

  @Spec private CommandSpec spec = CommandSpec.create();

  @Parameters(paramLabel = "RULE_ID", description = "규칙 ID. 예: CG001")
  private String ruleId = "";

  @Override
  public Integer call() {
    Rule rule =
        Rule.byId(ruleId)
            .orElseThrow(
                () ->
                    new ParameterException(
                        spec.commandLine(),
                        "모르는 규칙 ID: " + ruleId + " (config-guard rules로 목록을 본다)"));
    String doc =
        RuleDocs.markdown(rule)
            .orElseThrow(() -> new IllegalStateException("규칙 문서가 jar에 없다: " + rule.id()));
    spec.commandLine().getOut().print(doc);
    spec.commandLine().getOut().flush();
    return ExitCodes.OK;
  }
}
