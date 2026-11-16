// core를 감싼 CLI. 사람과 LLM 에이전트가 쓰는 1차 인터페이스다 (명세 6.9).
plugins {
    id("configguard.java-conventions")
    application
}

dependencies {
    implementation(project(":core"))
    implementation(libs.picocli)
    testImplementation(libs.jackson.databind)
}

application {
    mainClass = "io.github.leejaehyeok97.configguard.cli.Main"
    applicationName = "config-guard"
}

val cliVersion = project.version.toString()

tasks.processResources {
    // explain 명령의 출처는 docs/reference/rules/*.md 하나다. 문서와 CLI 설명이 어긋나지 않게 빌드 때 복사한다.
    from(rootProject.layout.projectDirectory.dir("docs/reference/rules")) {
        into("io/github/leejaehyeok97/configguard/cli/rules")
    }
    // 스크립트 수준 변수를 람다에서 직접 잡으면 configuration cache가 직렬화하지 못한다.
    val version = cliVersion
    inputs.property("version", version)
    filesMatching("**/version.properties") {
        expand("version" to version)
    }
}

// 배포용 단일 jar: java -jar config-guard-<version>-all.jar
val fatJar = tasks.register<Jar>("fatJar") {
    group = "build"
    description = "의존성을 모두 포함한 실행 가능한 CLI jar를 만든다."
    archiveBaseName = "config-guard"
    archiveClassifier = "all"
    manifest {
        attributes("Main-Class" to application.mainClass.get(), "Implementation-Version" to cliVersion)
    }
    from(sourceSets.main.map { it.output })
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.map { cp -> cp.map { if (it.isDirectory) it else zipTree(it) } })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/*/module-info.class", "module-info.class")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.assemble {
    dependsOn(fatJar)
}

// CLI 테스트는 core의 fixture를 그대로 쓴다. Gradle 플러그인과 같은 입력으로 비교하기 위해서다.
val fixtures = rootProject.layout.projectDirectory.dir("core/src/test/resources/fixtures")

tasks.test {
    inputs.dir(fixtures).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("fixtures")
    systemProperty("configguard.fixtures", fixtures.asFile.absolutePath)
}
