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

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
  MockMvc mockMvc;

  @Autowired
  private MongoTemplate template;

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
    String body = """
        {
          "name": "Test Calculation"
        }
        """;

    mockMvc.perform(post("/api/cct/calculation")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldBeForbiddenCreatingCalculationWhenNoTraineeId() throws Exception {
    String body = """
        {
          "name": "Test Calculation"
        }
        """;

    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldNotCreateCalculationWhenIdPopulated() throws Exception {
    String body = """
        {
          "id": "%s",
          "name": "Test Calculation"
        }
        """.formatted(ObjectId.get());

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type", is("about:blank")))
        .andExpect(jsonPath("$.title", is("Validation failure")))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.instance", is("/api/cct/calculation")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/id")))
        .andExpect(jsonPath("$.errors[0].detail", is("must be null")));
  }

  @Test
  void shouldReturnCreatedCalculationJsonWhenIdNotPopulated() throws Exception {
    String body = """
        {
          "name": "Test Calculation"
        }
        """;

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(post("/api/cct/calculation")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.traineeId").doesNotExist())
        .andExpect(jsonPath("$.name").value("Test Calculation"));
  }

  @Test
  void shouldReturnLocationOfCreatedCalculationWhenIdNotPopulated() throws Exception {
    String body = """
        {
          "name": "Test Calculation"
        }
        """;

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    MvcResult result = mockMvc.perform(
            post("/api/cct/calculation")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
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
