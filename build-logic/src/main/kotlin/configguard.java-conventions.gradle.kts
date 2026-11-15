// 모든 Java 모듈이 공유하는 빌드 규칙: 툴체인, 포맷(Spotless), 정적 분석(Error Prone + NullAway), 테스트.
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

val catalog = the<VersionCatalogsExtension>().named("libs")
fun lib(alias: String) = catalog.findLibrary(alias).get()
fun version(alias: String) = catalog.findVersion(alias).get().requiredVersion

java {
    // Error Prone(>=2.43)과 google-java-format(>=1.29)은 JDK 21에서만 돈다. 컴파일은 JDK 21, 바이트코드는 17.
    toolchain.languageVersion = JavaLanguageVersion.of(version("java-toolchain"))
}

dependencies {
    "errorprone"(lib("errorprone-core"))
    "errorprone"(lib("nullaway"))
    compileOnly(lib("jspecify"))
    testCompileOnly(lib("jspecify"))

    testImplementation(platform(lib("junit-bom")))
    testImplementation(lib("junit-jupiter"))
    testImplementation(lib("assertj"))
    testRuntimeOnly(lib("junit-platform-launcher"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = version("java-release").toInt()
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all,-processing,-serial",
            "-Werror",
            // Error Prone 2.46+를 JDK 21에서 돌릴 때 필요하다 (JDK-8225377 우회).
            "-XDaddTypeAnnotationsToSymbol=true",
        ),
    )
    val isTestSource = name.contains("Test")
    options.errorprone {
        disableWarningsInGeneratedCode = true
        check("NullAway", if (isTestSource) CheckSeverity.OFF else CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "io.github.leejaehyeok97.configguard")
    }
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat(version("google-java-format"))
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// 테스트 실행 JDK는 -PtestJavaVersion=17|21 로 고른다 (CI 매트릭스). 바이트코드 17이 실제로 17에서 도는지 확인한다.
val testJavaVersion = providers.gradleProperty("testJavaVersion").orElse(version("java-toolchain"))

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("file.encoding", "UTF-8")
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = testJavaVersion.map { JavaLanguageVersion.of(it) }
    }
}
