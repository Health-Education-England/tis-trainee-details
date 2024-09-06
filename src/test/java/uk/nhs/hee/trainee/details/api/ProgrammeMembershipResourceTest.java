/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.dto.ProgrammeMembershipDto;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.dto.signature.Signature;
import uk.nhs.hee.trainee.details.dto.signature.SignedDto;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapperImpl;
import uk.nhs.hee.trainee.details.mapper.SignatureMapperImpl;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.HeeUser;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.service.EventPublishService;
import uk.nhs.hee.trainee.details.service.ProgrammeMembershipService;
import uk.nhs.hee.trainee.details.service.SignatureService;

@ContextConfiguration(classes = {ProgrammeMembershipMapperImpl.class, SignatureMapperImpl.class})
@ExtendWith(SpringExtension.class)
@WebMvcTest(ProgrammeMembershipResource.class)
class ProgrammeMembershipResourceTest {

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private ProgrammeMembershipMapper programmeMembershipMapper;
  @Autowired
  private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @MockBean
  private ProgrammeMembershipService service;

  @MockBean
  private EventPublishService eventPublishService;

  @MockBean
  private SignatureService signatureService;

  @BeforeEach
  void setUp() {
    ProgrammeMembershipResource resource = new ProgrammeMembershipResource(service,
        programmeMembershipMapper, eventPublishService);
    mockMvc = MockMvcBuilders.standaloneSetup(resource)
        .setMessageConverters(jacksonMessageConverter)
        .build();
  }

