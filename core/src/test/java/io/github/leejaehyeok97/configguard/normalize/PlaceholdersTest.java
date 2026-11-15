package io.github.leejaehyeok97.configguard.normalize;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.leejaehyeok97.configguard.normalize.Placeholders.Reference;
import org.junit.jupiter.api.Test;

class PlaceholdersTest {

  @Test
  void withoutDefaultIsRequired() {
    assertThat(Placeholders.parse("${a.b}")).containsExactly(new Reference("a.b", false, false));
    assertThat(Placeholders.requiredNames("${a.b}")).containsExactly("a.b");
  }

  @Test
  void defaultValueAndEmptyDefaultAreOptional() {
    assertThat(Placeholders.parse("${a.b:default}"))
        .containsExactly(new Reference("a.b", true, false));
    assertThat(Placeholders.parse("${a.b:}")).containsExactly(new Reference("a.b", true, false));
    assertThat(Placeholders.requiredNames("${a.b:}")).isEmpty();
  }

  @Test
  void nestedPlaceholderInDefaultIsParsedRecursively() {
    assertThat(Placeholders.parse("${a:${b}}"))
        .containsExactly(new Reference("a", true, false), new Reference("b", false, false));
    assertThat(Placeholders.requiredNames("${a:${b}}")).containsExactly("b");
    assertThat(Placeholders.requiredNames("${a:${b:x}}")).isEmpty();
  }

  @Test
  void extractsEveryPlaceholderInText() {
    assertThat(Placeholders.requiredNames("prefix-${a.b}-mid-${c}-suffix"))
        .containsExactly("a.b", "c");
    assertThat(Placeholders.requiredNames("${a}${a}")).containsExactly("a");
  }

  @Test
  void dynamicNameIsMarked() {
    assertThat(Placeholders.parse("${${env}.url}"))
        .containsExactly(new Reference("${env}.url", false, true));
    assertThat(Placeholders.requiredNames("${${env}.url}")).isEmpty();
  }

  @Test
  void ignoresEscapedUnclosedAndPlainText() {
    assertThat(Placeholders.parse("\\${not.a.placeholder}")).isEmpty();
    assertThat(Placeholders.parse("${unclosed")).isEmpty();
    assertThat(Placeholders.parse("plain text")).isEmpty();
    assertThat(Placeholders.parse("")).isEmpty();
  }

  @Test
  void bracesInsideDefaultAreBalanced() {
    assertThat(Placeholders.parse("${a:{json}}")).containsExactly(new Reference("a", true, false));
  }

  @Test
  void detectsPlaceholderPresence() {
    assertThat(Placeholders.containsPlaceholder("x-${a}")).isTrue();
    assertThat(Placeholders.containsPlaceholder("#{1 + 1}")).isFalse();
  }
}
