# Gradle 플러그인 DSL

```kotlin
plugins {
    id("io.github.leejaehyeok97.config-guard") version "0.1.0"
}

configGuard {
    profiles.set(listOf("dev", "prod"))                 // 필수
    envFiles.put("prod", file("deploy/prod.env.keys"))  // 선택
    failOnError.set(true)                               // 기본 true
    failOnWarning.set(false)                            // 기본 false
    ignoreKeys.set(listOf("legacy.*"))
    extraFrameworkPrefixes.set(listOf("resilience4j"))
}
```

```bash
./gradlew configGuardCheck
```

## 속성

| 속성 | 타입 | 기본 | 설명 |
|---|---|---|---|
| `profiles` | `ListProperty<String>` | (필수) | 검사할 프로필. 비어 있으면 태스크가 실패한다 |
| `envFiles` | `MapProperty<String, File>` | 비어 있음 | 프로필 → 환경변수 이름 목록 파일. 지정하지 않은 프로필은 환경변수 검사를 건너뛴다 |
| `failOnError` | `Property<Boolean>` | `true` | ERROR가 있으면 태스크 실패 |
| `failOnWarning` | `Property<Boolean>` | `false` | WARNING이 있으면 태스크 실패 |
| `ignoreKeys` | `ListProperty<String>` | 비어 있음 | 모든 판정에서 뺄 키 glob. `*`는 점을 포함한 임의 문자열 |
| `extraFrameworkPrefixes` | `ListProperty<String>` | 비어 있음 | CG004(미사용 키)에서 뺄 prefix 추가분 |

CLI 옵션과의 대응: `profiles` ↔ `--profile`, `envFiles` ↔ `--env-file`, `ignoreKeys` ↔ `--ignore`, `extraFrameworkPrefixes` ↔ `--framework-prefix`, `failOnError`/`failOnWarning` ↔ `--fail-on`. 같은 입력이면 `report.json`이 CLI의 `--format json` 출력과 같다(테스트로 보장).

## 태스크 `configGuardCheck`

- 그룹: `verification`. **`check`에 자동으로 걸지 않는다.** 원하면:

  ```kotlin
  tasks.check { dependsOn("configGuardCheck") }
  ```

- 입력: `src/main/java/**/*.java`, `src/main/resources/application*`, `src/main/resources/config/application*`, `envFiles`의 파일, 위 속성들
- 출력: `build/reports/config-guard/report.md`(PR 코멘트용), `build/reports/config-guard/report.json`([JSON 리포트](json-report.md))
- 콘솔에는 [콘솔 형식](../../README.md#출력-예시) 리포트를 출력한다.
- 리포트를 먼저 쓰고 나서 실패한다. GitHub Action이 실패한 빌드에서도 코멘트를 달 수 있다.
- 증분 빌드: 입력이 그대로면 `UP-TO-DATE`. `@CacheableTask`라서 빌드 캐시도 쓴다. 이때는 콘솔 리포트가 다시 출력되지 않으니 `build/reports/config-guard/`를 본다.
- configuration cache와 호환된다(`--configuration-cache`).

## 여러 모듈

플러그인은 적용한 프로젝트의 `src/main`만 본다. 멀티 모듈이면 Spring Boot 애플리케이션 모듈에 적용한다. 리포트는 그 모듈의 `build/reports/config-guard/`에 생긴다.
