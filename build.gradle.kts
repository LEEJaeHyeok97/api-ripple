plugins {
    base
    // 하위 프로젝트들이 같은 클래스로더에서 플러그인을 쓰도록 루트에서 한 번 불러온다 (Spotless 공유 빌드 서비스).
    id("configguard.java-conventions") apply false
    alias(libs.plugins.gradle.plugin.publish) apply false
}

// 릴리스 워크플로가 태그에서 버전을 넘긴다: -PreleaseVersion=0.1.0
val projectVersion = providers.gradleProperty("releaseVersion").getOrElse("0.1.0-SNAPSHOT")

allprojects {
    group = "io.github.leejaehyeok97"
    version = projectVersion
}
