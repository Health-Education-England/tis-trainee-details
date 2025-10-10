/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.trainee.details.event;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.trainee.details.DockerImageNames;
import uk.nhs.hee.trainee.details.model.CctCalculation;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ProfileMoveListenerIntegrationTest {

  private static final String FROM_TRAINEE_ID = UUID.randomUUID().toString();
  private static final String TO_TRAINEE_ID = UUID.randomUUID().toString();

  private static final String PROFILE_MOVE_QUEUE = UUID.randomUUID().toString();

  @Container
  @ServiceConnection
  private static final MongoDBContainer mongoContainer = new MongoDBContainer(
      DockerImageNames.MONGO);

  @Container
  private static final LocalStackContainer localstack = new LocalStackContainer(
      DockerImageNames.LOCALSTACK)
      .withServices(SQS);

  @DynamicPropertySource
  private static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("application.aws.sqs.profile-move", () -> PROFILE_MOVE_QUEUE);

    registry.add("spring.cloud.aws.region.static", localstack::getRegion);
    registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
    registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
    registry.add("spring.cloud.aws.sqs.endpoint",
        () -> localstack.getEndpointOverride(SQS).toString());
    registry.add("spring.cloud.aws.sqs.enabled", () -> true);
    registry.add("spring.flyway.enabled", () -> false);
  }

  @BeforeAll
  static void setUpBeforeAll() throws IOException, InterruptedException {
    localstack.execInContainer("awslocal sqs create-queue --queue-name",
        PROFILE_MOVE_QUEUE);
  }

  @Autowired
  private SqsTemplate sqsTemplate;

  @Autowired
  private MongoTemplate mongoTemplate;

  @AfterEach
  void cleanUp() {
    mongoTemplate.findAllAndRemove(new Query(), CctCalculation.class);
  }

  @Test
  void shouldMoveAllCctCalculationsWhenProfileMove() throws JsonProcessingException {
    UUID id1 = UUID.randomUUID();
    CctCalculation cctToMove1 =
        CctCalculation.builder()
            .id(id1)
            .traineeId(FROM_TRAINEE_ID)
            .name("Some Name")
            .changes(List.of())
            .created(Instant.EPOCH)
            .build();
    cctToMove1 = mongoTemplate.insert(cctToMove1);

    UUID id2 = UUID.randomUUID();
    CctCalculation cctToMove2 =
        CctCalculation.builder()
            .id(id2)
            .traineeId(FROM_TRAINEE_ID)
            .name("Some Other Name")
            .changes(List.of())
            .created(Instant.EPOCH)
            .build();
    cctToMove2 = mongoTemplate.insert(cctToMove2);

    String eventString = """
        {
          "fromTraineeId": "%s",
          "toTraineeId": "%s"
        }""".formatted(FROM_TRAINEE_ID, TO_TRAINEE_ID);

    JsonNode eventJson = JsonMapper.builder()
        .build()
        .readTree(eventString);

    sqsTemplate.send(PROFILE_MOVE_QUEUE, eventJson);

    Criteria criteria = Criteria.where("traineeId").is(TO_TRAINEE_ID);
    Query query = Query.query(criteria);
    List<CctCalculation> cctCalculations = new ArrayList<>();

    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(10))
        .ignoreExceptions()
        .untilAsserted(() -> {
          List<CctCalculation> found = mongoTemplate.find(query, CctCalculation.class);
          assertThat("Unexpected moved CCT calculation count.", found.size(),
              is(2));
          cctCalculations.addAll(found);
        });

    CctCalculation movedCct1 = cctCalculations.stream()
        .filter(a -> a.id().equals(id1))
        .findFirst()
        .orElseThrow();
    assertThat("Unexpected moved CCT calculation trainee.", movedCct1.traineeId(),
        is(TO_TRAINEE_ID));
    assertThat("Unexpected moved history data.",
        movedCct1.withTraineeId(FROM_TRAINEE_ID).withLastModified(cctToMove1.lastModified()),
        is(cctToMove1));

    CctCalculation movedCct2 = cctCalculations.stream()
        .filter(a -> a.id().equals(id2))
        .findFirst()
        .orElseThrow();
    assertThat("Unexpected moved CCT calculation trainee.", movedCct2.traineeId(),
        is(TO_TRAINEE_ID));
    assertThat("Unexpected moved history data.",
        movedCct2.withTraineeId(FROM_TRAINEE_ID).withLastModified(cctToMove2.lastModified()),
        is(cctToMove2));

    //the lastModified date is changed during the move.
    assertThat("Unexpected original CCT calculation lastModified.",
        movedCct1.lastModified().truncatedTo(MILLIS),
        not(cctToMove1.lastModified().truncatedTo(MILLIS)));
    assertThat("Unexpected original CCT calculation lastModified.",
        movedCct2.lastModified().truncatedTo(MILLIS),
        not(cctToMove2.lastModified().truncatedTo(MILLIS)));
  }

  @Test
  void shouldNotMoveUnexpectedCctCalculationsWhenProfileMove() throws JsonProcessingException {
    UUID id1 = UUID.randomUUID();
    CctCalculation cctToMove1 =
        CctCalculation.builder()
            .id(id1)
            .traineeId(TO_TRAINEE_ID)
            .name("Some Name")
            .changes(List.of())
            .build();
    cctToMove1 = mongoTemplate.insert(cctToMove1);

    UUID id2 = UUID.randomUUID();
    CctCalculation cctToMove2 =
        CctCalculation.builder()
            .id(id2)
            .traineeId("another trainee id")
            .name("Some Other Name")
            .changes(List.of())
            .build();
    cctToMove2 = mongoTemplate.insert(cctToMove2);

    String eventString = """
        {
          "fromTraineeId": "%s",
          "toTraineeId": "%s"
        }""".formatted(FROM_TRAINEE_ID, TO_TRAINEE_ID);

    JsonNode eventJson = JsonMapper.builder()
        .build()
        .readTree(eventString);

    sqsTemplate.send(PROFILE_MOVE_QUEUE, eventJson);

    Criteria criteria = Criteria.where("traineeId").ne(FROM_TRAINEE_ID);
    Query query = Query.query(criteria);
    List<CctCalculation> cctCalculations = new ArrayList<>();

    await()
        .pollInterval(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(10))
        .ignoreExceptions()
        .untilAsserted(() -> {
          List<CctCalculation> found = mongoTemplate.find(query, CctCalculation.class);
          assertThat("Unexpected unchanged CCT calculation count.", found.size(),
              is(2));
          cctCalculations.addAll(found);
        });

    CctCalculation unchangedCct1 = cctCalculations.stream()
        .filter(a -> a.id().equals(id1))
        .findFirst()
        .orElseThrow();
    assertThat("Unexpected changed CCT calculation.",
        unchangedCct1.withLastModified(unchangedCct1.lastModified().truncatedTo(MILLIS)),
        is(cctToMove1.withLastModified(cctToMove1.lastModified().truncatedTo(MILLIS))));

    CctCalculation unchangedCct2 = cctCalculations.stream()
        .filter(a -> a.id().equals(id2))
        .findFirst()
        .orElseThrow();
    assertThat("Unexpected changed CCT calculation.",
        unchangedCct2.withLastModified(unchangedCct2.lastModified().truncatedTo(MILLIS)),
        is(cctToMove2.withLastModified(cctToMove2.lastModified().truncatedTo(MILLIS))));
  }
}