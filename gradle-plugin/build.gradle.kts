// core를 감싼 Gradle 플러그인 (명세 6.7). configGuardCheck 태스크를 등록한다.
plugins {
    id("configguard.java-conventions")
    `java-gradle-plugin`
    alias(libs.plugins.gradle.plugin.publish)
}

// Plugin Portal은 플러그인 아티팩트만 받는다. core는 별도로 배포하지 않고 플러그인 jar 안에 넣는다.
// core의 외부 의존성은 플러그인의 implementation으로 선언해 POM에 들어가게 한다.
val bundled: Configuration = configurations.create("bundled") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    bundled(project(":core"))
    compileOnly(project(":core"))
    implementation(libs.javaparser.symbol.solver)
    implementation(libs.snakeyaml)
    implementation(libs.jackson.databind)

    testImplementation(project(":core"))
    testImplementation(project(":cli"))
    testImplementation(libs.picocli)
    testImplementation(gradleTestKit())
}

interface InjectedArchives {
    @get:Inject
    val archives: ArchiveOperations
}

tasks.jar {
    // 스크립트의 zipTree를 람다에서 직접 부르면 configuration cache가 스크립트를 직렬화하려다 실패한다.
    val archives = objects.newInstance<InjectedArchives>().archives
    from(bundled.elements.map { jars -> jars.map { archives.zipTree(it) } })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// TestKit(withPluginClasspath)이 쓰는 클래스패스에도 core를 넣는다.
tasks.pluginUnderTestMetadata {
    pluginClasspath.from(bundled)
}

// 배포 설정만 해 둔다. 실제 배포(./gradlew :gradle-plugin:publishPlugins)는 사람이 결정한다 (명세 6.14).
gradlePlugin {
    website = "https://github.com/LEEJaeHyeok97/config-guard"
    vcsUrl = "https://github.com/LEEJaeHyeok97/config-guard.git"
    plugins {
        register("configGuard") {
            id = "io.github.leejaehyeok97.config-guard"
            implementationClass = "io.github.leejaehyeok97.configguard.gradle.ConfigGuardPlugin"
            displayName = "config-guard"
            description =
                "Statically compares the config keys your Spring Boot code requires with the keys each " +
                "profile provides, and fails CI on missing keys before deployment. Never reads or prints values."
            tags = listOf("spring-boot", "configuration", "static-analysis", "ci", "verification")
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

// plugin-publish가 javadoc jar를 만든다. 문법 검사는 유지하고 "주석 없음" 경고만 끈다.
tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}

// 기본 artifactId(프로젝트 이름 "gradle-plugin")는 너무 일반적이다.
publishing {
    publications.withType<MavenPublication>().configureEach {
        if (name == "pluginMaven") {
            artifactId = "config-guard-gradle-plugin"
        }
    }
}
