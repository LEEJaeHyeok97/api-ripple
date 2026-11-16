package io.github.leejaehyeok97.configguard.gradle;

import java.io.File;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/**
 * {@code configGuard { ... }} DSL.
 *
 * <pre>{@code
 * configGuard {
 *     profiles.set(listOf("dev", "prod"))                 // 필수
 *     envFiles.put("prod", file("deploy/prod.env.keys"))  // 선택
 *     failOnError.set(true)                               // 기본 true
 *     failOnWarning.set(false)                            // 기본 false
 *     ignoreKeys.set(listOf("legacy.*"))
 *     extraFrameworkPrefixes.set(listOf("resilience4j"))
 * }
 * }</pre>
 */
public abstract class ConfigGuardExtension {

  /** 검사할 프로필. 필수. */
  public abstract ListProperty<String> getProfiles();

  /** 프로필 → 환경변수 이름 목록 파일. 지정하지 않은 프로필은 환경변수 검사를 건너뛴다. */
  public abstract MapProperty<String, File> getEnvFiles();

  /** ERROR가 있으면 태스크를 실패시킨다. 기본 true. */
  public abstract Property<Boolean> getFailOnError();

  /** WARNING이 있으면 태스크를 실패시킨다. 기본 false. */
  public abstract Property<Boolean> getFailOnWarning();

  /** 모든 판정에서 뺄 키 glob. */
  public abstract ListProperty<String> getIgnoreKeys();

  /** 미사용 키(CG004) 판정에서 뺄 prefix 추가분. */
  public abstract ListProperty<String> getExtraFrameworkPrefixes();
}
