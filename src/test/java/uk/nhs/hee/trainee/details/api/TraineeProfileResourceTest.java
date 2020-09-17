/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapper;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

@ContextConfiguration(classes = TraineeProfileMapper.class)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = TraineeProfileResource.class)
class TraineeProfileResourceTest {

  private static final String TIS_ID_ATTRIBUTE = "custom:tisId";

  private static final String DEFAULT_ID_1 = "DEFAULT_ID_1";
  private static final String DEFAULT_TIS_ID_1 = "123";

  private static final String PERSON_SURNAME = "Gilliam";
  private static final String PERSON_FORENAME = "Anthony Mara";
  private static final String PERSON_KNOWNAS = "Ivy";
  private static final String PERSON_MAIDENNAME = "N/A";
  private static final String PERSON_TITLE = "Mr";
  private static final String PERSON_PERSONOWNER = "Health Education England Thames Valley";
  private static final LocalDate PERSON_DATEOFBIRTH = LocalDate.parse("1991-11-11",
      DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  private static final String PERSON_GENDER = "Male";
  private static final String PERSON_QUALIFICATION =
      "MBBS Bachelor of Medicine and Bachelor of Surgery";
  private static final LocalDate PERSON_DATEATTAINED = LocalDate.parse("2018-05-30",
      DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  private static final String PERSON_MEDICALSCHOOL = "University of Science and Technology";
  private static final String PERSON_TELEPHONENUMBER = "01632960363";
  private static final String PERSON_MOBILE = "08465879348";
  private static final String PERSON_EMAIL = "email@email.com";
  private static final String PERSON_ADDRESS1 = "585-6360 Interdum Street";
  private static final String PERSON_ADDRESS2 = "Goulburn";
  private static final String PERSON_ADDRESS3 = "London";
  private static final String PERSON_ADDRESS4 = "UK";
  private static final String PERSON_POSTCODE = "SW1A1AA";
  private static final String PERSON_GMC = "11111111";

  private static final String PROGRAMME_TISID = "1";
  private static final String PROGRAMME_NAME = "General Practice";
  private static final String PROGRAMME_NUMBER = "EOE8950";

  private static final String CURRICULUM_TISID = "1";
  private static final String CURRICULUM_NAME = "ST3";
  private static final String CURRICULUM_SUBTYPE = "MEDICAL_CURRICULUM";

  private static final String PLACEMENT_TISID = "1";
  private static final String PLACEMENT_SITE = "Addenbrookes Hospital";
  private static final Status PLACEMENT_STATUS = Status.CURRENT;

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  @Autowired
  private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @MockBean
  private TraineeProfileService traineeProfileServiceMock;

  private TraineeProfile traineeProfile;
  private PersonalDetails personalDetails;
  private ProgrammeMembership programmeMembership;
  private Curriculum curriculum;
  private Placement placement;

  /**
   * Set up mocks before each test.
   */
  @BeforeEach
  void setup() {
    TraineeProfileMapper mapper = Mappers.getMapper(TraineeProfileMapper.class);
    TraineeProfileResource traineeProfileResource = new TraineeProfileResource(
        traineeProfileServiceMock, mapper, objectMapper);
    this.mockMvc = MockMvcBuilders.standaloneSetup(traineeProfileResource)
        .setMessageConverters(jacksonMessageConverter)
        .build();

    setupData();
  }

  /**
   * Set up data for traineeProfile.
   */
  void setupData() {
    setupPersonalDetailsData();
    setupCurriculumData();
    setupProgrammeMembershipsData();
    setupPlacementData();

    traineeProfile = new TraineeProfile();
    traineeProfile.setId(DEFAULT_ID_1);
    traineeProfile.setTraineeTisId(DEFAULT_TIS_ID_1);
    traineeProfile.setPersonalDetails(personalDetails);
    traineeProfile.setProgrammeMemberships(Lists.newArrayList(programmeMembership));
    traineeProfile.setPlacements(Lists.newArrayList(placement));
  }

  /**
   * Set up data for personalDetails.
   */
  void setupPersonalDetailsData() {
    personalDetails = new PersonalDetails();
    personalDetails.setSurname(PERSON_SURNAME);
    personalDetails.setForenames(PERSON_FORENAME);
    personalDetails.setKnownAs(PERSON_KNOWNAS);
    personalDetails.setMaidenName(PERSON_MAIDENNAME);
    personalDetails.setTitle(PERSON_TITLE);
    personalDetails.setPersonOwner(PERSON_PERSONOWNER);
    personalDetails.setDateOfBirth(PERSON_DATEOFBIRTH);
    personalDetails.setGender(PERSON_GENDER);
    personalDetails.setQualification(PERSON_QUALIFICATION);
    personalDetails.setDateAttained(PERSON_DATEATTAINED);
    personalDetails.setMedicalSchool(PERSON_MEDICALSCHOOL);
    personalDetails.setTelephoneNumber(PERSON_TELEPHONENUMBER);
    personalDetails.setMobileNumber(PERSON_MOBILE);
    personalDetails.setEmail(PERSON_EMAIL);
    personalDetails.setAddress1(PERSON_ADDRESS1);
    personalDetails.setAddress2(PERSON_ADDRESS2);
    personalDetails.setAddress3(PERSON_ADDRESS3);
    personalDetails.setAddress4(PERSON_ADDRESS4);
    personalDetails.setPostCode(PERSON_POSTCODE);
    personalDetails.setGmcNumber(PERSON_GMC);
  }

  /**
   * Set up data for programmeMembership.
   */
  void setupProgrammeMembershipsData() {
    programmeMembership = new ProgrammeMembership();
    programmeMembership.setProgrammeTisId(PROGRAMME_TISID);
    programmeMembership.setProgrammeName(PROGRAMME_NAME);
    programmeMembership.setProgrammeNumber(PROGRAMME_NUMBER);
    programmeMembership.setCurricula(Lists.newArrayList(curriculum));
  }

  /**
   * Set up data for curriculum.
   */
  void setupCurriculumData() {
    curriculum = new Curriculum();
    curriculum.setCurriculumTisId(CURRICULUM_TISID);
    curriculum.setCurriculumName(CURRICULUM_NAME);
    curriculum.setCurriculumSubType(CURRICULUM_SUBTYPE);
  }

  /**
   * Set up data for placement.
   */
  void setupPlacementData() {
    placement = new Placement();
    placement.setPlacementTisId(PLACEMENT_TISID);
    placement.setSite(PLACEMENT_SITE);
    placement.setStatus(PLACEMENT_STATUS);
  }

  @Test
  void shouldReturnBadRequestWhenTokenNotFound() throws Exception {
    this.mockMvc.perform(
        MockMvcRequestBuilders.get("/api/trainee-profile")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequestWhenPayloadNotMap() throws Exception {
    String payload = "[]";
    String encodedPayload = Base64.getEncoder()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    String token = String.format("aGVhZGVy.%s.c2lnbmF0dXJl", encodedPayload);

    this.mockMvc.perform(
        MockMvcRequestBuilders.get("/api/trainee-profile")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnNotFoundWhenTisIdNotInToken() throws Exception {
    String payload = "{}";
    String encodedPayload = Base64.getEncoder()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    String token = String.format("aGVhZGVy.%s.c2lnbmF0dXJl", encodedPayload);

    this.mockMvc.perform(
        MockMvcRequestBuilders.get("/api/trainee-profile")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnNotFoundWhenTisIdNotExists() throws Exception {
    String payload = String.format("{\"%s\":\"40\"}", TIS_ID_ATTRIBUTE);
    String encodedPayload = Base64.getEncoder()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    String token = String.format("aGVhZGVy.%s.c2lnbmF0dXJl", encodedPayload);

    this.mockMvc.perform(
        MockMvcRequestBuilders.get("/api/trainee-profile")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnTraineeProfileWhenTisIdExists() throws Exception {
    String payload = String.format("{\"%s\":\"%s\"}", TIS_ID_ATTRIBUTE, DEFAULT_TIS_ID_1);
    String encodedPayload = Base64.getEncoder()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    String token = String.format("aGVhZGVy.%s.c2lnbmF0dXJl", encodedPayload);

    when(traineeProfileServiceMock.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1))
        .thenReturn(traineeProfile);
    when(traineeProfileServiceMock.hidePastProgrammes(traineeProfile)).thenReturn(traineeProfile);
    when(traineeProfileServiceMock.hidePastPlacements(traineeProfile)).thenReturn(traineeProfile);
    this.mockMvc.perform(
        MockMvcRequestBuilders.get("/api/trainee-profile")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(DEFAULT_ID_1))
        .andExpect(jsonPath("$.personalDetails.surname").value(PERSON_SURNAME))
        .andExpect(jsonPath("$.personalDetails.forenames").value(PERSON_FORENAME))
        .andExpect(jsonPath("$.personalDetails.knownAs").value(PERSON_KNOWNAS))
        .andExpect(jsonPath("$.personalDetails.maidenName").value(PERSON_MAIDENNAME))
        .andExpect(jsonPath("$.personalDetails.title").value(PERSON_TITLE))
        .andExpect(jsonPath("$.personalDetails.personOwner").value(PERSON_PERSONOWNER))
        .andExpect(jsonPath("$.personalDetails.dateOfBirth").value(PERSON_DATEOFBIRTH.toString()))
        .andExpect(jsonPath("$.personalDetails.gender").value(PERSON_GENDER))
        .andExpect(jsonPath("$.personalDetails.qualification").value(PERSON_QUALIFICATION))
        .andExpect(jsonPath("$.personalDetails.dateAttained").value(PERSON_DATEATTAINED.toString()))
        .andExpect(jsonPath("$.personalDetails.medicalSchool").value(PERSON_MEDICALSCHOOL))
        .andExpect(jsonPath("$.personalDetails.telephoneNumber").value(PERSON_TELEPHONENUMBER))
        .andExpect(jsonPath("$.personalDetails.mobileNumber").value(PERSON_MOBILE))
        .andExpect(jsonPath("$.personalDetails.email").value(PERSON_EMAIL))
        .andExpect(jsonPath("$.personalDetails.address1").value(PERSON_ADDRESS1))
        .andExpect(jsonPath("$.personalDetails.address2").value(PERSON_ADDRESS2))
        .andExpect(jsonPath("$.personalDetails.address3").value(PERSON_ADDRESS3))
        .andExpect(jsonPath("$.personalDetails.address4").value(PERSON_ADDRESS4))
        .andExpect(jsonPath("$.personalDetails.postCode").value(PERSON_POSTCODE))
        .andExpect(jsonPath("$.personalDetails.gmcNumber").value(PERSON_GMC))
        .andExpect(jsonPath("$.programmeMemberships[*].programmeTisId").value(PROGRAMME_TISID))
        .andExpect(jsonPath("$.programmeMemberships[*].programmeName").value(PROGRAMME_NAME))
        .andExpect(jsonPath("$.programmeMemberships[*].programmeNumber").value(PROGRAMME_NUMBER))
        .andExpect(jsonPath("$.programmeMemberships[*].curricula[*].curriculumTisId")
            .value(CURRICULUM_TISID))
        .andExpect(jsonPath("$.programmeMemberships[*].curricula[*].curriculumName")
            .value(CURRICULUM_NAME))
        .andExpect(jsonPath("$.programmeMemberships[*].curricula[*].curriculumSubType")
            .value(CURRICULUM_SUBTYPE))
        .andExpect(jsonPath("$.placements[*].placementTisId").value(PLACEMENT_TISID))
        .andExpect(jsonPath("$.placements[*].site").value(PLACEMENT_SITE))
        .andExpect(jsonPath("$.placements[*].status").value(PLACEMENT_STATUS.toString()));
  }
}
