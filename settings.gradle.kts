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

// gradle-plugin 모듈은 M7에서 추가한다. 빈 모듈을 미리 만들지 않는다.
include("core", "cli")
