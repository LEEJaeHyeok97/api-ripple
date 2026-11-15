package io.github.leejaehyeok97.configguard;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

/** {@code src/test/resources/fixtures/<name>} 경로. */
public final class Fixtures {

  private Fixtures() {}

  public static Path path(String name) {
    URL url = Fixtures.class.getResource("/fixtures/" + name);
    if (url == null) {
      throw new IllegalArgumentException("fixture not found: " + name);
    }
    try {
      return Path.of(url.toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
