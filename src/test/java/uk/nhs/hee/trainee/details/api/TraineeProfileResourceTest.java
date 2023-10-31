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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;
import uk.nhs.hee.trainee.details.dto.signature.Signature;
import uk.nhs.hee.trainee.details.dto.signature.SignedDto;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapperImpl;
import uk.nhs.hee.trainee.details.mapper.PlacementMapperImpl;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapperImpl;
import uk.nhs.hee.trainee.details.mapper.SignatureMapperImpl;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapper;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapperImpl;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.Site;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.service.SignatureService;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

@ContextConfiguration(classes = {TraineeProfileMapperImpl.class, PersonalDetailsMapperImpl.class,
    PlacementMapperImpl.class, ProgrammeMembershipMapperImpl.class, SignatureMapperImpl.class})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = TraineeProfileResource.class)
class TraineeProfileResourceTest {

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
  private static final Instant NOW = Instant.now();
  private static final Instant COJ_SYNCED_AT = Instant.MAX;

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  @Autowired
  private TraineeProfileMapper traineeProfileMapper;

  private MockMvc mockMvc;

  @MockBean
  private TraineeProfileService service;

  @MockBean
  private SignatureService signatureService;

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
    TraineeProfileResource traineeProfileResource = new TraineeProfileResource(service,
        traineeProfileMapper);
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
    traineeProfile.setProgrammeMemberships(List.of(programmeMembership));
    traineeProfile.setPlacements(List.of(placement));
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
    programmeMembership.setCurricula(List.of(curriculum));
    programmeMembership.setConditionsOfJoining(
        new ConditionsOfJoining(NOW, GoldGuideVersion.GG9, COJ_SYNCED_AT));
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
    placement.setTisId(PLACEMENT_TISID);
    placement.setStatus(PLACEMENT_STATUS);

