/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (Health Education England)
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
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.trainee.details.DockerImageNames;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.event.EmailDetailsProvidedEvent;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
class BasicDetailsResourceIntegrationTest {

  private static final String TRAINEE_ID = UUID.randomUUID().toString();

  @Container
  @ServiceConnection
  private static final MongoDBContainer mongoContainer = new MongoDBContainer(
      DockerImageNames.MONGO);

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MongoTemplate mongoTemplate;

  @MockBean
  private SqsTemplate sqsTemplate;

  @MockBean
  private RestTemplate restTemplate;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SnsTemplate snsTemplate;

  private final String emailJson = "{\"email\": \"test@test.com\"}";;

  @AfterEach
  void tearDown() {
    mongoTemplate.findAllAndRemove(new Query(), TraineeProfile.class);
  }

  @Test
  void shouldBeBadRequestWhenUpdatingEmailWithNoToken() throws Exception {
    mockMvc.perform(put("/api/basic-details/email-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(emailJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldBeBadRequestWhenUpdatingEmailWithNoTraineeId() throws Exception {
    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(put("/api/basic-details/email-address")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(emailJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldReturnBadRequestWhenUpdatingEmailIsBlank() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    String blankEmail = "{\"email\": \"\"}";
    mockMvc.perform(put("/api/basic-details/email-address")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(blankEmail))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].detail", is("Email address must not be blank.")));
  }

  @Test
  void shouldReturnBadRequestWhenUpdatingEmailIsInvalid() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    String invalidEmail = "{\"email\": \"not-an-email\"}";
    mockMvc.perform(put("/api/basic-details/email-address")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidEmail))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].detail", is("Email address must be valid.")));
  }

  @Test
  void shouldReturnBadRequestWhenUpdatingEmailIsNotUnique() throws Exception {
    // Insert another profile with the same email
    PersonalDetails details = new PersonalDetails();
    details.setEmail("test@test.com");
    TraineeProfile otherProfile = new TraineeProfile();
    otherProfile.setTraineeTisId(UUID.randomUUID().toString());
    otherProfile.setPersonalDetails(details);
    mongoTemplate.save(otherProfile);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(put("/api/basic-details/email-address")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(emailJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].detail", is("Email address is already in use.")));
  }

  @Test
  void shouldReturnBadRequestWhenUpdatingEmailIsUnchanged() throws Exception {
    PersonalDetails details = new PersonalDetails();
    details.setEmail("test@test.com");
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    profile.setPersonalDetails(details);
    mongoTemplate.save(profile);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(put("/api/basic-details/email-address")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(emailJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].detail", is("Email address is already in use.")));
  }

  @Test
  void shouldSendEmailUpdateRequestSuccessfully() throws Exception {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);
    mongoTemplate.save(profile);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc
        .perform(
            put("/api/basic-details/email-address")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(emailJson))
        .andExpect(status().isNoContent());

    ArgumentCaptor<SnsNotification<EmailDetailsProvidedEvent>> notificationCaptor
        = ArgumentCaptor.forClass(SnsNotification.class);
    verify(snsTemplate, times(1))
        .sendNotification(eq("dummy"), notificationCaptor.capture());
    EmailDetailsProvidedEvent event = notificationCaptor.getValue().getPayload();
    assertThat("Unexpected trainee ID in event.", event.traineeId(), is(TRAINEE_ID));
    assertThat("Unexpected email in event.", event.emailDetails().getEmail(), is("test@test.com"));
  }
}