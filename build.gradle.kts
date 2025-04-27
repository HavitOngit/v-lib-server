plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.vlib-server"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.session:spring-session-core")
    implementation("commons-io:commons-io:2.13.0")
    // SQLite JDBC driver
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    runtimeOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    // Added Hibernate dialect support for SQLite
    implementation("org.hibernate.orm:hibernate-community-dialects")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // Removed H2 database
    // runtimeOnly("com.h2database:h2")
    runtimeOnly("com.oracle.database.jdbc:ojdbc11")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
