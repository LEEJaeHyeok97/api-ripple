package io.github.leejaehyeok97.configguard.source;

import io.github.leejaehyeok97.configguard.model.SourceLocation;
import io.github.leejaehyeok97.configguard.normalize.Placeholders;
import io.github.leejaehyeok97.configguard.normalize.RelaxedNames;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** 파서가 키를 하나씩 넣으면 {@link ConfigDocument}를 만든다. 값은 여기서 플레이스홀더 이름만 뽑고 버린다. */
final class DocumentBuilder {

  private final String file;
  private final @Nullable String fileProfile;
  private final int line;
  private final List<ConfigEntry> entries = new ArrayList<>();
  private final StructuralKeys.Collector structure = new StructuralKeys.Collector();

  DocumentBuilder(String file, @Nullable String fileProfile, int line) {
    this.file = file;
    this.fileProfile = fileProfile;
    this.line = line;
  }

  void add(String rawKey, int keyLine, @Nullable String value) {
    String canonical = RelaxedNames.canonical(rawKey);
    if (canonical.isEmpty()) {
      return;
    }
    structure.accept(canonical, value);
    List<String> referenced = new ArrayList<>();
    for (Placeholders.Reference ref : Placeholders.parse(value == null ? "" : value)) {
      if (!ref.dynamic()) {
        referenced.add(ref.name());
      }
    }
    entries.add(
        new ConfigEntry(
            canonical,
            new SourceLocation(file, keyLine),
            Placeholders.requiredNames(value == null ? "" : value),
            referenced));
  }

  ConfigDocument build() {
    return new ConfigDocument(
        file,
        fileProfile,
        line,
        List.copyOf(structure.onProfiles),
        List.copyOf(structure.includes),
        structure.groups,
        List.copyOf(entries),
        structure.supported);
  }
}
