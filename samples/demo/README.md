# demo

config-guard 데모용 Spring Boot 앱. **운영(prod) 설정을 일부러 빠뜨렸다.**

```bash
cd samples/demo
./gradlew configGuardCheck
```

기대 결과 (CI e2e가 이 숫자를 검사한다):

```
config-guard: dev
  문제 없음
config-guard: prod
  ERROR  payment.api-key       PaymentClient.java:14  @Value, 기본값 없음
  ERROR  env DB_URL            application-prod.yml:7  spring.datasource.url이 요구
  WARN   alarm.push.fcm-key    FcmSender.java:13  @ConditionalOnProperty(alarm.push.enabled)
2 errors, 1 warning, 0 info
```

| 무엇이 빠졌나 | 규칙 | 고치는 법 |
|---|---|---|
| prod에 `payment.api-key` 없음 (dev는 `PAYMENT_APIKEY` 환경변수로 제공) | CG001 | `deploy/prod.env.keys`에 `PAYMENT_APIKEY` 추가 |
| prod `spring.datasource.url: ${DB_URL}`인데 `DB_URL`이 prod 환경변수 목록에 없음 | CG002 | `deploy/prod.env.keys`에 `DB_URL` 추가 |
| prod에서 `alarm.push.enabled: true`인데 `alarm.push.fcm-key` 없음 | CG003 | `application-prod.yml`에 `alarm.push.fcm-key: ${FCM_KEY}`, 목록에 `FCM_KEY` 추가 |

플러그인은 `settings.gradle.kts`의 `includeBuild("../..")`로 이 저장소 소스에서 바로 가져온다.
