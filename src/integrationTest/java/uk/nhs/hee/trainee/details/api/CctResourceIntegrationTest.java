/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.api;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.LTFT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.jayway.jsonpath.JsonPath;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.trainee.details.DockerImageNames;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType;
import uk.nhs.hee.trainee.details.model.CctCalculation;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctChange;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
class CctResourceIntegrationTest {

  private static final String TRAINEE_ID = UUID.randomUUID().toString();

  private static final LocalDate CCT_DATE_75 = LocalDate.of(2024, 11, 1);
  //based on calculationJson change below.

  @Container
  @ServiceConnection
  private static final MongoDBContainer mongoContainer = new MongoDBContainer(
      DockerImageNames.MONGO);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MongoTemplate template;

  @MockBean
  private SqsTemplate sqsTemplate;

  private ObjectNode calculationJson;

  @BeforeEach
  void setUp() throws IOException {
    calculationJson = (ObjectNode) new ObjectMapper().readTree("""
        {
          "name": "Test Calculation",
          "programmeMembership": {
            "id": "12345678-aaaa-bbbb-cccc-012345678910",
            "name": "Test Programme",
            "startDate": "2024-01-01",
            "endDate": "2025-01-01",
            "wte": 0.5,
            "designatedBodyCode": "testDbc"
          },
          "cctDate": "2024-01-01",
          "changes": [
            {
              "type": "LTFT",
              "startDate": "2024-07-01",
              "wte": 0.75
            }
          ]
        }
        """);
  }

  @AfterEach
  void tearDown() {
    template.findAllAndRemove(new Query(), CctCalculation.class);
    template.findAllAndRemove(new Query(), TraineeProfile.class);
  }

