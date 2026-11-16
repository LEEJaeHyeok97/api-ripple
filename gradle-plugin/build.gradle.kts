// core를 감싼 Gradle 플러그인 (명세 6.7). configGuardCheck 태스크를 등록한다.
plugins {
    id("configguard.java-conventions")
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":core"))
    testImplementation(project(":cli"))
    testImplementation(libs.picocli)
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        register("configGuard") {
            id = "io.github.leejaehyeok97.config-guard"
            implementationClass = "io.github.leejaehyeok97.configguard.gradle.ConfigGuardPlugin"
            displayName = "config-guard"
            description = "Detects missing Spring Boot configuration keys per profile before deployment."
        }
    }
}

// TestKit 테스트는 core의 fixture를 임시 프로젝트로 복사해 쓴다.
val fixtures = rootProject.layout.projectDirectory.dir("core/src/test/resources/fixtures")

// 명세 6.15: samples/ 프로젝트에 대해 태스크 성공/실패, 리포트 생성을 검증한다.
val samples = rootProject.layout.projectDirectory.dir("samples/demo")

tasks.test {
    inputs.dir(fixtures).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("fixtures")
    systemProperty("configguard.fixtures", fixtures.asFile.absolutePath)
    inputs.files(fileTree(samples) { include("src/**", "deploy/**") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("samples")
    systemProperty("configguard.samples", samples.asFile.absolutePath)
}
