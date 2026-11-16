---
name: config-guard
description: Spring Boot 프로젝트에서 설정 키를 추가·변경했거나 배포 설정(application-*.yml, 환경변수 목록)을 바꿨을 때, 프로필별 설정 누락을 config-guard로 검사하고 수정 제안(Suggestion)을 적용해 종료 코드 0을 만든다. 배포 전 설정 점검, "Could not resolve placeholder" 기동 실패 예방에 쓴다.
---

# config-guard

코드가 요구하는 설정 키와 프로필별로 제공되는 키를 정적으로 비교하는 도구다. 설정 **값**은 읽지도 출력하지도 않는다.

## 언제 쓰나

- `@Value`, `@ConfigurationProperties`, `Environment.getProperty`로 새 설정 키를 추가하거나 이름을 바꿨을 때
- `application*.yml|properties`, 환경변수 목록 파일(예: `deploy/*.env.keys`)을 바꿨을 때
- 배포 전에 "운영에서 기동 실패할 설정 누락이 없나"를 확인할 때

## 실행

프로젝트에 Gradle 플러그인이 있으면:

```bash
./gradlew configGuardCheck          # 결과: build/reports/config-guard/report.json
```

CLI jar가 있으면 (stdout에 JSON만 나온다):

```bash
java -jar config-guard-all.jar check . --profile prod --env-file prod=deploy/prod.env.keys --format json
```

- `--profile`은 검사할 프로필마다 반복한다. 프로젝트의 `build.gradle.kts`의 `configGuard { profiles }`와 같은 값을 쓴다.
- 환경변수 목록 파일이 있으면 `--env-file <profile>=<path>`로 꼭 넘긴다. 없으면 환경변수 검사를 건너뛴다.

## 결과 해석

종료 코드: `0` 문제 없음 / `1` ERROR 있음 / `2` 명령이 잘못됨(옵션 확인) / `3` 설정 파일을 읽을 수 없음(stderr의 위치 확인).

JSON의 `findings[]`에서 `severity`를 본다.

| ruleId | 심각도 | 뜻 |
|---|---|---|
| CG001 | ERROR | 기본값 없이 요구하는 키가 그 프로필 설정에도 환경변수 목록에도 없다 |
| CG002 | ERROR | 설정 값의 `${NAME}`이 요구하는 환경변수가 목록에 없다 (`key`가 환경변수 이름) |
| CG003 | WARNING | 조건부(`@ConditionalOnProperty`/`@Profile`) 키가 없다 |
| CG004 | INFO | 설정에 있는데 코드가 쓰지 않는 키 |
| CG005 | INFO | 정적으로 분석하지 못한 곳 (SpEL, 동적 키) |

모르는 규칙은 `java -jar config-guard-all.jar explain <ruleId>`로 설명을 본다.

## 수정 절차

1. ERROR Finding마다 `suggestions`를 위에서부터 본다. 모든 ERROR에는 최소 1개가 있다.
   - `ADD_YAML_KEY`: `file`에 `target` 키를 추가한다. 값이 secret이거나 환경마다 다르면 `${ENV_NAME}` 플레이스홀더로 쓰고 `ENV_NAME`을 환경변수 목록 파일에 추가한다. secret이 아닌 고정값만 직접 쓴다.
   - `ADD_ENV_VAR`: 그 프로필의 환경변수 목록 파일에 `target` 이름을 한 줄 추가한다.
2. 다시 실행한다.
3. ERROR가 0이 될 때까지 반복한다. 종료 코드 0이 목표다.
4. 끝나면 사람에게 알린다: 어떤 키를 추가했는지, **배포 환경에 실제 값을 넣어야 하는 환경변수 이름 목록**.
5. CG003 WARNING은 조건이 그 프로필에서 켜지는지 확인한 뒤에만 고친다.

## 하지 말 것

- **secret 값을 파일에 쓰지 않는다.** API 키, 비밀번호, 토큰을 yml/properties/코드/환경변수 목록에 적지 않는다. 플레이스홀더와 이름만 쓴다. 실제 값이 필요하면 사람에게 요청한다.
- **에러를 없애려고 ignore를 남발하지 않는다.** `--ignore`/`ignoreKeys`는 이 도구가 읽지 않는 출처(Config Server, ConfigMap, `spring.config.import`)에서 오는 키에만, 가장 좁은 패턴으로 쓴다. `*`, `payment.*`처럼 넓게 쓰면 진짜 누락이 가려진다. 쓰면 이유를 사람에게 알린다.
- **코드에 빈 기본값을 넣어 누락을 숨기지 않는다** (`${payment.api-key:}`). 기동은 되지만 빈 값으로 운영에 나간다.
- `src/test`를 고쳐서 해결하려 하지 않는다. 이 도구는 `src/main`만 본다.
- `--fail-on never`로 종료 코드를 0으로 만들지 않는다.
