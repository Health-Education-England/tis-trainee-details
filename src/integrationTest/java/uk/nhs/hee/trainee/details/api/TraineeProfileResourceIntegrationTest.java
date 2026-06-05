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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.model.HeeUser;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TraineeProfileResourceIntegrationTest {

  private static final String TRAINEE_ID = UUID.randomUUID().toString();

  private static final String TIS_ID = "12345";
  private static final String F2_PLACEMENT_ID = "f2-placement-id";
  private static final String NON_F2_PLACEMENT_ID = "other-placement-id";
  private static final String PROGRAMME_MEMBERSHIP_ID = "pm-id";
  private static final String PROGRAMME_DESIGNATED_BODY = "NHSE Education East Midlands";
  private static final String PROGRAMME_RO_FIRST_NAME = "RO First Name";
  private static final String PROGRAMME_RO_LAST_NAME = "RO Last Name";

  @Container
  @ServiceConnection
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6");

  @Autowired
  MockMvc mockMvc;

  @Autowired
  MongoTemplate mongoTemplate;

  @MockitoBean
  SnsTemplate snsTemplate;

  @MockitoBean
  SqsTemplate sqsTemplate;

  @MockitoBean
  RestTemplate restTemplate;

  @AfterEach
  void tearDown() {
    mongoTemplate.remove(new Query(), TraineeProfile.class);
  }

  @Test
  void shouldReturnNoContentWhenUpdatingEmailWithNoToken() throws Exception {
    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            TIS_ID, F2_PLACEMENT_ID)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldReturnNoContentWhenUpdatingEmailWithNoTraineeId() throws Exception {
    String token = TestJwtUtil.generateToken("{}");
    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            TIS_ID, F2_PLACEMENT_ID)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldReturnNoContentWhenTraineeNotFound() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            "unknown-tis-id", F2_PLACEMENT_ID)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnProgrammeMembershipDetailsWhenPlacementIsFirstF2() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    TraineeProfile profile = buildProfile(TIS_ID,
        List.of(
            buildPlacement(F2_PLACEMENT_ID, "F2", LocalDate.of(2020, 1, 1))
        ),
        List.of(
            buildProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.of(2019, 8, 1),
                LocalDate.of(2022, 7, 31), PROGRAMME_DESIGNATED_BODY,
                PROGRAMME_RO_FIRST_NAME, PROGRAMME_RO_LAST_NAME)
        )
    );
    mongoTemplate.save(profile);

    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            TIS_ID, F2_PLACEMENT_ID)
        .header(HttpHeaders.AUTHORIZATION, token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tisId").value(PROGRAMME_MEMBERSHIP_ID))
        .andExpect(jsonPath("$.startDate").value("2019-08-01"))
        .andExpect(jsonPath("$.designatedBody").value(PROGRAMME_DESIGNATED_BODY))
        .andExpect(jsonPath("$.responsibleOfficer.firstName").value(PROGRAMME_RO_FIRST_NAME))
        .andExpect(jsonPath("$.responsibleOfficer.lastName").value(PROGRAMME_RO_LAST_NAME));
  }

  @Test
  void shouldReturnEarliestProgrammeWhenMultipleProgrammesWithFirstF2() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    TraineeProfile profile = buildProfile(TIS_ID,
        List.of(
            buildPlacement(F2_PLACEMENT_ID, "F2", LocalDate.of(2020, 1, 1))
        ),
        List.of(
            buildProgrammeMembership("pm-later", LocalDate.of(2020, 2, 1),
                LocalDate.of(2023, 1, 31), "dummy", "dummy", "dummy"),
            buildProgrammeMembership("pm-earlier", LocalDate.of(2019, 8, 1),
                LocalDate.of(2022, 7, 31), PROGRAMME_DESIGNATED_BODY,
                PROGRAMME_RO_FIRST_NAME, PROGRAMME_RO_LAST_NAME)
        )
    );
    mongoTemplate.save(profile);

    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            TIS_ID, F2_PLACEMENT_ID)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tisId").value("pm-earlier"))
        .andExpect(jsonPath("$.startDate").value("2019-08-01"))
        .andExpect(jsonPath("$.designatedBody").value(PROGRAMME_DESIGNATED_BODY))
        .andExpect(jsonPath("$.responsibleOfficer.firstName").value(PROGRAMME_RO_FIRST_NAME))
        .andExpect(jsonPath("$.responsibleOfficer.lastName").value(PROGRAMME_RO_LAST_NAME));
  }

  @Test
  void shouldReturnFirstF2WhenTraineeHasMultipleF2PlacementsAndProvidedPlacementIsFirst()
      throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    TraineeProfile profile = buildProfile(TIS_ID,
        List.of(
            buildPlacement(F2_PLACEMENT_ID,  "F2", LocalDate.of(2020, 1, 1)),
            buildPlacement("p-f2-002",       "F2", LocalDate.of(2022, 1, 1))
        ),
        List.of(
            buildProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.of(2019, 8, 1),
                LocalDate.of(2023, 7, 31), PROGRAMME_DESIGNATED_BODY,
                PROGRAMME_RO_FIRST_NAME, PROGRAMME_RO_LAST_NAME)
        )
    );
    mongoTemplate.save(profile);

    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            TIS_ID, F2_PLACEMENT_ID)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tisId").value(PROGRAMME_MEMBERSHIP_ID));
  }

  @Test
  void shouldReturnNoContentWhenPlacementIsNotAnF2() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    TraineeProfile profile = buildProfile(TIS_ID,
        List.of(
            buildPlacement(NON_F2_PLACEMENT_ID, "F3", LocalDate.of(2020, 1, 1))
        ),
        List.of(
            buildProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.of(2019, 8, 1),
                LocalDate.of(2022, 7, 31), PROGRAMME_DESIGNATED_BODY,
                PROGRAMME_RO_FIRST_NAME, PROGRAMME_RO_LAST_NAME)
        )
    );
    mongoTemplate.save(profile);

    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            TIS_ID, NON_F2_PLACEMENT_ID)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNoContentWhenTraineeHasNoF2Placements() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    TraineeProfile profile = buildProfile(TIS_ID,
        List.of(
            buildPlacement(NON_F2_PLACEMENT_ID, "F1", LocalDate.of(2019, 1, 1))
        ),
        List.of(
            buildProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.of(2018, 8, 1),
                LocalDate.of(2021, 7, 31), PROGRAMME_DESIGNATED_BODY,
                PROGRAMME_RO_FIRST_NAME, PROGRAMME_RO_LAST_NAME)
        )
    );
    mongoTemplate.save(profile);

    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            TIS_ID, F2_PLACEMENT_ID)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNoContentWhenProvidedPlacementIsNotTheFirstF2() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);

    TraineeProfile profile = buildProfile(TIS_ID,
        List.of(
            buildPlacement(F2_PLACEMENT_ID, "F2", LocalDate.of(2020, 1, 1)),
            buildPlacement("p-f2-002",      "F2", LocalDate.of(2022, 1, 1))
        ),
        List.of(
            buildProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.of(2019, 8, 1),
                LocalDate.of(2023, 7, 31), PROGRAMME_DESIGNATED_BODY,
                PROGRAMME_RO_FIRST_NAME, PROGRAMME_RO_LAST_NAME)
        )
    );
    mongoTemplate.save(profile);

    // The provided Placement is the later one
    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            TIS_ID, "p-f2-002")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNoContentWhenNoRelatedProgrammeMembershipFound() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    // Programme date not overlap with the F2 placement
    TraineeProfile profile = buildProfile(TIS_ID,
        List.of(
            buildPlacement(F2_PLACEMENT_ID, "F2", LocalDate.of(2022, 1, 1))
        ),
        List.of(
            buildProgrammeMembership(PROGRAMME_MEMBERSHIP_ID, LocalDate.of(2018, 8, 1),
                LocalDate.of(2019, 7, 31), PROGRAMME_DESIGNATED_BODY,
                PROGRAMME_RO_FIRST_NAME, PROGRAMME_RO_LAST_NAME)
        )
    );
    mongoTemplate.save(profile);

    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            TIS_ID, F2_PLACEMENT_ID)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNoContentWhenTraineeHasNoPlacementsAtAll() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    TraineeProfile profile = buildProfile(TIS_ID, List.of(), List.of());
    mongoTemplate.save(profile);

    mockMvc.perform(get("/api/trainee-profile/first-f2-programme/{tisId}/{placementId}",
            TIS_ID, F2_PLACEMENT_ID)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  private TraineeProfile buildProfile(String tisId, List<Placement> placements,
                                      List<ProgrammeMembership> programmeMemberships) {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(tisId);
    profile.setPlacements(placements);
    profile.setProgrammeMemberships(programmeMemberships);
    return profile;
  }

  private Placement buildPlacement(String tisId, String grade, LocalDate startDate) {
    Placement placement = new Placement();
    placement.setTisId(tisId);
    placement.setGrade(grade);
    placement.setStartDate(startDate);
    return placement;
  }

  private ProgrammeMembership buildProgrammeMembership(String tisId, LocalDate startDate,
                                                       LocalDate endDate, String designatedBody,
                                                       String roFirst, String roLast) {
    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setTisId(tisId);
    pm.setStartDate(startDate);
    pm.setProgrammeCompletionDate(endDate);
    pm.setDesignatedBody(designatedBody);

    HeeUser ro = new HeeUser();
    ro.setFirstName(roFirst);
    ro.setLastName(roLast);
    pm.setResponsibleOfficer(ro);
    return pm;
  }
}
