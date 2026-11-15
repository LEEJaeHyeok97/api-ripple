package io.github.leejaehyeok97.configguard.model;

/** 코드가 설정 키를 얼마나 강하게 요구하는지. */
public enum Requirement {
  /** 없으면 기동이 실패한다. */
  REQUIRED,
  /** 기본값이 있거나 null 바인딩이 허용되어 없어도 기동한다. */
  OPTIONAL,
  /** {@code @ConditionalOnProperty} / {@code @Profile} 조건이 맞을 때만 필요하다. */
  CONDITIONAL
}
