package io.github.leejaehyeok97.configguard.cli;

/** 고정된 종료 코드 (명세 6.9). 바꾸면 이 CLI를 쓰는 스크립트와 에이전트가 깨진다. */
public final class ExitCodes {

  /** 문제 없음 (fail-on 기준을 넘는 Finding이 없다). */
  public static final int OK = 0;

  /** fail-on 기준을 넘는 Finding이 있다. */
  public static final int FINDINGS = 1;

  /** 사용법 오류: 잘못된 옵션, 없는 경로, 모르는 규칙 ID. */
  public static final int USAGE = 2;

  /** 분석 중 내부 오류: 파일을 읽을 수 없음, YAML 문법 오류. */
  public static final int INTERNAL_ERROR = 3;

  private ExitCodes() {}
}
