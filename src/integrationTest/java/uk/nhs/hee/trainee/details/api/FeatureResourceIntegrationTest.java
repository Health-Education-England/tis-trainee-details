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

package uk.nhs.hee.trainee.details.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.nhs.hee.trainee.details.DockerImageNames.MONGO;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

@SpringBootTest(properties = "application.features.ltft.pilot.start-date=1970-01-01")
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
class FeatureResourceIntegrationTest {

  private static final String TRAINEE_ID = UUID.randomUUID().toString();

  @Container
  @ServiceConnection
  private static final MongoDBContainer mongoContainer = new MongoDBContainer(MONGO);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MongoTemplate template;

  @MockBean
  private SqsTemplate sqsTemplate;

  @AfterEach
  void tearDown() {
    template.findAllAndRemove(new Query(), TraineeProfile.class);
  }

  @Test
  void shouldDisableLtftWhenNoToken() throws Exception {
    mockMvc.perform(get("/api/features"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ltft", is(false)));
  }

  @Test
  void shouldHaveNoEnabledProgrammesWhenNoToken() throws Exception {
    mockMvc.perform(get("/api/features"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ltftProgrammes").isArray())
        .andExpect(jsonPath("$.ltftProgrammes", hasSize(0)));
  }

  @Test
  void shouldDisableLtftWhenNoTraineeId() throws Exception {
    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(get("/api/features")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ltft", is(false)));
  }

  @Test
  void shouldHaveNoEnabledProgrammesWhenNoTraineeId() throws Exception {
    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(get("/api/features")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ltftProgrammes").isArray())
        .andExpect(jsonPath("$.ltftProgrammes", hasSize(0)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"North West London", "North Central and East London", "South London",
      "South West"})
  void shouldEnableLtftAndSetLtftProgrammesWhenQualifyingProgrammeExists(String deanery)
      throws Exception {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    String pmId = UUID.randomUUID().toString();
    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setTisId(pmId);
    pm.setManagingDeanery(deanery);
    pm.setEndDate(LocalDate.now().plusDays(1));
    profile.setProgrammeMemberships(List.of(pm));

    template.save(profile);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(get("/api/features")
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ltft", is(true)))
        .andExpect(jsonPath("$.ltftProgrammes").isArray())
        .andExpect(jsonPath("$.ltftProgrammes", hasSize(1)))
        .andExpect(jsonPath("$.ltftProgrammes[0]", is(pmId)));
  }
}
