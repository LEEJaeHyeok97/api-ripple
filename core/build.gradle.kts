// 분석 엔진. Gradle API에 의존하지 않는 순수 라이브러리다.
plugins {
    id("configguard.java-conventions")
    jacoco
}

dependencies {
    implementation(libs.snakeyaml)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
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
