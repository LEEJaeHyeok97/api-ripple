# 릴리스 절차

버전은 [SemVer](https://semver.org/lang/ko/)를 따른다. 릴리스 여부와 시점은 사람이 결정한다.

## 1. CHANGELOG 정리

`## [Unreleased]` 내용을 `## [x.y.z] - YYYY-MM-DD` 섹션으로 옮긴다. 릴리스 워크플로가 이 섹션을 릴리스 노트로 쓴다.

## 2. 태그 푸시 → GitHub Release

```bash
git tag vX.Y.Z && git push origin vX.Y.Z
```

[release 워크플로](../.github/workflows/release.yml)가 버전을 `-PreleaseVersion=X.Y.Z`로 넘겨 전체 빌드·테스트를 다시 돌리고, `config-guard-X.Y.Z-all.jar`를 GitHub Release에 첨부한다.

## 3. GitHub Action 태그

Action 사용자는 `@v0`처럼 메이저 태그로 쓴다. 릴리스마다 메이저 태그를 옮긴다.

```bash
git tag -f v0 vX.Y.Z && git push -f origin v0
```

## 4. Gradle Plugin Portal (수동)

설정(`gradle-plugin/build.gradle.kts`의 `gradlePlugin { website, vcsUrl, tags }`, `com.gradle.plugin-publish`)은 되어 있다. 배포는 자격 증명이 필요하므로 사람이 실행한다.

```bash
export GRADLE_PUBLISH_KEY=...    # https://plugins.gradle.org 계정의 API 키
export GRADLE_PUBLISH_SECRET=...
./gradlew :gradle-plugin:publishPlugins -PreleaseVersion=X.Y.Z
```

- 플러그인 jar에는 `core`가 들어 있다(Plugin Portal은 플러그인 아티팩트만 받는다). 외부 의존성(JavaParser, SnakeYAML, Jackson)은 POM에 선언된다.
- 첫 배포는 Plugin Portal 검토를 거친다. 플러그인 ID `io.github.leejaehyeok97`은 GitHub 계정 소유 확인이 필요할 수 있다.
- 배포 전 로컬 확인: `./gradlew :gradle-plugin:publishToMavenLocal` 후 다른 프로젝트에서 `mavenLocal()`로 적용해 본다.
