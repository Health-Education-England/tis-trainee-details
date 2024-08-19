plugins {
  java
  id("org.springframework.boot") version "3.3.0"
  id("io.spring.dependency-management") version "1.1.4"

  // Code Quality
  checkstyle
  jacoco
  id("org.sonarqube") version "5.1.0.4882"
}

group = "uk.nhs.hee.trainee.details"
version = "1.7.0"

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

repositories {
  mavenCentral()
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.1.1")
  }
}

dependencies {
  // Spring Boot
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-validation")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
  testImplementation("com.playtika.testcontainers:embedded-redis:3.1.5")
  testImplementation("org.testcontainers:junit-jupiter:1.19.8")

  // Lombok
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")

  // Mapstruct
  val mapstructVersion = "1.5.5.Final"
  implementation("org.mapstruct:mapstruct:${mapstructVersion}")
  annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")

  val mongockVersion = "5.4.0"
  implementation("io.mongock:mongock-springboot:${mongockVersion}")
  implementation("io.mongock:mongodb-springdata-v4-driver:${mongockVersion}")

  // Sentry reporting
  val sentryVersion = "7.6.0"
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:$sentryVersion")

  // Amazon AWS
  implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
  implementation("com.amazonaws:aws-xray-recorder-sdk-spring:2.15.1")

  // Rabbit MQ
  implementation("org.springframework.boot:spring-boot-starter-amqp")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
    vendor.set(JvmVendorSpec.ADOPTIUM)
  }
}

checkstyle {
  config = resources.text.fromArchiveEntry(configurations.checkstyle.get().first(), "google_checks.xml")
}

sonarqube {
  properties {
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.login", System.getenv("SONAR_TOKEN"))
    property("sonar.organization", "health-education-england")
    property("sonar.projectKey", "Health-Education-England_TIS-TRAINEE-DETAILS")

    property("sonar.java.checkstyle.reportPaths",
      "build/reports/checkstyle/main.xml,build/reports/checkstyle/test.xml")
  }
}

tasks.jacocoTestReport {
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
  useJUnitPlatform()
}
