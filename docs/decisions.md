# 설계 결정

명세([spec.md](spec.md))가 모호하거나 판단이 필요했던 부분과, 그때 정한 해석을 한 줄씩 기록한다.
기준은 "Spring Boot 실제 동작과 같게"다. README의 "설계 결정과 한계" 섹션 재료로 쓴다.

형식: `- [마일스톤] 명세 위치 — 모호했던 점 → 정한 해석 (근거)`

<!-- 마일스톤 진행에 따라 아래에 추가한다. -->
- [M1] 3장 "Java 21로 빌드, 바이트코드 타깃 17" + 6.12 "JDK 17/21 매트릭스" — 매트릭스가 무엇을 바꾸는지(컴파일 JDK인지 실행 JDK인지) 정해져 있지 않았다 → 컴파일·린트는 JDK 21 툴체인으로 고정하고, 매트릭스는 테스트 실행 JDK만 바꾼다(`-PtestJavaVersion`). (Error Prone ≥2.43, google-java-format ≥1.29가 JDK 21에서만 돌고, 바이트코드 17이 실제로 JDK 17에서 도는지 확인할 수 있다.)
- [M1] 3장 라이브러리 표 — NullAway에 필요한 null 어노테이션이 표에 없었다 → `org.jspecify:jspecify`를 `compileOnly`로 추가했다 (사용자 승인). 런타임 의존성은 늘지 않는다.
- [M1] 3장 Spotless(google-java-format) — 최신 google-java-format 1.37.0이 Spotless 8.11에서 `InvocationTargetException`으로 실패했다(단독 실행은 정상) → 1.36.1로 고정하고 Dependabot에서 1.37.x를 제외했다.
- [M1] 7장 "M1 멀티 모듈" — 빈 cli/gradle-plugin 모듈을 미리 만들면 반쯤 만든 기능이 main에 노출된다 → M1에는 `core`만 두고, 공통 규칙은 `build-logic` 컨벤션 플러그인으로 묶어 이후 모듈이 한 줄로 붙게 했다. cli는 M6, gradle-plugin은 M7에서 추가한다.
- [M1] 5장 도메인 모델 — "모든 ERROR Finding은 최소 1개의 Suggestion"(6.10)을 어디서 보장할지 정해져 있지 않았다 → `Finding` 생성자에서 검사해 위반 객체를 만들 수 없게 했다. `Suggestion`도 `ADD_ENV_VAR`면 file이 null, `ADD_YAML_KEY`·`ADD_DEFAULT_VALUE`면 file이 필수인 규칙을 생성자에서 검사한다.
- [M1] 5장 규칙 표 — 규칙 ID를 문자열로만 다루면 오타를 막을 수 없다 → `Rule` enum(ID, 기본 심각도)을 두고, `Finding.ruleId`는 명세대로 String이되 생성자에서 알려진 ID인지 확인한다.
- [M2] 6.4 "환경변수 → 키: `_`를 `.`로, 소문자화" — 숫자 조각의 처리가 정해져 있지 않았다 → Spring SystemEnvironmentPropertyMapper와 같이 숫자 조각을 리스트 인덱스로 본다 (`MY_LIST_0_NAME` → `my.list[0].name`).
- [M2] 6.4 — 키를 제공할 수 있는 환경변수 이름이 하나뿐인지 정해져 있지 않았다 → Spring과 같이 대시 제거형(`PAYMENT_APIKEY`)과 레거시형(`PAYMENT_API_KEY`) 둘 다 인정한다.
- [M2] 6.4 "비교 시 대시 제거 후 같다고 본다" — 환경변수에서 온 키에만 적용하는지, 모든 비교에 적용하는지 모호했다 → 모든 비교를 "정규형에서 대시를 뺀 비교 키"로 한다. Spring도 키 출처와 관계없이 대시를 무시하고 비교한다. 단, 인덱스(`[...]`) 안은 그대로 둔다.
- [M2] 6.4 정규형 — `API_KEY`처럼 대문자만 있는 조각을 camelCase로 쪼개면 `a-p-i-k-e-y`가 된다 → 소문자가 없는 조각은 소문자화만 한다. 약어+단어(`URLPath`)는 `url-path`로 나눈다. 비교 키에는 영향이 없다(표시용).
- [M2] 8장 "동작을 바꾸는 커밋은 docs/reference도 함께" — 4장의 reference 목록(rules, cli, gradle-dsl, json-report)에 정규화 문서가 없었다 → 키 비교 규칙은 사용자가 결과를 이해하는 데 필요하므로 `docs/reference/key-normalization.md`를 추가했다.
- [M2 이후] 3장 "JUnit 5" — Dependabot이 JUnit 6으로 올리는 PR을 만들었다. 명세가 메이저 버전을 정한 라이브러리가 있다 → Gradle 의존성의 메이저 업데이트는 Dependabot이 제안하지 않게 했다. 메이저 업그레이드는 명세를 고친 뒤 사람이 한다. GitHub Actions는 메이저 업데이트도 받는다.
