plugins {
	kotlin("jvm") version "2.2.10"
	kotlin("plugin.spring") version "2.2.10"
	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "events.boudicca"
version = "0.0.1-SNAPSHOT"
description = "branch-deployer"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.11")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.github.docker-java:docker-java-core:3.6.0")
	implementation("com.github.docker-java:docker-java-transport-zerodep:3.6.0")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
	implementation("org.apache.commons:commons-compress:1.28.0")
	implementation("org.eclipse.jgit:org.eclipse.jgit:7.3.0.202506031305-r")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<Exec>("imageBuild") {
	inputs.file("src/main/docker/Dockerfile")
	inputs.files(tasks.named("bootJar"))
	dependsOn(tasks.named("assemble"))
	commandLine("docker", "build", "-t", "localhost/branch-deployer", "-f", "src/main/docker/Dockerfile", ".")
}
