package io.github.leejaehyeok97.configguard.model;

import java.util.List;

/**
 * 검사 결과 전체. JSON 리포트의 원본이다.
 *
 * @param findings 프로필 순서(지정한 순서, 공통 {@code *}은 마지막) → 심각도 → 키 순으로 정렬되어 있다.
 */
public record Report(Summary summary, List<Finding> findings, List<Notice> notices) {

  /** JSON 리포트 스키마 버전 ({@code schema/report.schema.json}). 깨지는 변경이면 올린다. */
  public static final String SCHEMA_VERSION = "1";

  /** 프로필과 무관한 Finding(분석 불가, 기본 파일의 미사용 키)의 profile 값. */
  public static final String ALL_PROFILES = "*";

  public Report {
    findings = List.copyOf(findings);
    notices = List.copyOf(notices);
  }
}
