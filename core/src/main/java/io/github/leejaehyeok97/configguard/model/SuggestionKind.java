package io.github.leejaehyeok97.configguard.model;

/** 에이전트나 사람이 바로 적용할 수 있는 수정 종류. */
public enum SuggestionKind {
  /** 설정 파일({@link Suggestion#file()})에 키({@link Suggestion#target()})를 추가한다. */
  ADD_YAML_KEY,
  /** 배포 환경에 환경변수({@link Suggestion#target()})를 추가한다. 파일은 없다. */
  ADD_ENV_VAR,
  /** 코드({@link Suggestion#file()})에서 키({@link Suggestion#target()})에 기본값을 준다. */
  ADD_DEFAULT_VALUE,
  /** 검사 대상에서 키({@link Suggestion#target()})를 제외한다. */
  IGNORE_KEY
}
