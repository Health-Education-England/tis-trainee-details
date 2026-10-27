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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType.LTFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.trainee.details.DockerImageNames;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.model.CctCalculation;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctAssumedFullTimePeriod;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctChange;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctProgrammeDetails;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctProjection;
import uk.nhs.hee.trainee.details.model.CctCalculation.CctResult;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
class CctResourceIntegrationTest {

  private static final String TRAINEE_ID = UUID.randomUUID().toString();

  @Container
  @ServiceConnection
  private static final MongoDBContainer mongoContainer = new MongoDBContainer(
      DockerImageNames.MONGO);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MongoTemplate template;

  @MockitoBean
  private SqsTemplate sqsTemplate;

  private ObjectNode calculationJson;

  @BeforeEach
  void setUp() throws IOException {
    calculationJson = calculateJson("""
        [
          {
            "type": "LTFT",
            "startDate": "2024-02-01",
            "endDate": "2024-08-01",
            "wte": 0.6,
            "countsTowardsCompletion": true,
            "notes": "Cardiology rotation"
          }
        ]
        """);
    calculationJson.put("name", "Test Calculation");
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
  void shouldGetCalculationsAsSavedWhenOwnedByUser() throws Exception {
    CctCalculation entity = template.insert(savedCalculation(TRAINEE_ID));

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(get("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(entity.id().toString()))
        .andExpect(jsonPath("$[0].name").value("Test Calculation"))
        .andExpect(jsonPath("$[0].programmeDetails.specialty").value("Paediatrics"))
        .andExpect(jsonPath("$[0].changes[0].wteMonths").value(3.6))
        .andExpect(jsonPath("$[0].result.cctDate").value("2099-01-01"))
        .andExpect(jsonPath("$[0].created").value(
            entity.created().truncatedTo(ChronoUnit.MILLIS).toString()))
        .andExpect(jsonPath("$[0].lastModified").value(
            entity.lastModified().truncatedTo(ChronoUnit.MILLIS).toString()));
  }

  @Test
  void shouldGetCalculationsOrderedByLatestWhenOwnedByUser() throws Exception {
    CctCalculation future = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .name("Future")
        .build();
    future = template.insert(future);

    CctCalculation past = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .name("Past")
        .build();
    template.insert(past);
    sleep(10); //ensure different last updated timestamps for ordering

    CctCalculation present = CctCalculation.builder()
        .traineeId(TRAINEE_ID)
        .name("Present")
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
  void shouldGetCalculationAsSavedWhenOwnedByUser() throws Exception {
    CctCalculation entity = template.insert(savedCalculation(TRAINEE_ID));

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(get("/api/cct/calculation/{id}", entity.id())
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {
              "id": "%s",
              "name": "Test Calculation",
              "programmeDetails": {
                "specialty": "Paediatrics",
                "startDate": "2023-08-02",
                "lengthMonths": 84,
                "startGrade": "ST1",
                "startGradeOverrideNotes": null,
                "additionalMonths": 0,
                "additionalMonthsNotes": null,
                "acceleratedMonths": 0,
                "acceleratedMonthsNotes": null,
                "eighteenMonthFinalGrade": null,
                "eighteenMonthFinalGradeNotes": null,
                "skippedGrade": null,
                "skippedGradeNotes": null
              },
              "changes": [
                {
                  "type": "LTFT",
                  "startDate": "2024-02-01",
                  "endDate": "2024-08-01",
                  "wte": 0.6,
                  "countsTowardsCompletion": true,
                  "notes": "Cardiology rotation",
                  "calendarMonths": 6.0,
                  "wteMonths": 3.6
                }
              ],
              "result": {
                "cctDate": "2099-01-01",
                "originalProgrammeEndDate": "2030-07-29",
                "adjustedProgrammeEndDate": "2030-07-29",
                "totalWteMonthsCompleted": 9.6,
                "monthsRemaining": 74.4,
                "assumedFullTimePeriods": [
                  {
                    "startDate": "2023-08-02",
                    "endDate": "2024-01-31",
                    "months": 6.0
                  }
                ],
                "projection": {
                  "startDate": "2024-08-02",
                  "wte": 1.0,
                  "calendarMonths": 74.4,
                  "wteMonths": 74.4
                }
              },
              "created": "%s",
              "lastModified": "%s"
            }
            """.formatted(entity.id(), entity.created().truncatedTo(ChronoUnit.MILLIS),
            entity.lastModified().truncatedTo(ChronoUnit.MILLIS)), JsonCompareMode.STRICT));
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
  void shouldFailCreateCalculationValidationWhenChangesNull() throws Exception {
    calculationJson.putNull("changes");

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/changes")))
        .andExpect(jsonPath("$.errors[0].detail", is("must not be null")));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      type      | Please choose a change type.
      startDate | Please enter a start date.
      wte       | LTFT WTE must be a whole number between 1 and 99.
      """)
  void shouldFailCreateCalculationValidationWhenChangePropertyNull(String propertyName,
      String detail) throws Exception {
    ((ObjectNode) calculationJson.withArray("changes").get(0))
        .remove(propertyName);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/changes[0]/" + propertyName)))
        .andExpect(jsonPath("$.errors[0].detail", is(detail)));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-10, -0.1, 0.125, 1.0, 10})
  void shouldFailCreateCalculationValidationWhenChangeWteNotValid(double wte) throws Exception {
    ((ObjectNode) calculationJson.withArray("changes").get(0))
        .put("wte", wte);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/changes[0]/wte")))
        .andExpect(jsonPath("$.errors[0].detail",
            is("LTFT WTE must be a whole number between 1 and 99.")));
  }

  @Test
  void shouldNotCreateCalculationWhenChangeStartsBeforeProgramme() throws Exception {
    ObjectNode request = calculateJson("""
        [
          {
            "type": "LTFT",
            "startDate": "2023-08-01",
            "endDate": "2024-07-31",
            "wte": 0.5,
            "countsTowardsCompletion": true,
            "notes": ""
          }
        ]
        """);
    request.put("name", "Test Calculation");

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(content().json("""
            {
              "type": "about:blank",
              "title": "Validation failure",
              "status": 400,
              "instance": "/api/cct/calculation",
              "errors": [
                {
                  "pointer": "#/changes[0]/startDate",
                  "detail": "Start date cannot be before programme start (02/08/2023)."
                }
              ]
            }
            """, JsonCompareMode.STRICT));

    assertThat("Unexpected saved calculation count.",
        template.count(new Query(), CctCalculation.class), is(0L));
  }

  @Test
  void shouldNotCreateCalculationWhenChangeStartsOrEndsAfterCctDate() throws Exception {
    ObjectNode request = calculateJson("""
        [
          {
            "type": "LTFT",
            "startDate": "2030-01-01",
            "endDate": "2032-12-31",
            "wte": 0.5,
            "countsTowardsCompletion": true,
            "notes": ""
          },
          {
            "type": "LTFT",
            "startDate": "2033-01-01",
            "endDate": null,
            "wte": 0.5,
            "countsTowardsCompletion": true,
            "notes": ""
          }
        ]
        """);
    request.put("name", "Test Calculation");
    String cctDateLimit = "cannot be after the current projected Completion of Training Date "
        + "(26/02/2031).";

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(content().json("""
            {
              "type": "about:blank",
              "title": "Validation failure",
              "status": 400,
              "instance": "/api/cct/calculation",
              "errors": [
                {
                  "pointer": "#/changes[0]/endDate",
                  "detail": "End date %s"
                },
                {
                  "pointer": "#/changes[1]/startDate",
                  "detail": "Start date %s"
                }
              ]
            }
            """.formatted(cctDateLimit, cctDateLimit), JsonCompareMode.STRICT));

    assertThat("Unexpected saved calculation count.",
        template.count(new Query(), CctCalculation.class), is(0L));
  }

  @Test
  void shouldReturnCreatedCalculationJsonWhenRequestValid() throws Exception {
    Instant start = Instant.now();

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    ResultActions result = mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    String response = result.andReturn().getResponse().getContentAsString();
    String id = JsonPath.read(response, "$.id");
    String created = JsonPath.read(response, "$.created");
    String lastModified = JsonPath.read(response, "$.lastModified");
    assertThat("Unexpected created timestamp.", Instant.parse(created), greaterThan(start));
    assertThat("Unexpected last modified timestamp.", Instant.parse(lastModified),
        greaterThan(start));

    result.andExpect(content().json("""
        {
          "id": "%s",
          "name": "Test Calculation",
          "programmeDetails": {
            "specialty": "Paediatrics",
            "startDate": "2023-08-02",
            "lengthMonths": 84,
            "startGrade": "ST1",
            "startGradeOverrideNotes": "",
            "additionalMonths": 0,
            "additionalMonthsNotes": "",
            "acceleratedMonths": 0,
            "acceleratedMonthsNotes": "",
            "eighteenMonthFinalGrade": null,
            "eighteenMonthFinalGradeNotes": "",
            "skippedGrade": null,
            "skippedGradeNotes": ""
          },
          "changes": [
            {
              "type": "LTFT",
              "startDate": "2024-02-01",
              "endDate": "2024-08-01",
              "wte": 0.6,
              "countsTowardsCompletion": true,
              "notes": "Cardiology rotation",
              "calendarMonths": 6.016438356164383,
              "wteMonths": 3.6098630136986296
            }
          ],
          "result": {
            "cctDate": "2030-10-10",
            "originalProgrammeEndDate": "2030-07-29",
            "adjustedProgrammeEndDate": "2030-07-29",
            "totalWteMonthsCompleted": 9.626301369863013,
            "monthsRemaining": 74.37,
            "assumedFullTimePeriods": [
              {
                "startDate": "2023-08-02",
                "endDate": "2024-01-31",
                "months": 6.016438356164383
              }
            ],
            "projection": {
              "startDate": "2024-08-02",
              "wte": 1,
              "calendarMonths": 74.37,
              "wteMonths": 74.37
            }
          },
          "created": "%s",
          "lastModified": "%s"
        }
        """.formatted(id, created, lastModified), JsonCompareMode.STRICT));
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
  void shouldSaveAndGetChangesAndResultAsCalculated() throws Exception {
    ObjectNode request = calculateJson("""
        [
          {
            "type": "LTFT",
            "startDate": "2025-09-01",
            "endDate": null,
            "wte": 0.8,
            "countsTowardsCompletion": true,
            "notes": ""
          },
          {
            "type": "PARENTAL",
            "startDate": "2025-01-01",
            "endDate": "2025-06-30",
            "wte": null,
            "countsTowardsCompletion": false,
            "notes": ""
          },
          {
            "type": "OOPT",
            "startDate": "2024-03-01",
            "endDate": "2024-08-31",
            "wte": null,
            "countsTowardsCompletion": true,
            "notes": ""
          }
        ]
        """);
    ObjectNode programmeDetails = (ObjectNode) request.get("programmeDetails");
    programmeDetails.put("additionalMonths", 6);
    programmeDetails.put("additionalMonthsNotes", "ARCP outcome");

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    String calculated = mockMvc.perform(post("/api/cct/calculate")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.toString()))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    request.put("name", "Test Calculation");
    String saved = mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.toString()))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String loaded = mockMvc.perform(get("/api/cct/calculation/{id}",
            JsonPath.<String>read(saved, "$.id"))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode expected = mapper.readTree(calculated);
    for (String body : List.of(saved, loaded)) {
      JsonNode actual = mapper.readTree(body);
      assertThat("Unexpected changes.", actual.get("changes"), is(expected.get("changes")));
      assertThat("Unexpected result.", actual.get("result"), is(expected.get("result")));
    }
    assertThat("Unexpected CCT date.", expected.at("/result/cctDate").asText(),
        is("2033-01-17"));
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
    calculationJson.put("id", id.toString());
    calculationJson.put("name", "Test Calculation updated");

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    mockMvc.perform(put("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnNotFoundWhenUpdatingNewCalculation() throws Exception {
    UUID id = UUID.randomUUID();
    calculationJson.put("id", id.toString());

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    mockMvc.perform(put("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldNotUpdateCalculationWhenChangeEndsAfterCctDate() throws Exception {
    CctCalculation entity = template.insert(savedCalculation(TRAINEE_ID));

    UUID id = entity.id();
    ObjectNode request = calculateJson("""
        [
          {
            "type": "LTFT",
            "startDate": "2030-01-01",
            "endDate": "2032-12-31",
            "wte": 0.5,
            "countsTowardsCompletion": true,
            "notes": ""
          }
        ]
        """);
    request.put("id", id.toString());
    request.put("name", "Test Calculation updated");

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    mockMvc.perform(put("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/changes[0]/endDate")))
        .andExpect(jsonPath("$.errors[0].detail", is(
            "End date cannot be after the current projected Completion of Training Date "
                + "(26/02/2031).")));

    CctCalculation savedEntity = template.findById(id, CctCalculation.class);
    assertThat("Unexpected missing saved entity.", savedEntity, notNullValue());
    assertThat("Unexpected saved entity name.", savedEntity.name(), is("Test Calculation"));
  }

  @Test
  void shouldUpdateCalculation() throws Exception {
    CctCalculation entity = template.insert(savedCalculation(TRAINEE_ID));

    UUID id = entity.id();
    Instant created = entity.created().truncatedTo(ChronoUnit.MILLIS);
    assertThat("Unexpected initial last modified date.", entity.created(),
        is(entity.lastModified()));

    calculationJson.put("id", id.toString());
    calculationJson.put("name", "Test Calculation updated");

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    MvcResult result = mockMvc.perform(put("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.name").value("Test Calculation updated"))
        .andExpect(jsonPath("$.changes[0].wteMonths").value(3.6098630136986296))
        .andExpect(jsonPath("$.result.cctDate").value("2030-10-10"))
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
    assertThat("Unexpected updated entity created.", updatedEntity.created(), is(created));
    assertThat("Unexpected updated entity CCT date.", updatedEntity.result().cctDate(),
        is(LocalDate.of(2030, 10, 10)));
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
    UUID id = template.insert(savedCalculation(TRAINEE_ID)).id();

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    mockMvc.perform(delete("/api/cct/calculation/" + id)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));

    assertThat("Unexpected deleted entity.", template.findById(id, CctCalculation.class),
        nullValue());
  }

  @Test
  void shouldBeForbiddenCalculatingWhenNoToken() throws Exception {
    mockMvc.perform(post("/api/cct/calculate"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldBeForbiddenCalculatingWhenNoTraineeId() throws Exception {
    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(post("/api/cct/calculate")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldCalculateWhenNoChanges() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculate")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculateJson("[]").toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {
              "changes": [],
              "result": {
                "cctDate": "2030-07-29",
                "originalProgrammeEndDate": "2030-07-29",
                "adjustedProgrammeEndDate": "2030-07-29",
                "totalWteMonthsCompleted": 0,
                "monthsRemaining": 84,
                "assumedFullTimePeriods": [],
                "projection": {
                  "startDate": "2023-08-02",
                  "wte": 1,
                  "calendarMonths": 84,
                  "wteMonths": 84
                }
              }
            }
            """, JsonCompareMode.STRICT));
  }

  @Test
  void shouldCalculateWhenDatedLtftChange() throws Exception {
    ObjectNode request = calculateJson("""
        [
          {
            "type": "LTFT",
            "startDate": "2024-02-01",
            "endDate": "2024-08-01",
            "wte": 0.6,
            "countsTowardsCompletion": true,
            "notes": "Cardiology rotation"
          }
        ]
        """);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculate")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {
              "changes": [
                {
                  "type": "LTFT",
                  "startDate": "2024-02-01",
                  "endDate": "2024-08-01",
                  "wte": 0.6,
                  "countsTowardsCompletion": true,
                  "notes": "Cardiology rotation",
                  "calendarMonths": 6.016438356164383,
                  "wteMonths": 3.6098630136986296
                }
              ],
              "result": {
                "cctDate": "2030-10-10",
                "originalProgrammeEndDate": "2030-07-29",
                "adjustedProgrammeEndDate": "2030-07-29",
                "totalWteMonthsCompleted": 9.626301369863013,
                "monthsRemaining": 74.37,
                "assumedFullTimePeriods": [
                  {
                    "startDate": "2023-08-02",
                    "endDate": "2024-01-31",
                    "months": 6.016438356164383
                  }
                ],
                "projection": {
                  "startDate": "2024-08-02",
                  "wte": 1,
                  "calendarMonths": 74.37,
                  "wteMonths": 74.37
                }
              }
            }
            """, JsonCompareMode.STRICT));
  }

  @Test
  void shouldCalculateWhenOpenLtftChange() throws Exception {
    ObjectNode request = calculateJson("""
        [
          {
            "type": "LTFT",
            "startDate": "2025-02-01",
            "endDate": null,
            "wte": 0.6,
            "countsTowardsCompletion": true,
            "notes": ""
          }
        ]
        """);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculate")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {
              "changes": [
                {
                  "type": "LTFT",
                  "startDate": "2025-02-01",
                  "endDate": null,
                  "wte": 0.6,
                  "countsTowardsCompletion": true,
                  "notes": "",
                  "calendarMonths": null,
                  "wteMonths": null
                }
              ],
              "result": {
                "cctDate": "2034-03-27",
                "originalProgrammeEndDate": "2030-07-29",
                "adjustedProgrammeEndDate": "2030-07-29",
                "totalWteMonthsCompleted": 18.04931506849315,
                "monthsRemaining": 65.95,
                "assumedFullTimePeriods": [
                  {
                    "startDate": "2023-08-02",
                    "endDate": "2025-01-31",
                    "months": 18.04931506849315
                  }
                ],
                "projection": {
                  "startDate": "2025-02-01",
                  "wte": 0.6,
                  "calendarMonths": 109.91666666666667,
                  "wteMonths": 65.95
                }
              }
            }
            """, JsonCompareMode.STRICT));
  }

  @Test
  void shouldNotCalculateWhenValidationFails() throws Exception {
    ObjectNode request = calculateJson("""
        [
          {
            "type": "LTFT",
            "startDate": "2024-01-01",
            "endDate": "2023-12-31",
            "wte": 1,
            "countsTowardsCompletion": true
          }
        ]
        """);
    ObjectNode programmeDetails = (ObjectNode) request.get("programmeDetails");
    programmeDetails.put("specialty", "");
    programmeDetails.put("lengthMonths", 0);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculate")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(content().json("""
            {
              "type": "about:blank",
              "title": "Validation failure",
              "status": 400,
              "instance": "/api/cct/calculate",
              "errors": [
                {
                  "pointer": "#/changes[0]/startDate",
                  "detail": "Start date cannot be after end date."
                },
                {
                  "pointer": "#/changes[0]/wte",
                  "detail": "LTFT WTE must be a whole number between 1 and 99."
                },
                {
                  "pointer": "#/programmeDetails/lengthMonths",
                  "detail": "Programme length must be a whole number of months between 1 and 120."
                },
                {
                  "pointer": "#/programmeDetails/specialty",
                  "detail": "Please choose a specialty."
                }
              ]
            }
            """, JsonCompareMode.STRICT));
  }

  @Test
  void shouldNotCalculateWhenProgrammeDetailsAndChangesMissing() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculate")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title", is("Validation failure")))
        .andExpect(jsonPath("$.errors", hasSize(2)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/changes")))
        .andExpect(jsonPath("$.errors[0].detail", is("must not be null")))
        .andExpect(jsonPath("$.errors[1].pointer", is("#/programmeDetails")))
        .andExpect(jsonPath("$.errors[1].detail", is("must not be null")));
  }

  @Test
  void shouldNotCalculateWhenBodyMissing() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculate")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("Bad Request")))
        .andExpect(jsonPath("$.instance", is("/api/cct/calculate")))
        .andExpect(jsonPath("$.errors").doesNotExist());
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      startDate         | '"2023-02-30"'
      startGrade        | '"ST99"'
      lengthMonths      | 84.5
      additionalMonths  | 2.5
      acceleratedMonths | 2.5
      """)
  void shouldNotCalculateWhenProgrammeDetailsNotReadable(String field, String value)
      throws Exception {
    ObjectNode request = calculateJson("[]");
    ObjectNode programmeDetails = (ObjectNode) request.get("programmeDetails");
    programmeDetails.set(field, new ObjectMapper().readTree(value));
    programmeDetails.put("additionalMonthsNotes", "why");
    programmeDetails.put("acceleratedMonthsNotes", "why");

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculate")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("Bad Request")))
        .andExpect(jsonPath("$.detail", is("Failed to read request")))
        .andExpect(jsonPath("$.instance", is("/api/cct/calculate")))
        .andExpect(jsonPath("$.errors").doesNotExist());
  }

  @Test
  void shouldNotCalculateWhenChangeTypeNotReadable() throws Exception {
    ObjectNode request = calculateJson("""
        [
          {
            "type": "HOLIDAY",
            "startDate": "2024-01-01",
            "endDate": "2024-12-31",
            "wte": 0.5,
            "countsTowardsCompletion": true
          }
        ]
        """);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculate")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title", is("Bad Request")))
        .andExpect(jsonPath("$.detail", is("Failed to read request")))
        .andExpect(jsonPath("$.errors").doesNotExist());
  }

  @Test
  void shouldBeForbiddenGettingSpecialtiesWhenNoToken() throws Exception {
    mockMvc.perform(get("/api/cct/specialties"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldBeForbiddenGettingSpecialtiesWhenNoTraineeId() throws Exception {
    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(get("/api/cct/specialties")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldGetSpecialties() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(get("/api/cct/specialties")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(175)))
        .andExpect(jsonPath("$[0].name", is("ACCS anaesthetics")))
        .andExpect(jsonPath("$[0].lengthMonths", is(48)))
        .andExpect(jsonPath("$[0].entryGrade", is("CT1")));
  }

  private static CctCalculation savedCalculation(String traineeId) {
    return CctCalculation.builder()
        .traineeId(traineeId)
        .name("Test Calculation")
        .programmeDetails(CctProgrammeDetails.builder()
            .specialty("Paediatrics")
            .startDate(LocalDate.of(2023, 8, 2))
            .lengthMonths(84)
            .startGrade(ST1)
            .additionalMonths(0)
            .acceleratedMonths(0)
            .build())
        .changes(List.of(CctChange.builder()
            .type(LTFT)
            .startDate(LocalDate.of(2024, 2, 1))
            .endDate(LocalDate.of(2024, 8, 1))
            .wte(0.6)
            .countsTowardsCompletion(true)
            .notes("Cardiology rotation")
            .calendarMonths(6.0)
            .wteMonths(3.6)
            .build()))
        .result(CctResult.builder()
            .cctDate(LocalDate.of(2099, 1, 1))
            .originalProgrammeEndDate(LocalDate.of(2030, 7, 29))
            .adjustedProgrammeEndDate(LocalDate.of(2030, 7, 29))
            .totalWteMonthsCompleted(9.6)
            .monthsRemaining(74.4)
            .assumedFullTimePeriods(List.of(CctAssumedFullTimePeriod.builder()
                .startDate(LocalDate.of(2023, 8, 2))
                .endDate(LocalDate.of(2024, 1, 31))
                .months(6.0)
                .build()))
            .projection(CctProjection.builder()
                .startDate(LocalDate.of(2024, 8, 2))
                .wte(1.0)
                .calendarMonths(74.4)
                .wteMonths(74.4)
                .build())
            .build())
        .build();
  }

  private static ObjectNode calculateJson(String changes) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = (ObjectNode) mapper.readTree("""
        {
          "programmeDetails": {
            "specialty": "Paediatrics",
            "startDate": "2023-08-02",
            "lengthMonths": 84,
            "startGrade": "ST1",
            "startGradeOverrideNotes": "",
            "additionalMonths": 0,
            "additionalMonthsNotes": "",
            "acceleratedMonths": 0,
            "acceleratedMonthsNotes": "",
            "eighteenMonthFinalGrade": null,
            "eighteenMonthFinalGradeNotes": "",
            "skippedGrade": null,
            "skippedGradeNotes": ""
          }
        }
        """);
    json.set("changes", mapper.readTree(changes));
    return json;
  }
}
