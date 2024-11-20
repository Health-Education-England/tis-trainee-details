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
version = "1.17.0"

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
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.1.0")
  }
}

val mapstructVersion = "1.5.5.Final"
val mongockVersion = "5.4.2"
val openHtmlToPdfVersion = "1.0.10"
val sentryVersion = "7.6.0"

dependencies {
  // Spring Boot
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
  implementation("org.springframework.boot:spring-boot-starter-validation")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
  testImplementation("com.playtika.testcontainers:embedded-redis:3.1.5")
  testImplementation("org.testcontainers:junit-jupiter:1.19.8")

  // Lombok
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")

  // Mapstruct
  implementation("org.mapstruct:mapstruct:${mapstructVersion}")
  annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")

  implementation("io.mongock:mongock-springboot:${mongockVersion}")
  implementation("io.mongock:mongodb-springdata-v4-driver:${mongockVersion}")

  // Sentry reporting
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:$sentryVersion")

  // Amazon AWS
  implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
  implementation("io.awspring.cloud:spring-cloud-aws-starter-sns")
  implementation("com.amazonaws:aws-xray-recorder-sdk-spring:2.15.1")

  // PDF
  implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:${openHtmlToPdfVersion}")
  implementation("com.openhtmltopdf:openhtmltopdf-slf4j:${openHtmlToPdfVersion}")
  implementation("org.jsoup:jsoup:1.17.2")
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
testing {
  suites {
    configureEach {
      if (this is JvmTestSuite) {
        useJUnitJupiter()
        dependencies {
          implementation(project())
          implementation("org.springframework.boot:spring-boot-starter-test")
        }
      }
    }

    val test by getting(JvmTestSuite::class) {
      dependencies {
        annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
      }
    }

    register<JvmTestSuite>("integrationTest") {
      dependencies {
        implementation("org.springframework.boot:spring-boot-testcontainers")
        implementation("org.testcontainers:junit-jupiter")
        implementation("org.testcontainers:localstack")
        implementation("org.testcontainers:mongodb")
      }

      targets {
        all {
          testTask.configure {
            shouldRunAfter(test)
            systemProperty("spring.profiles.active", "test")
          }
        }
      }
    }

    // Include implementation dependencies.
    val integrationTestImplementation by configurations.getting {
      extendsFrom(configurations.implementation.get())
    }
  }
}

tasks.named("check") {
  dependsOn(testing.suites.named("integrationTest"))
}

tasks.jacocoTestReport {
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}
