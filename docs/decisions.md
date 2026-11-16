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
- [M3] 6.2 "기본 위치: src/main/resources/" — Spring 기본 위치 중 `classpath:/config/`를 볼지 정해져 있지 않았다 → 본다 (사용자 승인). 빼면 config/에만 있는 키를 "누락"으로 오탐한다. 우선순위는 Spring과 같이 루트 < config/, 같은 위치에서 yml < yaml < properties.
- [M3] 6.2 "멀티 도큐먼트 yml" — properties의 멀티 도큐먼트(`#---`)는 명세에 없었다 → 지원한다 (사용자 승인). Spring 2.4+ 기본 동작이고, 빼면 프로필 전용 문서의 키가 모든 프로필에 들어가 누락을 놓친다.
- [M3] 6.2 on-profile — 프로필 식(`!`, `&`, `|`, 괄호) 처리가 정해져 있지 않았다 → `,`와 `|`는 OR로 지원하고, `!`·`&`·괄호를 쓴 문서는 분석에서 빼고 CG005로 알린다. 부분 지원으로 틀린 키 집합을 만드는 것보다 "분석 불가"가 낫다.
- [M3] 6.2 "include / group 1단계" — 순서와 출처가 정해져 있지 않았다 → include는 대상 프로필로 기동할 때 적용되는 문서에서만 읽고 대상보다 먼저 활성화한다. group은 어느 문서에 있든 읽고 대상 뒤에 활성화한다. 둘 다 재귀하지 않는다.
- [M3] 6.2 프로필 이름 "default" — `application.yml`의 프로필 이름과 Spring의 `default` 프로필(활성 프로필이 없을 때)이 겹쳤다 → 내부적으로 파일 프로필 없음(null)과 `default` 프로필을 구분한다. 리포트의 `ProvidedKey.profile`은 기본 파일이면 `default`다.
- [M3] 6.2 "${ENV_NAME}(기본값 없음)" — 설정 값의 `${a:${b}}`를 어떻게 볼지 정해져 있지 않았다 → 6.1의 @Value 규칙과 같게 기본값 안을 재귀 분석해 `b`를 요구 환경변수로 기록한다.
- [M3] 6.2 YAML 평탄화 — 빈 값 처리가 정해져 있지 않았다 → Spring과 같이 빈 리스트(`[]`)와 null 값은 키를 제공하고, 빈 매핑(`{}`)은 키를 만들지 않는다. 병합 키(`<<`)는 펼친다.
- [M3] 2장 "값은 절대 출력하지 않는다" — 파서 예외 메시지가 문제 줄을 인용할 수 있다 → YAML 문법 오류는 위치만 담은 `ConfigSourceException`으로 바꾼다. 문서 활성화용 구조 키(on-profile, include, group)의 값만 예외적으로 읽는다(프로필 이름).
- [M3] 5장 도메인 모델 — "분석 불가"를 담을 타입이 없었다 → `model.Unanalyzable(key, location, message)`을 추가했다. 소스(지원하지 않는 프로필 식)와 M4 추출(SpEL, 동적 키)이 함께 쓰고, M5에서 CG005가 된다.
- [M4] 6.1 "@NotNull 있고 @Validated → REQUIRED" vs "초기값 있음 → OPTIONAL" — 둘 다 해당하면 어느 쪽인지 정해져 있지 않았다 → OPTIONAL. Spring에서는 초기값이 검증을 통과해 기동이 성공한다.
- [M4] 6.1 "중첩 클래스 타입 필드 → 재귀" — 중첩 쪽 필드의 `@NotBlank`도 REQUIRED인지 모호했다 → 중첩 필드에 `@Valid`가 있을 때만 검증이 이어진다고 본다. Spring Boot 문서: 프로퍼티가 하나도 없는 중첩 객체를 검증하려면 `@Valid`가 필요하다.
- [M4] 6.1 "record / 생성자 바인딩" — 일반 클래스에서 생성자 바인딩을 언제 쓰는지 정해져 있지 않았다 → Spring Boot 3 규칙: `@ConstructorBinding` 생성자, 또는 `@Autowired`가 없는 유일한 파라미터 있는 생성자. JavaBean 바인딩은 setter 유무를 보지 않고 static이 아닌 필드를 모두 본다 (Lombok `@Data`/`@Setter`가 흔해서).
- [M4] 6.1 "Map, List 타입" — 다른 컬렉션 타입이 정해져 있지 않았다 → Map·List·Set·Collection·Iterable과 흔한 구현체, 배열, `Properties`를 같은 규칙(prefix만 OPTIONAL)으로 다룬다. 열거형은 값 하나로 본다.
- [M4] 6.1 "위 어노테이션이 붙은 클래스/메서드" — 바깥 클래스의 조건도 적용되는지 모호했다 → 키를 요구하는 지점을 감싸는 모든 선언(메서드, 생성자, 클래스, 바깥 클래스)의 조건을 본다. 조건이 여러 개면 바깥쪽부터 쉼표로 이어 이유로 쓴다.
- [M4] 6.5 UNUSED_KEY — `@ConditionalOnProperty(name = "alarm.push.enabled")`가 읽는 키가 "코드가 요구하지 않는 키"로 잡힐 수 있었다 → 조건 키를 OPTIONAL로 기록해 CG004 오탐을 막는다.
- [M4] 6.1 Environment — 호출 대상이 정말 Environment인지 판단 기준이 없었다 → 심볼 해석 없이 선언 타입의 단순 이름으로 판단한다(Environment 계열 8개 타입 + `getEnvironment()` 호출). `var`나 다른 타입이면 건너뛴다. `System.getProperty`, `Properties.getProperty` 오탐을 막기 위해서다.
- [M4] 6.1 — `lombok.Value`와 Spring `@Value`가 같은 단순 이름을 쓴다 → 파일이 `lombok.Value`를 import하면 그 파일의 `@Value`는 무시한다.
- [M4] 6.1 — 문법 오류가 있는 Java 파일 처리가 정해져 있지 않았다 → 그 파일만 빼고 CG005로 알린다. 파서 메시지는 코드 조각을 인용할 수 있어서 쓰지 않는다.
- [M4] 3장 — `javaparser-symbol-solver-core`를 쓰라고 했지만 심볼 해석은 이름 기반 휴리스틱으로 충분했다 → 의존성은 명세대로 두되 심볼 솔버 API는 쓰지 않는다. 외부 라이브러리 클래스(Spring 등)가 클래스패스에 없어서 해석이 어차피 실패한다.
- [M5] 6.3 "환경변수 파일을 지정하지 않은 프로필은 INFO로 알린다" — 규칙 ID(CG001~005) 중 맞는 것이 없었다 → Finding이 아닌 `notices[]`(code `ENV_CHECK_SKIPPED`)로 분리했다 (사용자 승인). 규칙 ID의 의미를 흐리지 않기 위해서다.
- [M5] 6.5 UNUSED_KEY — 기본 파일의 미사용 키를 프로필마다 반복할지 정해져 있지 않았다 → 사용 여부는 코드로만 정해져 프로필과 무관하므로 공통(`profile: "*"`)으로 한 번만 보고한다. 프로필 파일의 미사용 키는 그 프로필 아래에 보고한다.
- [M5] 6.5 "분석 불가 패턴 INFO" — 어느 프로필에 넣을지 정해져 있지 않았다 → 공통(`*`)으로 한 번만 보고한다. 콘솔·Markdown에서는 "모든 프로필 공통" 섹션으로 프로필 섹션 뒤에 둔다.
- [M5] 6.5 MISSING_ENV_VAR — 플레이스홀더가 환경변수가 아니라 다른 설정 키를 가리킬 수 있다(`${base.url}`) → 같은 이름의 설정 키가 유효 집합에 있으면 충족된 것으로 본다. Spring도 플레이스홀더를 모든 프로퍼티 소스에서 찾는다.
- [M5] 6.5 — 같은 키를 여러 곳에서 다른 수준으로 요구할 때 처리가 정해져 있지 않았다 → 가장 강한 요구(REQUIRED > CONDITIONAL > OPTIONAL)의 첫 위치로 한 번만 보고한다.
- [M5] 6.5 "환경변수에도 없음" — 환경변수 목록을 지정하지 않은 프로필의 REQUIRED 키 처리가 모호했다 → 설정 파일에 없으면 CG001 ERROR로 본다. 목록이 없으면 "환경변수로 준다"는 근거가 없기 때문이다.
- [M5] 6.10 "모든 ERROR에 Suggestion" — CG001에 무엇을 제안할지 정해져 있지 않았다 → `ADD_YAML_KEY`(그 프로필 파일. 없으면 `application-<프로필>.yml`)와 `ADD_ENV_VAR`(대시 제거형 이름)를 함께 준다. `ADD_DEFAULT_VALUE`는 누락을 가리는 수정이 될 수 있어 자동으로 제안하지 않는다.
- [M5] 6.6 콘솔 예시 — 키 열 너비가 명세에 없었다 → 섹션 안 가장 긴 키 + 4칸. 명세 예시와 글자 단위로 같다(테스트로 고정).
- [M5] 6.10 "실제 출력이 스키마를 통과하는지 테스트" — 3장 표에 JSON Schema 검증기가 없다 → 사용자가 자리에 없어 라이브러리 추가를 묻지 못했다. 스키마가 쓰는 키워드만 지원하는 테스트 전용 검증기(`MiniJsonSchema`)를 만들고, 모르는 키워드가 나오면 실패하게 했다. **이후 사용자 결정으로 networknt json-schema-validator(테스트 전용)로 바꾸고 직접 만든 검증기는 지웠다.** 3.x는 Jackson 3(`tools.jackson`)을 써서, Jackson 2를 쓰는 이 프로젝트에 맞는 2.x(2.0.8)를 쓴다.
- [M5] 6.6 Markdown — 공통 섹션에 "✅ 문제 없음" 줄을 붙일지 모호했다 → 공통 섹션에는 ERROR가 나오지 않으므로 상태 줄 없이 INFO만 접어서 보인다.
- [M6] 6.9 CLI 옵션 — 명세의 CLI 옵션에 Gradle DSL의 `extraFrameworkPrefixes`에 해당하는 것이 없어서 "같은 입력에 같은 결과"를 만들 수 없었다 → `--framework-prefix`를 추가했다.
- [M6] 6.9 `--env-file prod=deploy/prod.env.keys` — 상대 경로의 기준(현재 디렉터리 vs PATH)이 정해져 있지 않았다 → 일반적인 CLI 관례대로 현재 디렉터리 기준. 문서에 적었다.
- [M6] 6.9 종료 코드 2 "사용법 오류"의 범위가 정해져 있지 않았다 → 필수 옵션 누락, 없는 경로·파일, 모르는 형식·규칙 ID, 그리고 `--env-file`의 프로필이 `--profile`에 없는 경우(오타 방지)를 2로 본다. YAML 문법 오류처럼 입력 내용이 잘못된 경우는 3(내부 오류)이다.
- [M6] 3장 "CLI fat jar" — shadow 플러그인은 표에 없다 → Gradle 기본 `Jar` 태스크로 런타임 클래스패스를 합친 `fatJar`(`config-guard-<version>-all.jar`)를 만든다. 서명 파일과 `module-info`는 뺀다.
- [M6] 6.10 explain "빌드 시 리소스로 포함" → `cli`의 `processResources`가 `docs/reference/rules/*.md`를 그대로 복사한다. `explain` 출력이 문서 파일과 바이트 단위로 같은지 테스트한다.
- [M6] 6.9 `--format` 값 — 대소문자 구분이 정해져 있지 않았다 → 대소문자를 무시한다 (`--format JSON`도 허용).
- [M7] 6.15 "gradle-plugin: TestKit으로 samples/ 프로젝트에 대해" — samples/는 M8에서 만든다(7장 순서) → M7의 TestKit 테스트는 core fixture를 임시 프로젝트로 복사해 쓰고, samples/ 대상 테스트는 M8에서 추가한다.
- [M7] 6.7 "입력: envFiles" — 파일 내용뿐 아니라 "어느 파일이 어느 프로필인지"도 결과에 영향을 준다 → 파일은 `@InputFiles`, 프로필 → 상대 경로 맵은 `@Input`으로 함께 선언했다. 실제 경로(`@Internal`)는 태스크 실행에만 쓴다.
- [M7] 6.7 태스크 실패 — 실패하기 전에 리포트를 쓸지 정해져 있지 않았다 → 리포트(md, json)를 먼저 쓰고 실패한다. 6.8 Action이 "실패해도 리포트 생성까지 진행"하고 코멘트를 달아야 하기 때문이다.
- [M7] 6.7 증분 빌드 — 태스크를 `@CacheableTask`로 했다. 입력이 같으면 UP-TO-DATE/FROM-CACHE가 되어 콘솔 리포트가 다시 나오지 않는다. 실패한 실행은 캐시되지 않으므로 오류는 매번 다시 보인다.
- [M7] 6.7 `profiles` "필수" — 비었을 때 동작이 정해져 있지 않았다 → 태스크 실행 시 설정 방법을 알려 주며 실패한다. 설정 단계에서 실패시키면 `configGuardCheck`를 쓰지 않는 빌드까지 깨진다.
- [M8] 6.8 Action 경로 `LEEJaeHyeok97/config-guard/action@v0` — 저장소 이름이 `api-ripple`이다 → `LEEJaeHyeok97/api-ripple/action@v0`로 쓴다 (사용자 확인). `v0` 태그는 릴리스 때 사람이 만든다.
- [M8] 6.8 "Gradle 태스크의 종료 코드를 그대로 Action 결과로" — composite action은 단계 하나가 실패하면 뒤 단계(코멘트)가 돌지 않는다 → 첫 단계에서 종료 코드를 출력값으로 저장하고, 코멘트를 단 뒤 마지막 단계에서 그 코드로 `exit`한다. `exit-code` 출력도 준다.
- [M8] 6.8 PR 코멘트 권한 — 포크 PR은 토큰에 쓰기 권한이 없다 → 코멘트 실패는 경고로만 남기고 검사 결과(종료 코드)는 그대로 돌려준다. 리포트는 잡 요약(Step Summary)에도 붙인다.
- [M8] 6.8 Action 입력 — 사용자 입력을 `run:` 스크립트에 `${{ }}`로 직접 넣으면 스크립트 주입이 가능하다 → 모든 입력을 환경변수로 넘긴다.
- [M8] 9장 "samples/ 앱에서 ERROR 2건, WARNING 1건" — 프로필이 정해져 있지 않았다 → dev, prod를 검사하고 dev는 문제 없음, prod에서 정확히 ERROR 2(CG001 `payment.api-key`, CG002 `DB_URL`), WARNING 1(CG003 `alarm.push.fcm-key`), INFO 0이 나오게 만들었다. `src/test`에 넣은 키가 보고되지 않는지도 같은 테스트로 확인한다.
- [M8] 6.12 "의존성 버전은 전부 libs.versions.toml에서만" — 샘플은 독립 빌드라 카탈로그를 따로 둘 수 있었다 → 샘플이 루트 카탈로그를 `from(files("../../gradle/libs.versions.toml"))`로 공유하고, Spring Boot 플러그인 버전도 루트 카탈로그 `[plugins]`에 둔다. 플러그인은 `includeBuild("../..")`로 소스에서 가져온다.
- [M8] 6.12 e2e — 샘플은 의도적으로 실패한다 → e2e 잡은 "실패해야 통과"다: CLI와 Action 모두 종료 코드 1, 요약 숫자 일치, 두 JSON이 바이트 단위로 같은지(`diff`) 확인한다.
- [M9] 6.14 "Plugin Portal 배포는 설정만" — 플러그인이 `core` 프로젝트에 의존하는데 Plugin Portal은 플러그인 아티팩트만 받는다 → `core` 클래스를 플러그인 jar에 넣고, core의 외부 의존성은 플러그인 POM에 선언한다. shadow 플러그인은 표에 없어 Gradle 기본 `Jar`로 합친다. `publishToMavenLocal` 후 별도 프로젝트에서 적용되는 것을 확인했다.
- [M9] 6.13 "30초 설치" — 아직 릴리스(Release jar, Plugin Portal)가 없어 명세의 한 줄 설치가 동작하지 않는다 → README에 "릴리스 전" 방법(소스 빌드, `publishToMavenLocal` + `mavenLocal()`)과 릴리스 후 방법을 함께 적었다. 동작하지 않는 설치 방법만 보여 주지 않기 위해서다.
- [M9] 6.14 릴리스 버전 — 빌드 버전이 `0.1.0-SNAPSHOT`으로 고정돼 있었다 → 릴리스 워크플로가 태그(`v0.1.0`)에서 `-PreleaseVersion=0.1.0`을 넘긴다. 로컬 기본값은 SNAPSHOT 그대로다.
- [M9] 6.10 Agent Skill 형식 — 형식이 정해져 있지 않았다 → Claude Code Agent Skill 형식(frontmatter `name`, `description` + 본문)으로 쓰고, `.claude/skills/`에 복사해 쓰는 방법을 가이드에 적었다.
- [M9] 6.13 "문서의 코드 예시는 fixture에서 가져온 실제 코드" — 가이드의 설치 스니펫(build.gradle.kts, 워크플로)은 fixture에 없다 → 분석 대상 코드·설정·출력 예시는 fixture와 samples/demo의 실제 파일과 실제 출력만 쓰고, 설치·설정 스니펫은 TestKit 테스트와 e2e 워크플로에서 실제로 쓰는 형태를 따른다.
- [M9] 플러그인 artifactId — 기본값이 프로젝트 이름(`gradle-plugin`)이라 너무 일반적이다 → `config-guard-gradle-plugin`으로 바꿨다. 플러그인 ID는 그대로다.
- [M10] 6.11 "같은 에이전트" — 어떤 에이전트인지 정해져 있지 않았다 → 이 저장소를 만든 것과 같은 Claude Code를 헤드리스(`claude -p`)로 쓴다. 과제당 모드별 1회, `--max-turns 30`, 허용 도구를 제한한다.
- [M10] 6.11 "Skill 없이" — 도구(CLI)도 없이인지 모호했다 → 두 모드 모두 프로젝트 루트에 `./config-guard`를 두고, Skill 파일 유무만 다르게 한다. Skill의 효과만 떼어 보기 위해서다.
- [M10] 6.11 지시문 — 명세 문구만으로는 환경변수 목록 파일의 역할이 정해지지 않아, 시험 실행에서 에이전트가 목록에 이름을 넣지 않고 멈췄다(회고 참고) → 두 모드에 똑같이 "목록에 이름을 추가하면 운영팀이 값을 등록한다"를 덧붙였다.
- [M10] 6.11 "잘못된 수정" 판정 — 기준이 정해져 있지 않았다 → secret 이름(`key/secret/password/token`) 설정에 플레이스홀더가 아닌 값, 코드에 기본값 추가, 환경변수 목록에 값 기록, `--ignore`/`--fail-on never` 사용을 잘못된 수정으로 센다.
- [M10] 평가 산출물 — transcript에는 로컬 경로와 긴 로그가 들어간다 → `evals/results/`는 커밋하지 않고, 요약 표만 `evals/RESULTS.md`로 커밋한다. 실행 스크립트는 함께 커밋해 재현할 수 있게 한다.
