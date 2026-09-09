plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.sunmoon.platform"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

// Order service — sun-moon-java-platform family. Docker-deployed
// executable JAR with embedded Jetty (see docs/adr/0001) — replaces the
// WAR + shared external Jetty setup archived at
// sun-moon-java-platform-order-jetty. That setup needed several
// workarounds (providedRuntime slf4j-api packaging, providedCompile
// servlet-api, an external deployment descriptor for a Logback SCI
// exclusion) that only existed *because* of sharing one Jetty process —
// none of that applies once each service owns its own container/JVM.
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // Order events reach the Device Server through Redis Pub/Sub, which
    // is already running on the host. Kafka would be the other answer
    // and is far too heavy for a 2010 four-core box (umbrella ADR-0002).
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.postgresql:postgresql")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")

    // Pinned to 2.6.0 — see the archived -jetty repo's ADR 0005 for why
    // (2.6.0 is the version springdoc's own POM declares against Spring
    // Boot 3.3.0; newer releases target 3.4/3.5 and won't compile here).
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}
