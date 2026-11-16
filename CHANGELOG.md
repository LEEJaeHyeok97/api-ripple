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
- 키 추출: `@Value`(중첩 플레이스홀더, 같은 파일 상수), `Environment.getProperty/getRequiredProperty`, `@ConfigurationProperties`(JavaBean·생성자·record 바인딩, 중첩, 컬렉션), `@ConditionalOnProperty`/`@Profile` 조건부 강등, 분석 불가 패턴 보고, 레퍼런스 문서 `docs/reference/key-extraction.md` (M4)
- 비교와 리포트: 규칙 CG001~CG005, 수정 제안(Suggestion), 콘솔·Markdown(PR 코멘트 마커)·JSON 렌더러, JSON 스키마 `schema/report.schema.json`(schemaVersion 1), 규칙 문서 `docs/reference/rules/`, `docs/reference/json-report.md`, fixture 8개 기대값 테스트, secret 유출 테스트 (M5)
- CLI `config-guard`: `check`(console/markdown/json, `--fail-on`, `--ignore`, `--env-file`, `--framework-prefix`), `explain`, `rules`, `--version`; 고정 종료 코드(0/1/2/3), stdout/stderr 분리, 단일 실행 jar(`:cli:fatJar`), 레퍼런스 문서 `docs/reference/cli.md` (M6)
- Gradle 플러그인 `io.github.leejaehyeok97.config-guard`: `configGuard` DSL, `configGuardCheck` 태스크(verification, 증분 빌드·빌드 캐시·configuration cache 호환), report.md/report.json, TestKit 테스트, CLI와 JSON 동일성 테스트, 레퍼런스 문서 `docs/reference/gradle-dsl.md` (M7)
- GitHub Action `action/`: `configGuardCheck --continue` 실행, PR 코멘트 게시·갱신(마커), 잡 요약, 종료 코드 전달. 데모 앱 `samples/demo`(ERROR 2, WARNING 1), e2e 워크플로(CLI·Action 실행, 두 JSON 동일성) (M8)
- 문서: README(설치·출력 예시·AI 에이전트·한계·설계 결정), 가이드(`docs/guide/`: 시작하기, CI 연동, AI 에이전트와 쓰기), Agent Skill `skills/config-guard/SKILL.md`, `llms.txt`, 릴리스 절차 `docs/releasing.md` (M9)
- 릴리스 설정: 태그 푸시 시 CLI jar를 GitHub Release에 첨부하는 워크플로, Gradle Plugin Portal 배포 설정(core를 플러그인 jar에 포함, artifactId `config-guard-gradle-plugin`) (M9)
- Agent Skill 평가 `evals/`: 과제 프로젝트 5개, 실행·채점 스크립트(`run.sh`, `score.py`, `report.py`), 결과 `evals/RESULTS.md` (M10)

### Changed

- JSON 리포트 스키마 테스트가 직접 만든 검증기 대신 networknt json-schema-validator(JSON Schema 2020-12)를 쓴다
