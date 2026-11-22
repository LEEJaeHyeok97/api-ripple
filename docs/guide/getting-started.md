# 시작하기

Spring Boot 프로젝트에 config-guard를 붙이고, 첫 결과를 읽고 고치는 데까지. 예시는 [samples/demo](../../samples/demo)다.

## 1. 플러그인 적용

Gradle Plugin Portal에서 받는다. 내 프로젝트의 `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.leejaehyeok97.config-guard") version "0.1.1"
}

configGuard {
    profiles.set(listOf("dev", "prod"))
}
```

## 2. 실행

```bash
./gradlew configGuardCheck
```

프로필마다 결과가 나온다. ERROR가 있으면 태스크가 실패한다. 리포트는 `build/reports/config-guard/report.md`, `report.json`에도 쓴다.

## 3. 환경변수 목록 알려 주기

운영 환경은 보통 비밀 값을 환경변수로 준다. config-guard는 값이 아니라 **이름**만 알면 된다. 배포 환경에 들어가는 환경변수 이름을 파일로 적는다.

```bash
# deploy/prod.env.keys — 한 줄에 이름 하나. 값은 적지 않는다.
PAYMENT_APIKEY
DB_URL
```

`.env` 형식(`KEY=value`)도 받지만 값은 읽자마자 버린다. 그래도 저장소에는 이름만 있는 파일을 두는 것이 안전하다.

```kotlin
configGuard {
    profiles.set(listOf("dev", "prod"))
    envFiles.put("prod", file("deploy/prod.env.keys"))
}
```

이제 두 가지를 더 확인한다.

- `PAYMENT_APIKEY`가 있으면 `payment.api-key`가 제공된 것으로 본다 (relaxed binding).
- `spring.datasource.url: ${DB_URL}`의 `DB_URL`이 목록에 없으면 CG002 ERROR.

목록을 지정하지 않은 프로필은 환경변수 검사를 건너뛰고 안내(NOTE)를 남긴다.

## 4. 결과 읽기

samples/demo의 결과:

```
config-guard: prod
  ERROR  payment.api-key       PaymentClient.java:14  @Value, 기본값 없음
  ERROR  env DB_URL            application-prod.yml:7  spring.datasource.url이 요구
  WARN   alarm.push.fcm-key    FcmSender.java:13  @ConditionalOnProperty(alarm.push.enabled)
2 errors, 1 warning, 0 info
```

| 줄 | 뜻 | 고치는 법 |
|---|---|---|
| `ERROR payment.api-key` | 코드가 기본값 없이 요구하는데 prod 어디에도 없다 (CG001) | prod 환경변수 목록에 `PAYMENT_APIKEY`를 넣거나 `application-prod.yml`에 키 추가 |
| `ERROR env DB_URL` | 설정 값 `${DB_URL}`이 요구하는 환경변수가 목록에 없다 (CG002) | 배포 환경과 목록에 `DB_URL` 추가 |
| `WARN alarm.push.fcm-key` | 조건(`alarm.push.enabled`)이 켜지면 필요하다 (CG003) | prod에서 켜져 있으면 키 추가 |

규칙마다 설명이 있다: `config-guard explain CG002` 또는 [규칙 문서](../reference/rules/).

## 5. 오탐 다루기

이 도구가 모르는 출처(`spring.config.import`, Config Server, ConfigMap)에서 오는 키는 누락으로 보일 수 있다.

```kotlin
configGuard {
    ignoreKeys.set(listOf("vault.*"))                  // 모든 판정에서 뺀다
    extraFrameworkPrefixes.set(listOf("resilience4j")) // CG004(미사용 키)에서만 뺀다
}
```

`ignoreKeys`는 꼭 필요한 곳에만 쓴다. 에러를 지우려고 넓게 쓰면 진짜 누락도 가린다.

## 6. CI에 붙이기

[CI 연동](ci-integration.md)을 본다. `check`에 연결하려면:

```kotlin
tasks.check { dependsOn("configGuardCheck") }
```
