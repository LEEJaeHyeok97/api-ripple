# config-guard

[![CI](https://github.com/LEEJaeHyeok97/api-ripple/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/LEEJaeHyeok97/api-ripple/actions/workflows/ci.yml)
[![status](https://img.shields.io/badge/status-pre--alpha%20(M8)-orange)](#roadmap)
[![license](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

Spring Boot 애플리케이션의 **환경별 설정 누락을 배포 전에 CI에서 잡아내는** 정적 분석 도구.

> **현재 상태:** 개발 초기 단계라 아직 사용할 수 있는 기능이 없다. 진행 상황은 [Roadmap](#roadmap)에 있다.

## 해결하려는 문제

`PaymentClient`가 `@Value("${payment.api-key}")`로 API 키를 주입받는다고 하자. 개발자는 `application-dev.yml`에만 이 키를 넣고 `application-prod.yml`에는 빠뜨렸다. 컴파일, 테스트, 이미지 빌드, 배포 파이프라인이 모두 초록색으로 통과한다. 그런데 운영 서버에서 애플리케이션이 기동하다가 `Could not resolve placeholder 'payment.api-key'`를 내고 죽는다. 롤링 배포라면 새 파드가 계속 재시작되고, 누군가 로그를 열어 볼 때까지 원인이 드러나지 않는다. 문제는 코드가 아니라 **"코드가 요구하는 키"와 "그 환경이 실제로 제공하는 키"의 차이**에 있다. 그런데 이 차이를 배포 전에 확인하는 단계가 없다.

## 동작 방식

코드(`@Value`, `@ConfigurationProperties`, `Environment`)가 요구하는 키와 프로필별 설정 파일·환경변수 목록이 제공하는 키를 **애플리케이션을 띄우지 않고 정적으로 비교**한다. 누락이 있으면 CI를 실패시킨다.

## 출력 예시 (목표)

아래는 [명세](docs/spec.md#66-리포트-report)에 정의된 목표 출력이다. 아직 구현 전이다.

```
config-guard: prod
  ERROR  payment.api-key       PaymentClient.java:23  @Value, 기본값 없음
  ERROR  env DB_URL            application-prod.yml:4  spring.datasource.url이 요구
  WARN   alarm.push.fcm-key    FcmProperties.java:12  @ConditionalOnProperty(alarm.push.enabled)
  INFO   legacy.s3.bucket      application-prod.yml:41  사용되지 않는 키
2 errors, 1 warning, 1 info
```

설정 **값**은 어떤 출력에도 나오지 않는다. 이 도구는 키 이름만 다룬다.

## Roadmap

- [x] **M0** 첫인상: README, LICENSE, CLAUDE.md, 명세 문서, CI 골격
- [x] **M1** 뼈대: 멀티 모듈 Gradle, version catalog, Spotless·Error Prone·JaCoCo, 도메인 모델
- [x] **M2** 정규화: Spring relaxed binding 규칙 + 단위 테스트
- [x] **M3** 설정 소스: yml/properties 파싱, 프로필 병합, 플레이스홀더, env 파일
- [x] **M4** 키 추출: `@Value`, `Environment`, `@ConfigurationProperties`, 조건부 강등
- [x] **M5** 비교 + 리포트: 규칙 CG001~CG005, 수정 제안, 콘솔/Markdown/JSON, JSON 스키마
- [x] **M6** CLI: `check` / `explain` / `rules`, 고정된 종료 코드
- [x] **M7** Gradle 플러그인: `configGuardCheck` 태스크, TestKit 테스트
- [x] **M8** GitHub Action + 샘플 앱 + e2e
- [ ] **M9** 문서: 가이드, 규칙 레퍼런스, Agent Skill, llms.txt, 릴리스
- [ ] **M10** (선택) Agent Skill 효과 평가

## 개발 방식

이 프로젝트는 [명세(docs/spec.md)](docs/spec.md)를 기준으로 LLM(Claude Code)과 함께 개발한다.

- [CLAUDE.md](CLAUDE.md): LLM이 이 저장소에서 작업할 때 따르는 규칙
- [docs/decisions.md](docs/decisions.md): 명세가 모호했던 부분과 정한 해석
- [docs/retrospective/spec-driven-dev.md](docs/retrospective/spec-driven-dev.md): LLM이 명세를 잘못 이해했던 사례와 명세를 어떻게 고쳤어야 했는지

## License

[MIT](LICENSE)