  @Test
  void shouldReturnBadRequestWhenIdIsNull() throws Exception {
    when(service.updateProgrammeMembershipForTrainee("40", new ProgrammeMembership()))
        .thenReturn(Optional.empty());

    mockMvc.perform(patch("/api/programme-membership/{traineeTisId}", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(new ProgrammeMembershipDto())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnNotFoundStatusWhenTraineeNotFound() throws Exception {
    when(service.updateProgrammeMembershipForTrainee("40", new ProgrammeMembership()))
        .thenReturn(Optional.empty());

    ProgrammeMembershipDto dto = new ProgrammeMembershipDto();
    dto.setTisId("tisIdValue");

    mockMvc.perform(patch("/api/programme-membership/{traineeTisId}", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(dto)))
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Trainee not found."));
  }

  @Test
  void shouldUpdateProgrammeMembershipWhenTraineeFound() throws Exception {
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusYears(1);
    LocalDate completion = end.plusYears(1);

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("tisIdValue");
    programmeMembership.setProgrammeTisId("programmeTisIdValue");
    programmeMembership.setProgrammeName("programmeNameValue");
    programmeMembership.setProgrammeNumber("programmeNumberValue");
    programmeMembership.setManagingDeanery("managingDeaneryValue");
    programmeMembership.setDesignatedBody("designatedBodyValue");
    programmeMembership.setProgrammeMembershipType("programmeMembershipTypeValue");
    programmeMembership.setStartDate(start);
    programmeMembership.setEndDate(end);
    programmeMembership.setProgrammeCompletionDate(completion);

    HeeUser heeUser = new HeeUser();
    heeUser.setFirstName("firstnameValue");
    heeUser.setLastName("lastnameValue");
    heeUser.setEmailAddress("emailValue");
    heeUser.setPhoneNumber("phoneValue");
    heeUser.setGmcId("gmcValue");
    programmeMembership.setResponsibleOfficer(heeUser);

    when(service.updateProgrammeMembershipForTrainee(eq("40"), any(ProgrammeMembership.class)))
        .thenReturn(Optional.of(programmeMembership));

    Signature signature = new Signature(Duration.ofMinutes(60));
    signature.setHmac("not-really-a-hmac");
    doAnswer(inv -> {
      SignedDto dto = inv.getArgument(0);
      dto.setSignature(signature);
      return null;
    }).when(signatureService).signDto(any());

    ProgrammeMembershipDto dto = new ProgrammeMembershipDto();
    dto.setTisId("tisIdValue");

    mockMvc.perform(patch("/api/programme-membership/{traineeTisId}", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(dto)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.tisId").value(is("tisIdValue")))
        .andExpect(jsonPath("$.programmeTisId").value(is("programmeTisIdValue")))
        .andExpect(jsonPath("$.programmeName").value(is("programmeNameValue")))
        .andExpect(jsonPath("$.programmeNumber").value(is("programmeNumberValue")))
        .andExpect(jsonPath("$.managingDeanery").value(is("managingDeaneryValue")))
        .andExpect(jsonPath("$.designatedBody").value(is("designatedBodyValue")))
        .andExpect(jsonPath("$.programmeMembershipType").value(is("programmeMembershipTypeValue")))
        .andExpect(jsonPath("$.startDate").value(is(start.toString())))
        .andExpect(jsonPath("$.endDate").value(is(end.toString())))
        .andExpect(jsonPath("$.programmeCompletionDate").value(is(completion.toString())))
        .andExpect(jsonPath("$.responsibleOfficer.firstName").value(is("firstnameValue")))
        .andExpect(jsonPath("$.responsibleOfficer.lastName").value(is("lastnameValue")))
        .andExpect(jsonPath("$.responsibleOfficer.emailAddress").value(is("emailValue")))
        .andExpect(jsonPath("$.responsibleOfficer.gmcId").value(is("gmcValue")))
        .andExpect(jsonPath("$.responsibleOfficer.phoneNumber").value(is("phoneValue")))
        .andExpect(jsonPath("$.signature.hmac").value(signature.getHmac()))
        .andExpect(jsonPath("$.signature.signedAt").value(signature.getSignedAt().toString()))
        .andExpect(jsonPath("$.signature.validUntil").value(signature.getValidUntil().toString()));
  }

  @Test
  void shouldDeleteProgrammeMembershipWhenTraineeFound() throws Exception {
    when(service
        .deleteProgrammeMembershipForTrainee("40", "140"))
        .thenReturn(true);

    MvcResult result = mockMvc.perform(
            delete("/api/programme-membership/{traineeTisId}/{programmeMembershipId}", 40, 140)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Boolean resultBoolean = Boolean.parseBoolean(result.getResponse().getContentAsString());
    assertThat("Unexpected result.", resultBoolean, is(true));
  }

  @Test
  void shouldNotDeleteProgrammeMembershipWhenTraineeNotFound() throws Exception {
    when(service
        .deleteProgrammeMembershipForTrainee("40", "140"))
        .thenReturn(false);

    mockMvc.perform(
            delete("/api/programme-membership/{traineeTisId}/{programmeMembershipId}", 40, 140)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldThrowBadRequestWhenDeleteProgrammeMembershipException() throws Exception {
    when(service
        .deleteProgrammeMembershipForTrainee("triggersError", "triggersError"))
        .thenThrow(new IllegalArgumentException());

    mockMvc.perform(
            delete("/api/programme-membership/{traineeTisId}/{programmeMembershipId}",
                "triggersError", "triggersError")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldDeleteProgrammeMembershipsWhenTraineeFound() throws Exception {
    when(service
        .deleteProgrammeMembershipsForTrainee("40"))
        .thenReturn(true);

    MvcResult result = mockMvc.perform(
            delete("/api/programme-membership/{traineeTisId}", 40)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Boolean resultBoolean = Boolean.parseBoolean(result.getResponse().getContentAsString());
    assertThat("Unexpected result.", resultBoolean, is(true));
  }

  @Test
  void shouldNotDeleteProgrammeMembershipsWhenTraineeNotFound() throws Exception {
    when(service
        .deleteProgrammeMembershipsForTrainee("40"))
        .thenReturn(false);

    mockMvc.perform(
            delete("/api/programme-membership/{traineeTisId}", 40)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldThrowBadRequestWhenDeleteProgrammeMembershipsException() throws Exception {
    when(service
        .deleteProgrammeMembershipsForTrainee("triggersError"))
        .thenThrow(new IllegalArgumentException());

    mockMvc.perform(
            delete("/api/programme-membership/{traineeTisId}", "triggersError")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldReturnBadRequestWhenTokenNotFound() throws Exception {
    mockMvc.perform(post("/api/programme-membership/{programmeMembershipId}/sign-coj", 0)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldReturnBadRequestWhenTokenNotMap() throws Exception {
    String token = TestJwtUtil.generateToken("[]");

    mockMvc.perform(post("/api/programme-membership/{programmeMembershipId}/sign-coj", 0)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldReturnNotFoundWhenTisIdNotInToken() throws Exception {
    String token = TestJwtUtil.generateToken("{}");

    mockMvc.perform(post("/api/programme-membership/{programmeMembershipId}/sign-coj", 0)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnNotFoundStatusInSignCojWhenPmNotFound() throws Exception {
    when(service.signProgrammeMembershipCoj("tisIdValue", "0"))
        .thenReturn(Optional.empty());

    String token = TestJwtUtil.generateTokenForTisId("tisIdValue");
    mockMvc.perform(post("/api/programme-membership/{programmeMembershipId}/sign-coj", 0)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Trainee with Programme Membership 0 not found."));
  }

  @Test
  void shouldSignCojWhenTraineePmFound() throws Exception {
    final Instant signedAt = Instant.now();

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("tisIdValue");
    programmeMembership.setProgrammeTisId("programmeTisIdValue");
    programmeMembership.setProgrammeName("programmeNameValue");
    programmeMembership.setProgrammeNumber("programmeNumberValue");
    programmeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(signedAt, GoldGuideVersion.getLatest(), null));

    when(service.signProgrammeMembershipCoj("tisIdValue", "40"))
        .thenReturn(Optional.of(programmeMembership));

    ProgrammeMembershipDto dto = new ProgrammeMembershipDto();
    dto.setTisId("tisIdValue");

    String token = TestJwtUtil.generateTokenForTisId("tisIdValue");
    mockMvc.perform(post("/api/programme-membership/{programmeMembershipId}/sign-coj", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(dto))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.tisId").value(is("tisIdValue")))
        .andExpect(jsonPath("$.programmeTisId").value(is("programmeTisIdValue")))
        .andExpect(jsonPath("$.programmeName").value(is("programmeNameValue")))
        .andExpect(jsonPath("$.programmeNumber").value(is("programmeNumberValue")))
        .andExpect(jsonPath("$.conditionsOfJoining.signedAt").value(is(signedAt.toString())))
        .andExpect(jsonPath("$.conditionsOfJoining.version")
            .value(is(GoldGuideVersion.GG10.toString())))
        .andExpect(jsonPath("$.conditionsOfJoining.syncedAt")
            .isEmpty());
  }

  @Test
  void shouldNotPublishCojSignedEventWhenTraineePmNotFound() throws Exception {
    when(service.signProgrammeMembershipCoj("tisIdValue", "40"))
        .thenReturn(Optional.empty());

    ProgrammeMembershipDto dto = new ProgrammeMembershipDto();
    dto.setTisId("tisIdValue");

    String token = TestJwtUtil.generateTokenForTisId("tisIdValue");
    mockMvc.perform(post("/api/programme-membership/{programmeMembershipId}/sign-coj", 40)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(dto))
        .header(HttpHeaders.AUTHORIZATION, token));

    verifyNoInteractions(eventPublishService);
  }

  @Test
  void shouldPublishCojSignedEventWhenTraineePmFound() throws Exception {
    final Instant signedAt = Instant.now();

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("tisIdValue");
    programmeMembership.setProgrammeTisId("programmeTisIdValue");
    programmeMembership.setProgrammeName("programmeNameValue");
    programmeMembership.setProgrammeNumber("programmeNumberValue");
    programmeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(signedAt, GoldGuideVersion.getLatest(), null));

    when(service.signProgrammeMembershipCoj("tisIdValue", "40"))
        .thenReturn(Optional.of(programmeMembership));

    ProgrammeMembershipDto dto = new ProgrammeMembershipDto();
    dto.setTisId("tisIdValue");

    String token = TestJwtUtil.generateTokenForTisId("tisIdValue");
    mockMvc.perform(post("/api/programme-membership/{programmeMembershipId}/sign-coj", 40)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(dto))
        .header(HttpHeaders.AUTHORIZATION, token));

    verify(eventPublishService).publishCojSignedEvent(programmeMembership);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldReturnProgrammeMembershipNewStarterWhenTraineeFound(boolean isNewStarter)
      throws Exception {
    when(service
        .isNewStarter("40", "1"))
        .thenReturn(isNewStarter);

    mockMvc.perform(
            get("/api/programme-membership/isnewstarter/{traineeTisId}/{programmeMembershipId}",
                "40", "1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string(String.valueOf(isNewStarter)));
  }

  @Test
  void shouldThrowBadRequestWhenProgrammeMembershipNewStarterException() throws Exception {
    when(service
        .isNewStarter("triggersError", "1"))
        .thenThrow(new IllegalArgumentException());

    mockMvc.perform(
            get("/api/programme-membership/isnewstarter/{traineeTisId}/{programmeMembershipId}",
                "triggersError", "1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldReturnProgrammeMembershipPilot2024WhenTraineeFound(boolean isPilot2024)
      throws Exception {
    when(service
        .isPilot2024("40", "1"))
        .thenReturn(isPilot2024);

    mockMvc.perform(
            get("/api/programme-membership/ispilot2024/{traineeTisId}/{programmeMembershipId}",
                "40", "1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string(String.valueOf(isPilot2024)));
  }

  @Test
  void shouldThrowBadRequestWhenProgrammeMembershipPilot2024Exception() throws Exception {
    when(service
        .isPilot2024("triggersError", "1"))
        .thenThrow(new IllegalArgumentException());

    mockMvc.perform(
            get("/api/programme-membership/ispilot2024/{traineeTisId}/{programmeMembershipId}",
                "triggersError", "1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }
}