    Site site = new Site();
    site.setName(PLACEMENT_SITE);
    placement.setSite(site);
  }

  @Test
  void getShouldReturnBadRequestWhenTokenNotFound() throws Exception {
    this.mockMvc.perform(get("/api/trainee-profile")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldReturnBadRequestWhenPayloadNotMap() throws Exception {
    String token = TestJwtUtil.generateToken("[]");

    this.mockMvc.perform(get("/api/trainee-profile")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldReturnNotFoundWhenTisIdNotInToken() throws Exception {
    String token = TestJwtUtil.generateToken("{}");

    this.mockMvc.perform(get("/api/trainee-profile")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound());
  }

  @Test
  void getShouldReturnNotFoundWhenTisIdNotExists() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId("40");

    this.mockMvc.perform(get("/api/trainee-profile")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound());
  }

  @Test
  void getShouldReturnTraineeProfileWhenTisIdExists() throws Exception {
    when(service.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(service.hidePastProgrammes(traineeProfile)).thenReturn(traineeProfile);
    when(service.hideDeprecatedProgrammes(traineeProfile)).thenReturn(traineeProfile);
    when(service.hidePastPlacements(traineeProfile)).thenReturn(traineeProfile);

    Signature signature = new Signature(Duration.ofMinutes(60));
    signature.setHmac("not-really-a-hmac");
    doAnswer(inv -> {
      SignedDto dto = inv.getArgument(0);
      dto.setSignature(signature);
      return null;
    }).when(signatureService).signDto(any());

    String token = TestJwtUtil.generateTokenForTisId(DEFAULT_TIS_ID_1);
    this.mockMvc.perform(get("/api/trainee-profile")
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
        .andExpect(jsonPath("$.personalDetails.signature.hmac").value(signature.getHmac()))
        .andExpect(jsonPath("$.personalDetails.signature.signedAt").value(
            signature.getSignedAt().toString()))
        .andExpect(jsonPath("$.personalDetails.signature.validUntil").value(
            signature.getValidUntil().toString()))
        .andExpect(jsonPath("$.programmeMemberships[*].programmeTisId").value(PROGRAMME_TISID))
        .andExpect(jsonPath("$.programmeMemberships[*].programmeName").value(PROGRAMME_NAME))
        .andExpect(jsonPath("$.programmeMemberships[*].programmeNumber").value(PROGRAMME_NUMBER))
        .andExpect(jsonPath("$.programmeMemberships[*].curricula[*].curriculumTisId")
            .value(CURRICULUM_TISID))
        .andExpect(jsonPath("$.programmeMemberships[*].curricula[*].curriculumName")
            .value(CURRICULUM_NAME))
        .andExpect(jsonPath("$.programmeMemberships[*].curricula[*].curriculumSubType")
            .value(CURRICULUM_SUBTYPE))
        .andExpect(jsonPath("$.programmeMemberships[*].conditionsOfJoining.signedAt").value(
            NOW.toString()))
        .andExpect(jsonPath("$.programmeMemberships[*].conditionsOfJoining.version").value(
            GoldGuideVersion.GG9.toString()))
        .andExpect(
            jsonPath("$.programmeMemberships[*].conditionsOfJoining.syncedAt")
                .value(COJ_SYNCED_AT.toString()))
        .andExpect(jsonPath("$.programmeMemberships[*].signature.hmac").value(signature.getHmac()))
        .andExpect(jsonPath("$.programmeMemberships[*].signature.signedAt")
            .value(signature.getSignedAt().toString()))
        .andExpect(jsonPath("$.programmeMemberships[*].signature.validUntil")
            .value(signature.getValidUntil().toString()))
        .andExpect(jsonPath("$.placements[*].tisId").value(PLACEMENT_TISID))
        .andExpect(jsonPath("$.placements[*].site").value(PLACEMENT_SITE))
        .andExpect(jsonPath("$.placements[*].status").value(PLACEMENT_STATUS.toString()))
        .andExpect(jsonPath("$.placements[*].signature.hmac").value(signature.getHmac()))
        .andExpect(jsonPath("$.placements[*].signature.signedAt")
            .value(signature.getSignedAt().toString()))
        .andExpect(jsonPath("$.placements[*].signature.validUntil")
            .value(signature.getValidUntil().toString()));
  }

  @Test
  void getShouldHandleUrlCharactersInToken() throws Exception {
    // The payload is specifically crafted to include an underscore.
    String token = "aGVhZGVy.eyJjdXN0b206dGlzSWQiOiAiMTIiLCJuYW1lIjogIkpvaG4gRG_DqyJ9.c2lnbmF0dXJl";

    this.mockMvc.perform(get("/api/trainee-profile")
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, token));

    verify(service).getTraineeProfileByTraineeTisId("12");
  }

  @Test
  void shouldReturnTraineeIdWhenProfileFoundByEmail() throws Exception {
    when(service.getTraineeTisIdsByEmail(PERSON_EMAIL))
        .thenReturn(List.of(DEFAULT_TIS_ID_1, "id2"));

    mockMvc.perform(get("/api/trainee-profile/trainee-ids")
            .param("email", PERSON_EMAIL)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0]").value(DEFAULT_TIS_ID_1))
        .andExpect(jsonPath("$[1]").value("id2"));
  }

  @Test
  void shouldReturnNotFoundWhenProfileNotFoundByEmail() throws Exception {
    when(service.getTraineeTisIdsByEmail(PERSON_EMAIL)).thenReturn(Collections.emptyList());

    mockMvc.perform(get("/api/trainee-profile/trainee-ids")
            .param("email", PERSON_EMAIL)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldReturnTraineeIdWhenProfileFoundByEmailGmcAndDob() throws Exception {
    when(service.getTraineeTisIdsByEmailGmcAndDob(PERSON_EMAIL, PERSON_GMC, PERSON_DATEOFBIRTH))
        .thenReturn(List.of(DEFAULT_TIS_ID_1));
    mockMvc.perform(get("/api/trainee-profile/trainee-verify")
            .param("email", PERSON_EMAIL)
            .param("gmc", PERSON_GMC)
            .param("dob", PERSON_DATEOFBIRTH.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").value(DEFAULT_TIS_ID_1));
  }

  @Test
  void shouldReturnNotFoundWhenProfileNotFoundByEmailGmcAndDob() throws Exception {
    when(service.getTraineeTisIdsByEmailGmcAndDob(PERSON_EMAIL, PERSON_GMC, PERSON_DATEOFBIRTH))
        .thenReturn(Collections.emptyList());

    mockMvc.perform(get("/api/trainee-profile/trainee-verify")
            .param("email", PERSON_EMAIL)
            .param("gmc", PERSON_GMC)
            .param("dob", PERSON_DATEOFBIRTH.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldReturnNotFoundWhenMultipleProfilesFoundByEmailGmcAndDob() throws Exception {
    when(service.getTraineeTisIdsByEmailGmcAndDob(PERSON_EMAIL, PERSON_GMC, PERSON_DATEOFBIRTH))
        .thenReturn(List.of(DEFAULT_TIS_ID_1, "id2"));

    mockMvc.perform(get("/api/trainee-profile/trainee-verify")
            .param("email", PERSON_EMAIL)
            .param("gmc", PERSON_GMC)
            .param("dob", PERSON_DATEOFBIRTH.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void shouldReturnNoContentWhenDeletingProfile() throws Exception {
    mockMvc.perform(delete("/api/trainee-profile/{tisId}", "1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent())
        .andExpect(jsonPath("$").doesNotExist());
  }

  @Test
  void getEmailShouldReturnNotFoundWhenProfileNotFoundByTisId() throws Exception {
    mockMvc.perform(get("/api/trainee-profile/trainee-email")
            .contentType(MediaType.APPLICATION_JSON)
            .param("id", "non existent tis id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getEmailShouldReturnEmailWhenProfileFoundByTisId() throws Exception {
    when(service.getTraineeEmailByTisId(DEFAULT_TIS_ID_1)).thenReturn(PERSON_EMAIL);
    mockMvc.perform(get("/api/trainee-profile/trainee-email")
            .param("id", DEFAULT_TIS_ID_1))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").value(PERSON_EMAIL));
  }
}
