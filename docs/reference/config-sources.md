# 설정 소스

config-guard가 "환경별로 실제 제공되는 키"를 어떻게 모으는지 설명한다. 기준은 Spring Boot의 ConfigData 동작이다.

구현: `core/.../source/`. 아래 예시는 모두 `core/src/test/resources/fixtures/`의 실제 파일이다.

## 읽는 파일

리소스 디렉터리(기본 `src/main/resources`) 아래 두 위치를 본다. Spring Boot 기본 위치와 같다.

| 위치 | 파일 |
|---|---|
| `src/main/resources/` | `application.yml`, `application.yaml`, `application.properties`, `application-{profile}.*` |
| `src/main/resources/config/` | 위와 같음. 루트보다 우선한다. |

뒤에 읽는 파일이 앞의 값을 덮어쓴다. 순서: 루트 → `config/`. 같은 위치에서는 `yml` → `yaml` → `properties`.

## 프로필 결정

| 출처 | 프로필 |
|---|---|
| `application.yml` | 기본 (모든 프로필에 적용) |
| `application-prod.yml` | `prod` |
| 문서 안의 `spring.config.activate.on-profile: prod` | `prod` |

멀티 도큐먼트는 yml의 `---`, properties의 `#---`(또는 `!---`)로 나눈다.

`on-profile` 값은 다음 형식을 지원한다.

| 값 | 의미 |
|---|---|
| `prod` | prod일 때 |
| `prod, staging` / `prod \| staging` / `[dev, local]` | 하나라도 맞으면 |
| `!prod`, `prod & cloud`, 괄호 | **지원하지 않음.** 그 문서는 분석에서 빼고 CG005(분석 불가)로 알린다. |

## 프로필 병합

프로필 P의 유효 키 집합 = 기본 문서 ∪ P에 해당하는 문서 ∪ include/group 프로필의 문서.

- `spring.profiles.include`: P로 기동할 때 적용되는 문서에 적힌 값을 **1단계만** 따라간다. include된 프로필의 문서가 다시 include하는 것은 따라가지 않는다.
- `spring.profiles.group.P`: 어느 문서에 적혀 있든 P의 그룹 멤버를 **1단계만** 활성화한다.
- 덮어쓰기 순서: 기본 파일의 문서(파일 안에서는 위→아래) → include 프로필 파일 → P 파일 → group 멤버 파일.
- `default` 프로필은 "활성 프로필이 없을 때"를 뜻한다. `application-default.yml`과 `on-profile: default` 문서도 적용된다.

예시 (`fixtures/profiles`):

```yaml
# application.yml
app:
  name: demo
  feature:
    timeout: 30
spring:
  profiles:
    group:
      prod: prod-db, prod-mq
---
spring:
  config:
    activate:
      on-profile: prod
app:
  feature:
    timeout: 60
  region: ap-northeast-2
```

```properties
# application-dev.properties
spring.profiles.include=common
app.feature.timeout=5
```

| 대상 | 활성 프로필 | `app.feature.timeout`을 제공하는 곳 |
|---|---|---|
| `prod` | prod, prod-db, prod-mq | `application.yml:16` (prod 문서) |
| `dev` | common, dev | `application-dev.properties:2` |
| `default` | default | `application.yml:4` |

## 평탄화

YAML 중첩은 점 표기로, 리스트는 인덱스로 펼친다. 키는 [정규화](key-normalization.md)된다.

| YAML | 키 |
|---|---|
| `payment: { apiKey: … }` | `payment.api-key` |
| `servers: [ { host: … } ]` | `servers[0].host` |
| `"[my.key]": …` | `map[my.key]` (괄호 키는 점 없이 붙인다) |
| `empty-list: []` | `empty-list` (키를 제공한다) |
| `empty-map: {}` | (키 없음) |
| `<<: *base` | 병합 대상의 키를 현재 위치에 펼친다 |

## 값 안의 플레이스홀더

값 자체는 저장하지 않는다. 값 안의 `${NAME}` 중 **기본값이 없는 것**의 이름만 기록한다. 이 키는 "제공된 것"으로 보되 `NAME` 환경변수를 요구한다.

예시 (`fixtures/env-placeholder`):

```yaml
spring:
  datasource:
    url: ${DB_URL}            # DB_URL 필요
    username: ${DB_USER:app}  # 기본값 있음 → 필요 없음
mail:
  sender: noreply-${MAIL_DOMAIN}       # MAIL_DOMAIN 필요
cache:
  ttl: ${CACHE_TTL:${DEFAULT_TTL}}     # DEFAULT_TTL 필요 (기본값 안의 플레이스홀더도 분석)
```

`\${...}`로 이스케이프한 것과 이름 자체가 동적인 `${${env}.url}`은 무시한다.

## 환경변수 이름 목록

프로필별로 파일을 지정한다. 두 형식을 한 파일에 섞어 써도 된다.

```bash
# fixtures/env-placeholder/deploy/prod.env.keys — 이름 목록 형식
DB_URL
MAIL_DOMAIN
```

```bash
# fixtures/env-placeholder/deploy/dev.env — .env 형식. 이름만 읽고 값은 버린다.
DB_URL=jdbc:postgresql://localhost:5432/app
export MAIL_DOMAIN=example.test
DEFAULT_TTL=60
```

- `#`로 시작하는 줄과 빈 줄은 무시한다. `export ` 접두어를 허용한다.
- 이름은 `=` 또는 공백 앞까지다. 그 뒤(값, 줄 끝 주석)는 버린다.

## 값 보호

- 설정 값은 플레이스홀더 이름을 뽑는 데만 쓰고 바로 버린다. 결과 객체에 남지 않는다.
- 예외: 문서 활성화를 정하는 `spring.config.activate.on-profile`, `spring.profiles.include`, `spring.profiles.group.*`의 값(프로필 이름)은 읽는다.
- YAML 문법 오류는 `파일:라인`만 보고한다. 파서 원문 메시지는 문제가 된 줄(값 포함)을 인용할 수 있어서 쓰지 않는다.
