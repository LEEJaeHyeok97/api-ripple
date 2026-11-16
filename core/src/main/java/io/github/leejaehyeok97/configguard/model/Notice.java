package io.github.leejaehyeok97.configguard.model;

import static java.util.Objects.requireNonNull;

/**
 * 규칙 위반은 아니지만 사용자가 알아야 하는 안내. 예: 환경변수 목록 파일이 없어 검사를 건너뛰었다. 규칙 ID 의미를 흐리지 않으려고 {@link Finding}과 분리한다
 * (docs/decisions.md).
 *
 * @param code 기계가 읽는 코드. 예: {@code ENV_CHECK_SKIPPED}
 */
public record Notice(String code, String profile, String message) {

  public static final String ENV_CHECK_SKIPPED = "ENV_CHECK_SKIPPED";

  public Notice {
    requireNonNull(code, "code");
    requireNonNull(profile, "profile");
    requireNonNull(message, "message");
  }
}
