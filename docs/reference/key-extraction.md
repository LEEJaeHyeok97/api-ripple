# 키 추출

config-guard가 "코드가 요구하는 키"를 어떻게 찾는지 설명한다. 애플리케이션을 실행하지 않고 `src/main/java`를 JavaParser로 읽는다. `src/test`는 분석하지 않는다.

구현: `core/.../extract/`. 아래 예시는 모두 `core/src/test/resources/fixtures/`의 실제 코드다.

## 요구 수준

| 수준 | 뜻 | 없을 때 |
|---|---|---|
| REQUIRED | 없으면 기동이 실패한다 | CG001 ERROR |
| CONDITIONAL | 조건이 맞을 때만 필요하다 | CG003 WARNING |
| OPTIONAL | 기본값이 있거나 null로 바인딩된다 | 보고하지 않음 |

## `@Value`

`fixtures/value-basic/.../PaymentClient.java`:

```java
@Value("${payment.api-key}")                               // payment.api-key: REQUIRED
@Value("${payment.timeout:30}")                            // OPTIONAL
@Value("${payment.region:}")                               // OPTIONAL (빈 기본값도 기본값)
@Value("${payment.url:${payment.fallback-url}}")           // url OPTIONAL, fallback-url REQUIRED
@Value("${payment.retry:${payment.default-retry:3}}")      // 둘 다 OPTIONAL
@Value("https://${payment.host}/v1/${payment.version:v1}") // host REQUIRED, version OPTIONAL
public PaymentClient(@Value("${payment.merchantId}") String merchantId) // payment.merchant-id REQUIRED
```

- 필드, 생성자·메서드 파라미터, 메서드에 붙은 `@Value` 모두 본다.
- `lombok.Value`를 import한 파일의 `@Value`는 무시한다.

## `Environment`

`fixtures/unanalyzable/.../DynamicConfig.java`:

```java
env.getRequiredProperty("app.env-required")                       // REQUIRED
this.env.getProperty("app.env-optional", "fallback")              // OPTIONAL
context.getEnvironment().getProperty("app.context-key")           // OPTIONAL
System.getProperty("user.home")                                   // 무시 (Environment가 아님)
```

호출 대상이 `Environment`, `ConfigurableEnvironment`, `PropertyResolver` 등 Spring 타입으로 선언된 변수·필드·파라미터이거나 `getEnvironment()` 호출일 때만 인정한다. `var`로 선언한 변수는 타입을 알 수 없어서 건너뛴다.

## `@ConfigurationProperties`

`fixtures/config-properties/.../MailProperties.java`:

```java
@Validated
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {
  @NotBlank private String senderAddress;      // app.mail.sender-address: REQUIRED
  private int timeout = 30;                     // OPTIONAL (초기값)
  private String replyTo;                       // OPTIONAL (null 바인딩)
  @NotNull private Integer maxRetries = 3;      // OPTIONAL (초기값이 검증을 통과)
  private Smtp smtp = new Smtp();               // app.mail.smtp.*: @Valid가 없어 검증이 이어지지 않음 → OPTIONAL
  @Valid private Auth auth;                     // app.mail.auth.username: REQUIRED (@NotEmpty)
  private Map<String, String> headers;          // app.mail.headers: OPTIONAL, 하위 키 열거 안 함
  private List<String> recipients;              // app.mail.recipients: OPTIONAL
}
```

`fixtures/config-properties/.../StorageProperties.java` (record = 생성자 바인딩):

```java
@Validated
@ConfigurationProperties("app.storage")
public record StorageProperties(
    @NotBlank String bucketName,                    // REQUIRED
    @DefaultValue("ap-northeast-2") String region,  // OPTIONAL
    Duration timeout) {}                            // OPTIONAL
```

| 규칙 | 판정 |
|---|---|
| 클래스에 `@Validated` + 멤버에 `@NotNull`/`@NotBlank`/`@NotEmpty` + 기본값 없음 | REQUIRED |
| 초기값 또는 `@DefaultValue` | OPTIONAL |
| 그 외 | OPTIONAL |
| 소스에 있는 클래스·record 타입 | 재귀 (`x.nested.field`). 검증은 `@Valid`가 붙은 필드로만 이어진다 |
| `Map`, `List`, `Set`, `Collection`, 배열 | prefix만 OPTIONAL로 기록 |
| 열거형, JDK·라이브러리 타입 | 값 하나 |

바인딩 방식은 Spring Boot 3과 같다. record, `@ConstructorBinding` 생성자, `@Autowired`가 없는 유일한 파라미터 있는 생성자는 생성자 파라미터를, 그 밖에는 static이 아닌 필드를 본다.

## 조건부 강등

키를 요구하는 지점을 감싸는 메서드·생성자·클래스(바깥 클래스 포함)에 `@ConditionalOnProperty`나 `@Profile`이 있으면 REQUIRED를 CONDITIONAL로 낮춘다.

`fixtures/conditional/`:

```java
@ConditionalOnProperty(prefix = "alarm.push", name = "enabled", havingValue = "true")
public class FcmSender {
  @Value("${alarm.push.fcm-key}")      // CONDITIONAL: @ConditionalOnProperty(alarm.push.enabled)
  private String fcmKey;
}

@Bean
@Profile("prod")
public S3Store s3Store(@Value("${storage.s3.bucket}") String bucket)  // CONDITIONAL: @Profile(prod)
```

`@ConditionalOnProperty`가 읽는 키(`alarm.push.enabled`)는 OPTIONAL로 기록한다. 그래야 그 키가 CG004(미사용 키)로 잡히지 않는다.

## 상수와 동적 키

키 표현식은 문자열 리터럴, 리터럴끼리의 `+`, **같은 파일 안의** `static final` 상수(인터페이스 필드 포함)까지만 해석한다.

```java
private static final String KEY = "app.const-key";
@Value("${" + KEY + "}")              // app.const-key: REQUIRED (같은 파일)
@Value(Constants.EXTERNAL)            // CG005: 다른 파일의 상수
```

## 분석 불가 (CG005)

추측하지 않고 CG005 INFO로 알린다.

| 패턴 | 메시지 |
|---|---|
| `@Value("#{...}")` | `@Value: SpEL(#{...}), 분석 불가` |
| `@Value(다른파일.상수)`, 해석할 수 없는 식 | `@Value: 동적 키, 분석 불가` |
| `@Value("${${tenant}.url}")` | `@Value: 키 이름 안에 플레이스홀더(동적 키), 분석 불가` |
| `env.getProperty(name)` | `Environment.getProperty: 동적 키, 분석 불가` |
| `@ConfigurationProperties(prefix = 해석 불가)` | `@ConfigurationProperties: 동적 prefix, 분석 불가` |
| 문법 오류가 있는 파일 | `Java 파싱 실패, 이 파일은 분석에서 제외` |
