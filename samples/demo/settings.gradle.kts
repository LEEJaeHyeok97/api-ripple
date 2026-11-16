// config-guard 데모 앱. 일부러 운영(prod) 설정을 빠뜨렸다: ERROR 2건, WARNING 1건.
pluginManagement {
    // 이 저장소의 config-guard 플러그인을 소스에서 바로 쓴다.
    includeBuild("../..")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "demo"
