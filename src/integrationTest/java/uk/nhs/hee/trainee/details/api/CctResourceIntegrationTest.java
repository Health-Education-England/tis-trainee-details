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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.jayway.jsonpath.JsonPath;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.IOException;
import java.util.UUID;
import org.bson.types.ObjectId;
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
import uk.nhs.hee.trainee.details.model.CctCalculation;

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
            "wte": 0.5
          },
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
    ObjectId id = ObjectId.get();
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
    ObjectId id = ObjectId.get();
    CctCalculation entity = CctCalculation.builder()
        .id(id)
        .traineeId(TRAINEE_ID)
        .build();
    template.insert(entity);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(get("/api/cct/calculation/{id}", id)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.traineeId").doesNotExist());
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
    calculationJson.set("id", TextNode.valueOf(ObjectId.get().toString()));

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
            "wte": 0.5
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

  @ParameterizedTest
  @ValueSource(strings = {"type", "startDate", "wte"})
  void shouldFailCreateCalculationValidationWhenChangeTypeNull(String propertyName)
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
            "wte": 0.5
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
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(calculationJson.toString()))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.traineeId").doesNotExist())
        .andExpect(jsonPath("$.name").value("Test Calculation"));
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
}
