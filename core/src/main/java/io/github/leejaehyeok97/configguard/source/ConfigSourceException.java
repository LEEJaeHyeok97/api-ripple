package io.github.leejaehyeok97.configguard.source;

import io.github.leejaehyeok97.configguard.model.SourceLocation;

/** 설정 파일을 읽을 수 없을 때 던진다. 메시지에는 위치만 넣는다. 파서의 원래 메시지는 문제가 된 줄(설정 값 포함)을 인용할 수 있어서 쓰지 않는다. */
public final class ConfigSourceException extends Exception {

  private static final long serialVersionUID = 1L;

  private final SourceLocation location;

  public ConfigSourceException(SourceLocation location, String reason) {
    super(location + ": " + reason);
    this.location = location;
  }

  public SourceLocation location() {
    return location;
  }
}
