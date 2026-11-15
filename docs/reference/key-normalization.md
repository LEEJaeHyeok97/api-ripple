# 키 정규화 (relaxed binding)

config-guard는 키를 비교하기 전에 [Spring Boot relaxed binding](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.relaxed-binding) 규칙으로 정규화한다. 그래서 코드와 설정 파일이 같은 키를 다르게 써도 같은 키로 본다.

구현: `core/.../normalize/RelaxedNames.java`. 아래 예시는 모두 `RelaxedNamesTest`의 실제 테스트 케이스다.

## 정규형

소문자 + kebab-case + 점 구분. 리포트에 나오는 키는 모두 이 형태다.

| 입력 | 정규형 |
|---|---|
| `payment.api-key` | `payment.api-key` |
| `payment.apiKey` | `payment.api-key` |
| `payment.api_key` | `payment.api-key` |
| `PAYMENT.API_KEY` | `payment.api-key` |
| `payment.URLPath` | `payment.url-path` |
| `app.s3Bucket` | `app.s3-bucket` |
| `a.b[0][1].cKey` | `a.b[0][1].c-key` |
| `a.map[Some.Key].v` | `a.map[Some.Key].v` (인덱스 안은 그대로) |

## 비교 규칙

두 키는 **정규형에서 대시를 뺀 형태가 같으면** 같은 키다. 환경변수는 대시를 표현할 수 없으므로 Spring도 이렇게 비교한다.

| 키 A | 키 B | 같은 키? |
|---|---|---|
| `payment.api-key` | `payment.apikey` | 예 |
| `payment.apiKey` | `payment.api_key` | 예 |
| `a.my-list[0].x-y` | `a.mylist[0].xy` | 예 |
| `a.map[k-ey]` | `a.map[key]` | 아니오 (인덱스 안의 대시는 유지) |

## 환경변수 → 키

`_`를 `.`로 바꾸고 소문자로 만든다. 숫자만 있는 조각은 리스트 인덱스가 된다.

| 환경변수 | 키 |
|---|---|
| `PAYMENT_APIKEY` | `payment.apikey` (= `payment.api-key`) |
| `SERVER_PORT` | `server.port` |
| `MY_LIST_0_NAME` | `my.list[0].name` |
| `DB__URL` | `db.url` (빈 조각은 무시) |

## 키 → 환경변수 후보

코드가 요구하는 키를 어떤 환경변수가 제공할 수 있는지 판단할 때 쓴다. Spring과 같이 두 가지 이름을 모두 인정한다.

| 키 | 후보 |
|---|---|
| `payment.api-key` | `PAYMENT_APIKEY`, `PAYMENT_API_KEY` (레거시) |
| `my.list[0].name` | `MY_LIST_0_NAME` |

## 한계

- 대문자로만 된 조각(`API_KEY`)은 camelCase로 나누지 않고 소문자화만 한다. 비교에는 영향이 없다.
- `URLPath`처럼 약어와 단어가 붙은 이름은 `url-path`로 나눈다. Spring의 표시 형태와 다를 수 있지만, 비교는 대시를 빼고 하므로 결과는 같다.
