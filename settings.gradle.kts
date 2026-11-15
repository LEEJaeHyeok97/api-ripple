pluginManagement {
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "config-guard"

// cli, gradle-plugin 모듈은 해당 마일스톤(M6, M7)에서 추가한다. 빈 모듈을 미리 만들지 않는다.
include("core")
