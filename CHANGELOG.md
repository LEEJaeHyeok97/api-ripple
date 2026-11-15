# Changelog

이 프로젝트의 주요 변경 사항을 기록한다.
형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 따르고, 버전은 [SemVer](https://semver.org/lang/ko/)를 따른다.

## [Unreleased]

### Added

- 프로젝트 문서: README, 명세(`docs/spec.md`), `CLAUDE.md`, 설계 결정·아이디어·회고 문서 틀 (M0)
- Gradle wrapper와 빌드 골격, JDK 17/21 매트릭스 CI 워크플로 (M0)
- 멀티 모듈 빌드: `build-logic` 컨벤션 플러그인, version catalog, `core` 모듈 (M1)
- 품질 게이트: Spotless(google-java-format), Error Prone + NullAway, JaCoCo(core 라인 90%) (M1)
- 도메인 모델: `RequiredKey`, `ProvidedKey`, `Finding`, `Suggestion`, 규칙 ID `CG001`~`CG005` (M1)
- CI: JDK 21로 빌드하고 JDK 17/21에서 테스트, Dependabot, PR 템플릿 (M1)
- 키 정규화: Spring relaxed binding 규칙(kebab-case 정규형, 대시 무시 비교, 환경변수 ↔ 키 변환), 레퍼런스 문서 `docs/reference/key-normalization.md` (M2)
- 설정 소스 수집: `application*.yml|yaml|properties`(루트·`config/`), 멀티 도큐먼트(`---`, `#---`), 프로필 병합(include/group 1단계), 플레이스홀더 환경변수 추적, 환경변수 이름 목록(.env / 이름 목록), 레퍼런스 문서 `docs/reference/config-sources.md` (M3)
