package io.github.leejaehyeok97.configguard.gradle;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;

/**
 * {@code io.github.leejaehyeok97.config-guard}. {@code configGuard} 확장과 {@code configGuardCheck}
 * 태스크를 등록한다. {@code check}에는 자동으로 걸지 않는다 (명세 6.7). 원하면 {@code tasks.check {
 * dependsOn("configGuardCheck") }}.
 */
public final class ConfigGuardPlugin implements Plugin<Project> {

  public static final String EXTENSION_NAME = "configGuard";
  public static final String TASK_NAME = "configGuardCheck";

  @Override
  public void apply(Project project) {
    ConfigGuardExtension extension =
        project.getExtensions().create(EXTENSION_NAME, ConfigGuardExtension.class);
    extension.getFailOnError().convention(true);
    extension.getFailOnWarning().convention(false);
    extension.getProfiles().convention(List.of());
    extension.getIgnoreKeys().convention(List.of());
    extension.getExtraFrameworkPrefixes().convention(List.of());

    Directory projectDir = project.getLayout().getProjectDirectory();
    Path projectPath = projectDir.getAsFile().toPath();
    Directory javaDir = projectDir.dir("src/main/java");
    Directory resourceDir = projectDir.dir("src/main/resources");

    project
        .getTasks()
        .register(
            TASK_NAME,
            ConfigGuardCheckTask.class,
            task -> {
              task.setGroup("verification");
              task.setDescription("프로필별 Spring Boot 설정 누락을 검사한다 (config-guard).");
              task.getProjectDirectory().set(projectDir);
              task.getJavaSourceDirectory().set(javaDir);
              task.getResourceDirectory().set(resourceDir);
              task.getJavaSources()
                  .from(project.fileTree(javaDir, tree -> tree.include("**/*.java")));
              task.getConfigFiles()
                  .from(
                      project.fileTree(
                          resourceDir,
                          tree ->
                              tree.include(
                                  "application*.yml",
                                  "application*.yaml",
                                  "application*.properties",
                                  "config/application*.yml",
                                  "config/application*.yaml",
                                  "config/application*.properties")));
              task.getEnvFiles().set(extension.getEnvFiles());
              task.getEnvFileInputs().from(extension.getEnvFiles().map(Map::values));
              task.getEnvFileProfiles()
                  .set(extension.getEnvFiles().map(files -> relativize(projectPath, files)));
              task.getProfiles().set(extension.getProfiles());
              task.getFailOnError().set(extension.getFailOnError());
              task.getFailOnWarning().set(extension.getFailOnWarning());
              task.getIgnoreKeys().set(extension.getIgnoreKeys());
              task.getExtraFrameworkPrefixes().set(extension.getExtraFrameworkPrefixes());
              task.getReportDirectory()
                  .set(project.getLayout().getBuildDirectory().dir("reports/config-guard"));
            });
  }

  private static Map<String, String> relativize(Path projectDir, Map<String, File> files) {
    Map<String, String> result = new LinkedHashMap<>();
    files.forEach(
        (profile, file) ->
            result.put(
                profile,
                projectDir
                    .relativize(file.toPath().toAbsolutePath())
                    .toString()
                    .replace(File.separatorChar, '/')));
    return result;
  }
}
