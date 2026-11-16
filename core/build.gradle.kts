// 분석 엔진. Gradle API에 의존하지 않는 순수 라이브러리다.
plugins {
    id("configguard.java-conventions")
    jacoco
}

dependencies {
    implementation(libs.snakeyaml)
    implementation(libs.javaparser.symbol.solver)
    implementation(libs.jackson.databind)

    testImplementation(libs.json.schema.validator)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

// JSON 출력이 공개 스키마를 통과하는지 테스트한다 (명세 6.10).
val reportSchema = rootProject.layout.projectDirectory.file("schema/report.schema.json")

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    inputs.file(reportSchema).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("reportSchema")
    systemProperty("configguard.reportSchema", reportSchema.asFile.absolutePath)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

// 명세 3장: core 라인 커버리지 90% 미만이면 빌드 실패.
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