  @Test
  void shouldBeForbiddenGettingCalculationsWhenNoToken() throws Exception {
    mockMvc.perform(get("/api/cct/calculation", "1"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldBeForbiddenGettingCalculationsWhenNoTraineeId() throws Exception {
    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(get("/api/cct/calculation", "1")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldNotGetCalculationsWhenNotOwnedByUser() throws Exception {
    UUID id = UUID.randomUUID();
    CctCalculation entity = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .build();
    template.insert(entity);

    String token = TestJwtUtil.generateTokenForTisId(UUID.randomUUID().toString());
    mockMvc.perform(get("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void shouldGetCalculationsWhenOwnedByUser() throws Exception {
    UUID pmId = UUID.randomUUID();

    CctCalculation entity = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembership.builder()
            .id(pmId)
            .startDate(LocalDate.parse("2024-01-01"))
            .endDate(LocalDate.parse("2025-01-01"))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder()
                .type(CctChangeType.LTFT)
                .startDate(LocalDate.parse("2024-07-01"))
                .wte(0.5)
                .build()))
        .build();
    entity = template.insert(entity);

    LocalDate cctDate = LocalDate.of(2025, 7, 4); //based on details above

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(get("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(entity.id().toString()))
        .andExpect(jsonPath("$[0].name").value("Test Calculation"))
        .andExpect(jsonPath("$[0].programmeMembership.id").value(pmId.toString()))
        .andExpect(jsonPath("$[0].created").value(
            entity.created().truncatedTo(ChronoUnit.MILLIS).toString()))
        .andExpect(jsonPath("$[0].cctDate").value(cctDate.toString()))
        .andExpect(jsonPath("$[0].lastModified").value(
            entity.lastModified().truncatedTo(ChronoUnit.MILLIS).toString()));
  }

  @Test
  void shouldGetCalculationsOrderedByLatestWhenOwnedByUser() throws Exception {
    CctCalculation future = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .name("Future")
        .programmeMembership(CctProgrammeMembership.builder()
            .id(UUID.randomUUID())
            .startDate(LocalDate.parse("2024-01-01"))
            .endDate(LocalDate.parse("2025-01-01"))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder()
                .type(CctChangeType.LTFT)
                .startDate(LocalDate.parse("2024-07-01"))
                .wte(0.5)
                .build()))
        .build();
    future = template.insert(future);

    CctCalculation past = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .name("Past")
        .programmeMembership(CctProgrammeMembership.builder()
            .id(UUID.randomUUID())
            .startDate(LocalDate.parse("2024-01-01"))
            .endDate(LocalDate.parse("2025-01-01"))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder()
                .type(CctChangeType.LTFT)
                .startDate(LocalDate.parse("2024-07-01"))
                .wte(0.5)
                .build()))
        .build();
    template.insert(past);
    sleep(10); //ensure different last updated timestamps for ordering

    CctCalculation present = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .name("Present")
        .programmeMembership(CctProgrammeMembership.builder()
            .id(UUID.randomUUID())
            .startDate(LocalDate.parse("2024-01-01"))
            .endDate(LocalDate.parse("2025-01-01"))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .build())
        .changes(List.of(
            CctChange.builder()
                .type(CctChangeType.LTFT)
                .startDate(LocalDate.parse("2024-07-01"))
                .wte(0.5)
                .build()))
        .build();
    template.insert(present);
    sleep(10); //ensure different last updated timestamps for ordering

    template.save(future);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(get("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].name").value("Past"))
        .andExpect(jsonPath("$[1].name").value("Present"))
        .andExpect(jsonPath("$[2].name").value("Future"));
  }

  @Test
  void shouldBeForbiddenGettingCalculationWhenNoToken() throws Exception {
    mockMvc.perform(get("/api/cct/calculation/{id}", "1"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldBeForbiddenGettingCalculationWhenNoTraineeId() throws Exception {
    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(get("/api/cct/calculation/{id}", "1")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldNotGetCalculationWhenNotOwnedByUser() throws Exception {
    UUID id = UUID.randomUUID();
    CctCalculation entity = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .build();
    template.insert(entity);

    String token = TestJwtUtil.generateTokenForTisId(UUID.randomUUID().toString());
    mockMvc.perform(get("/api/cct/calculation/{id}", id)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldGetCalculationWhenOwnedByUser() throws Exception {
    UUID pmId = UUID.randomUUID();

    CctCalculation entity = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembership.builder()
            .id(pmId)
            .name("Test Programme")
            .startDate(LocalDate.parse("2024-01-01"))
            .endDate(LocalDate.parse("2025-01-01"))
            .wte(1.0)
            .designatedBodyCode("testDbc")
            .managingDeanery("Test Deanery")
            .build())
        .changes(List.of(
            CctChange.builder()
                .type(CctChangeType.LTFT)
                .startDate(LocalDate.parse("2024-07-01"))
                .wte(0.5)
                .build()))
        .build();
    entity = template.insert(entity);

    LocalDate cctDate = LocalDate.of(2025, 7, 4); //based on details above

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(get("/api/cct/calculation/{id}", entity.id())
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(entity.id().toString()))
        .andExpect(jsonPath("$.traineeId").doesNotExist())
        .andExpect(jsonPath("$.name").value("Test Calculation"))
        .andExpect(jsonPath("$.programmeMembership").isMap())
        .andExpect(jsonPath("$.programmeMembership.id").value(pmId.toString()))
        .andExpect(jsonPath("$.programmeMembership.startDate").value("2024-01-01"))
        .andExpect(jsonPath("$.programmeMembership.endDate").value("2025-01-01"))
        .andExpect(jsonPath("$.programmeMembership.wte").value(1))
        .andExpect(jsonPath("$.programmeMembership.designatedBodyCode").value("testDbc"))
        .andExpect(jsonPath("$.programmeMembership.managingDeanery").value("Test Deanery"))
        .andExpect(jsonPath("$.changes").isArray())
        .andExpect(jsonPath("$.changes", hasSize(1)))
        .andExpect(jsonPath("$.changes[0].type").value("LTFT"))
        .andExpect(jsonPath("$.changes[0].startDate").value("2024-07-01"))
        .andExpect(jsonPath("$.changes[0].wte").value(0.5))
        .andExpect(jsonPath("$.cctDate").value(cctDate.toString()))
        .andExpect(
            jsonPath("$.created").value(entity.created().truncatedTo(ChronoUnit.MILLIS).toString()))
        .andExpect(jsonPath("$.lastModified").value(
            entity.lastModified().truncatedTo(ChronoUnit.MILLIS).toString()));
  }

  @Test
  void shouldBeForbiddenCreatingCalculationWhenNoToken() throws Exception {
    mockMvc.perform(post("/api/cct/calculation"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldBeForbiddenCreatingCalculationWhenNoTraineeId() throws Exception {
    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldNotCreateCalculationWhenValidationFails() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type", is("about:blank")))
        .andExpect(jsonPath("$.title", is("Validation failure")))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.instance", is("/api/cct/calculation")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors", hasSize(3)));
  }

  @Test
  void shouldFailCreateCalculationValidationWhenIdPopulated() throws Exception {
    calculationJson.set("id", TextNode.valueOf(UUID.randomUUID().toString()));

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/id")))
        .andExpect(jsonPath("$.errors[0].detail", is("must be null")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldFailCreateCalculationValidationWhenNameNotValid(String name) throws Exception {
    calculationJson.replace("name", TextNode.valueOf(name));

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/name")))
        .andExpect(jsonPath("$.errors[0].detail", is("must not be blank")));
  }

  @Test
  void shouldFailCreateCalculationValidationWhenProgrammeMembershipNull() throws Exception {
    calculationJson.remove("programmeMembership");

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/programmeMembership")))
        .andExpect(jsonPath("$.errors[0].detail", is("must not be null")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"id", "startDate", "endDate", "wte"})
  void shouldFailCreateCalculationValidationWhenProgrammeMembershipFieldNull(String propertyName)
      throws Exception {
    calculationJson.withObject("programmeMembership")
        .remove(propertyName);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/programmeMembership/" + propertyName)))
        .andExpect(jsonPath("$.errors[0].detail", is("must not be null")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void shouldFailCreateCalculationValidationWhenProgrammeMembershipNameNotValid(String pmName)
      throws Exception {
    calculationJson.withObject("programmeMembership")
        .replace("name", TextNode.valueOf(pmName));

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/programmeMembership/name")))
        .andExpect(jsonPath("$.errors[0].detail", is("must not be blank")));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-10, -0.1, 1.1, 10})
  void shouldFailCreateCalculationValidationWhenProgrammeMembershipWteNotValid(double wte)
      throws Exception {
    calculationJson.withObject("programmeMembership")
        .replace("wte", DoubleNode.valueOf(wte));

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/programmeMembership/wte")))
        .andExpect(jsonPath("$.errors[0].detail", is("must be between 0 and 1")));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = "[]")
  void shouldFailCreateCalculationValidationWhenChangesNotValid(String changes) throws Exception {
    String body = """
        {
          "name": "Test Calculation",
          "programmeMembership": {
            "id": "12345678-aaaa-bbbb-cccc-012345678910",
            "name": "Test Programme",
            "startDate": "2024-01-01",
            "endDate": "2025-01-01",
            "wte": 0.5,
            "designatedBodyCode": "testDbc",
            "managingDeanery": "Test Deanery"
          },
          "changes": %s
        }
        """.formatted(changes);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/changes")))
        .andExpect(jsonPath("$.errors[0].detail", is("must not be empty")));
  }

  @Test
  void shouldFailCreateCalculationValidationWhenChangeIdPopulated() throws Exception {
    String body = """
        {
          "name": "Test Calculation",
          "programmeMembership": {
            "id": "12345678-aaaa-bbbb-cccc-012345678910",
            "name": "Test Programme",
            "startDate": "2024-01-01",
            "endDate": "2025-01-01",
            "wte": 0.5,
            "designatedBodyCode": "testDbc",
            "managingDeanery": "Test Deanery"
          },
          "changes": [
            {
              "id": "%s",
              "type": "LTFT",
              "startDate": "2024-07-01",
              "wte": 0.75
            }
          ]
        }
        """.formatted(UUID.randomUUID());

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/changes[0]/id")))
        .andExpect(jsonPath("$.errors[0].detail", is("must be null")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"type", "startDate", "wte"})
  void shouldFailCreateCalculationValidationWhenChangePropertyNull(String propertyName)
      throws Exception {
    ((ObjectNode) calculationJson.withArray("changes").get(0))
        .remove(propertyName);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/changes[0]/" + propertyName)))
        .andExpect(jsonPath("$.errors[0].detail", is("must not be null")));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-10, -0.1, 1.1, 10})
  void shouldFailCreateCalculationValidationWhenChangeWteNotValid(double wte) throws Exception {
    String body = """
        {
          "name": "Test Calculation",
          "programmeMembership": {
            "id": "12345678-aaaa-bbbb-cccc-012345678910",
            "name": "Test Programme",
            "startDate": "2024-01-01",
            "endDate": "2025-01-01",
            "wte": 0.5,
            "designatedBodyCode": "testDbc",
            "managingDeanery": "Test Deanery"
          },
          "changes": [
            {
              "type": "LTFT",
              "startDate": "2024-07-01",
              "wte": %s
            }
          ]
        }
        """.formatted(wte);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/changes[0]/wte")))
        .andExpect(jsonPath("$.errors[0].detail", is("must be between 0 and 1")));
  }

  @Test
  void shouldReturnCreatedCalculationJsonWhenRequestValid() throws Exception {
    Instant start = Instant.now();

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    MvcResult result = mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.traineeId").doesNotExist())
        .andExpect(jsonPath("$.cctDate").value(CCT_DATE_75.toString()))
        .andExpect(jsonPath("$.name").value("Test Calculation"))
        .andExpect(jsonPath("$.created").exists())
        .andExpect(jsonPath("$.lastModified").exists())
        .andExpect(jsonPath("$.changes[0].id").exists())
        .andReturn();

    String response = result.getResponse().getContentAsString();
    Instant created = Instant.parse(JsonPath.read(response, "$.created"));
    assertThat("Unexpected created timestamp.", created, greaterThan(start));

    Instant lastModified = Instant.parse(JsonPath.read(response, "$.lastModified"));
    assertThat("Unexpected last modified timestamp.", lastModified, greaterThan(start));
  }

  @Test
  void shouldReturnLocationOfCreatedCalculationWhenRequestValid() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    MvcResult result = mockMvc.perform(
            post("/api/cct/calculation")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(calculationJson.toString()))
        .andReturn();

    MockHttpServletResponse response = result.getResponse();
    String location = response.getHeader(HttpHeaders.LOCATION);
    String id = JsonPath.read(response.getContentAsString(), "$.id");

    assertThat("Unexpected location.", location, is("/api/cct/calculation/" + id));

    mockMvc.perform(get(location)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value("Test Calculation"));
  }

  @Test
  void shouldReturnBadRequestWhenUpdatingCalculationWithoutEntityId() throws Exception {
    UUID id = UUID.randomUUID();
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    mockMvc.perform(put("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldReturnBadRequestWhenUpdatingCalculationWithConflictingIds() throws Exception {
    UUID id1 = UUID.randomUUID();
    String body = """
        {
          "id": %s
          "name": "Test Calculation",
        }
        """.formatted(id1);

    UUID id2 = UUID.randomUUID();
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    mockMvc.perform(put("/api/cct/calculation/" + id2)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnForbiddenWhenUpdatingCalculationNotOwnedByUser() throws Exception {
    CctCalculation entity = CctCalculation.builder()
        .traineeId("another trainee")
        .name("Test Calculation")
        .build();
    entity = template.insert(entity);

    UUID id = entity.id();
    Instant created = entity.created();

    String body = """
        {
          "id": "%s",
          "created": "%s",
          "name": "Test Calculation updated",
          "programmeMembership": {
            "id": "12345678-aaaa-bbbb-cccc-012345678910",
            "name": "Test Programme",
            "startDate": "2024-01-01",
            "endDate": "2025-01-01",
            "wte": 0.5,
            "designatedBodyCode": "testDbc",
            "managingDeanery": "Test Deanery"
          },
          "changes": [
            {
              "type": "LTFT",
              "startDate": "2024-07-01",
              "wte": 0.75
            }
          ]
        }
        """.formatted(id, created);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    mockMvc.perform(put("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnNotFoundWhenUpdatingNewCalculation() throws Exception {
    UUID id = UUID.randomUUID();
    String body = """
        {
          "id": "%s",
          "name": "Test Calculation updated",
          "programmeMembership": {
            "id": "12345678-aaaa-bbbb-cccc-012345678910",
            "name": "Test Programme",
            "startDate": "2024-01-01",
            "endDate": "2025-01-01",
            "wte": 0.5,
            "designatedBodyCode": "testDbc",
            "managingDeanery": "Test Deanery"
          },
          "changes": [
            {
              "type": "LTFT",
              "startDate": "2024-07-01",
              "wte": 0.75
            }
          ]
        }
        """.formatted(id);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    mockMvc.perform(put("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldNotUpdateCalculationWhenChangeIdValidationFails() throws Exception {
    CctCalculation entity = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2025, 1, 1))
            .wte(0.5)
            .designatedBodyCode("testDbc")
            .managingDeanery("Test Deanery")
            .build())
        .changes(List.of(
                CctChange.builder().type(LTFT).startDate(LocalDate.of(2024, 11, 1))
                    .wte(1.0)
                    .build()
            )
        )
        .build();
    entity = template.insert(entity);

    UUID id = entity.id();
    UUID changeId1 = entity.changes().get(0).id();
    UUID changeId2 = UUID.randomUUID();

    String body = """
        {
          "id": "%s",
          "name": "Test Calculation updated",
          "programmeMembership": {
            "id": "12345678-aaaa-bbbb-cccc-012345678910",
            "name": "Test Programme",
            "startDate": "2024-01-01",
            "endDate": "2025-01-01",
            "wte": 0.5,
            "designatedBodyCode": "testDbc",
            "managingDeanery": "Test Deanery"
          },
          "changes": [
            {
              "id": "%s",
              "type": "LTFT",
              "startDate": "2024-07-01",
              "wte": 0.75
            },
            {
              "id": "%s",
              "type": "LTFT",
              "startDate": "2025-08-02",
              "wte": 1.0
            },
            {
              "id": "%s",
              "type": "LTFT",
              "startDate": "2026-09-03",
              "wte": 0.8
            }
          ]
        }
        """.formatted(id, changeId1, changeId1, changeId2);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    mockMvc.perform(put("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(jsonPath("$.errors", hasSize(2)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/changes[1]/id")))
        .andExpect(jsonPath("$.errors[0].detail", is("must be unique")))
        .andExpect(jsonPath("$.errors[1].pointer", is("#/changes[2]/id")))
        .andExpect(jsonPath("$.errors[1].detail", is("must be null or match existing value")));
  }

  @Test
  void shouldUpdateCalculation() throws Exception {
    CctCalculation entity = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .name("Test Calculation")
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2025, 1, 1))
            .wte(0.5)
            .designatedBodyCode("testDbc")
            .managingDeanery("Test Deanery")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(LocalDate.of(2024, 11, 1))
                .wte(1.0)
                .build()))
        .build();
    entity = template.insert(entity);

    UUID id = entity.id();
    UUID changeId = entity.changes().get(0).id();
    Instant created = entity.created();
    Instant lastModified = entity.lastModified();
    assertThat("Unexpected initial last modified date.", created, is(lastModified));

    String body = """
        {
          "id": "%s",
          "created": "%s",
          "name": "Test Calculation updated",
          "programmeMembership": {
            "id": "12345678-aaaa-bbbb-cccc-012345678910",
            "name": "Test Programme",
            "startDate": "2024-01-01",
            "endDate": "2025-01-01",
            "wte": 0.5,
            "designatedBodyCode": "testDbc",
            "managingDeanery": "Test Deanery"
          },
          "changes": [
            {
              "id": "%s",
              "type": "LTFT",
              "startDate": "2024-07-01",
              "wte": 0.75
            },
            {
              "type": "LTFT",
              "startDate": "2025-08-02",
              "wte": 1.0
            }
          ]
        }
        """.formatted(id, created, changeId);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    MvcResult result = mockMvc.perform(put("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.name").value("Test Calculation updated"))
        .andExpect(jsonPath("$.changes[0].id").value(changeId.toString()))
        .andExpect(jsonPath("$.changes[1].id").exists())
        .andExpect(jsonPath("$.created").value(created.toString()))
        .andReturn();

    MockHttpServletResponse response = result.getResponse();
    String lastModifiedString = JsonPath.read(response.getContentAsString(), "$.lastModified");
    String createdString = JsonPath.read(response.getContentAsString(), "$.created");
    assertThat("Unexpected last modified date.", lastModifiedString, not(createdString));

    CctCalculation updatedEntity = template.findById(id, CctCalculation.class);
    assertThat("Unexpected missing saved entity.", updatedEntity, notNullValue());
    assertThat("Unexpected updated entity name.", updatedEntity.name(),
        is("Test Calculation updated"));
  }

  @Test
  void shouldBeForbiddenDeletingCalculationWhenNoToken() throws Exception {
    mockMvc.perform(delete("/api/cct/calculation/{id}", "1"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldBeForbiddenDeletingCalculationWhenNoTraineeId() throws Exception {
    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(delete("/api/cct/calculation/{id}", "1")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldNotDeleteCalculationWhenNotOwnedByUser() throws Exception {
    UUID id = UUID.randomUUID();
    CctCalculation entity = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .build();
    template.insert(entity);

    String token = TestJwtUtil.generateTokenForTisId(UUID.randomUUID().toString());
    mockMvc.perform(delete("/api/cct/calculation/{id}", id)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldReturnNotFoundWhenDeletingCalculationThatDoesNotExist() throws Exception {
    UUID id = UUID.randomUUID();
    CctCalculation entity = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .build();
    template.insert(entity);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    UUID anotherId = UUID.randomUUID();
    mockMvc.perform(delete("/api/cct/calculation/" + anotherId)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldDeleteCalculation() throws Exception {
    UUID id = UUID.randomUUID();
    CctCalculation entity = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .programmeMembership(CctProgrammeMembership.builder()
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2025, 1, 1))
            .wte(0.5)
            .designatedBodyCode("testDbc")
            .managingDeanery("Test Deanery")
            .build())
        .changes(List.of(
            CctChange.builder().type(LTFT).startDate(LocalDate.of(2024, 11, 1))
                .wte(1.0)
                .build()))
        .build();
    template.insert(entity);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    mockMvc.perform(delete("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }
}
