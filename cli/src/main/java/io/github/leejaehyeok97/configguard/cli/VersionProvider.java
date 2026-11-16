package io.github.leejaehyeok97.configguard.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine;

/** 빌드 때 넣은 {@code version.properties}에서 버전을 읽는다. */
final class VersionProvider implements CommandLine.IVersionProvider {

  static String version() {
    Properties properties = new Properties();
    try (InputStream in = VersionProvider.class.getResourceAsStream("version.properties")) {
      if (in != null) {
        properties.load(in);
      }
    } catch (IOException e) {
      // 버전을 못 읽어도 CLI는 동작해야 한다.
    }
    return properties.getProperty("version", "unknown");
  }

  @Override
  public String[] getVersion() {
    return new String[] {"config-guard " + version()};
  }
}
