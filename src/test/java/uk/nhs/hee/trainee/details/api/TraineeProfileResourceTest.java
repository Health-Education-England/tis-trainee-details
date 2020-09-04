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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.trainee.details.dto.CurriculumDto;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.dto.PlacementDto;
import uk.nhs.hee.trainee.details.dto.ProgrammeMembershipDto;
import uk.nhs.hee.trainee.details.dto.TraineeProfileDto;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapper;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

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


  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  private MockMvc mockMvc;

  @MockBean
  private TraineeProfileService traineeProfileServiceMock;

  @MockBean
  private TraineeProfileMapper traineeProfileMapperMock;

  private TraineeProfile traineeProfile;
  private PersonalDetails personalDetails;
  private ProgrammeMembership programmeMembership;
  private Curriculum curriculum;
  private Placement placement;

  private TraineeProfileDto traineeProfileDto;
  private PersonalDetailsDto personalDetailsDto;
  private ProgrammeMembershipDto programmeMembershipDto;
  private CurriculumDto curriculumDto;
  private PlacementDto placementDto;

  /**
   * Set up mocks before each test.
   */
  @BeforeEach
  void setup() {
    TraineeProfileResource traineeProfileResource = new TraineeProfileResource(
        traineeProfileServiceMock, traineeProfileMapperMock);
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

    traineeProfileDto = new TraineeProfileDto();
    traineeProfileDto.setId(DEFAULT_ID_1);
    traineeProfileDto.setTraineeTisId(DEFAULT_TIS_ID_1);
    traineeProfileDto.setPersonalDetails(personalDetailsDto);
    traineeProfileDto.setProgrammeMemberships(Lists.newArrayList(programmeMembershipDto));
    traineeProfileDto.setPlacements(Lists.newArrayList(placementDto));
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

    personalDetailsDto = new PersonalDetailsDto();
    personalDetailsDto.setSurname(PERSON_SURNAME);
    personalDetailsDto.setForenames(PERSON_FORENAME);
    personalDetailsDto.setKnownAs(PERSON_KNOWNAS);
    personalDetailsDto.setMaidenName(PERSON_MAIDENNAME);
    personalDetailsDto.setTitle(PERSON_TITLE);
    personalDetailsDto.setPersonOwner(PERSON_PERSONOWNER);
    personalDetailsDto.setDateOfBirth(PERSON_DATEOFBIRTH);
    personalDetailsDto.setGender(PERSON_GENDER);
    personalDetailsDto.setQualification(PERSON_QUALIFICATION);
    personalDetailsDto.setDateAttained(PERSON_DATEATTAINED);
    personalDetailsDto.setMedicalSchool(PERSON_MEDICALSCHOOL);
    personalDetailsDto.setTelephoneNumber(PERSON_TELEPHONENUMBER);
    personalDetailsDto.setMobileNumber(PERSON_MOBILE);
    personalDetailsDto.setEmail(PERSON_EMAIL);
    personalDetailsDto.setAddress1(PERSON_ADDRESS1);
    personalDetailsDto.setAddress2(PERSON_ADDRESS2);
    personalDetailsDto.setAddress3(PERSON_ADDRESS3);
    personalDetailsDto.setAddress4(PERSON_ADDRESS4);
    personalDetailsDto.setPostCode(PERSON_POSTCODE);
    personalDetailsDto.setGmcNumber(PERSON_GMC);
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

    programmeMembershipDto = new ProgrammeMembershipDto();
    programmeMembershipDto.setProgrammeTisId(PROGRAMME_TISID);
    programmeMembershipDto.setProgrammeName(PROGRAMME_NAME);
    programmeMembershipDto.setProgrammeNumber(PROGRAMME_NUMBER);
    programmeMembershipDto.setCurricula(Lists.newArrayList(curriculumDto));
  }

  /**
   * Set up data for curriculum.
   */
  void setupCurriculumData() {
    curriculum = new Curriculum();
    curriculum.setCurriculumTisId(CURRICULUM_TISID);
    curriculum.setCurriculumName(CURRICULUM_NAME);
    curriculum.setCurriculumSubType(CURRICULUM_SUBTYPE);

    curriculumDto = new CurriculumDto();
    curriculumDto.setCurriculumTisId(CURRICULUM_TISID);
    curriculumDto.setCurriculumName(CURRICULUM_NAME);
    curriculumDto.setCurriculumSubType(CURRICULUM_SUBTYPE);
  }

  /**
   * Set up data for placement.
   */
  void setupPlacementData() {
    placement = new Placement();
    placement.setPlacementTisId(PLACEMENT_TISID);
    placement.setSite(PLACEMENT_SITE);
    placement.setStatus(PLACEMENT_STATUS);

    placementDto = new PlacementDto();
    placementDto.setPlacementTisId(PLACEMENT_TISID);
    placementDto.setSite(PLACEMENT_SITE);
    placementDto.setStatus(PLACEMENT_STATUS);
  }


  @Test
  void testGetTraineeProfileById() throws Exception {
    when(traineeProfileServiceMock.getTraineeProfile(DEFAULT_ID_1)).thenReturn(traineeProfile);
    when(traineeProfileServiceMock.hidePastProgrammes(traineeProfile)).thenReturn(traineeProfile);
    when(traineeProfileServiceMock.hidePastPlacements(traineeProfile)).thenReturn(traineeProfile);
    when(traineeProfileMapperMock.toDto(traineeProfile)).thenReturn(traineeProfileDto);
    this.mockMvc.perform(MockMvcRequestBuilders.get("/api/trainee-profile/{id}", DEFAULT_ID_1)
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.traineeTisId").value(DEFAULT_TIS_ID_1))
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

  @Test
  void testGetTraineeProfileByTraineeTisId() throws Exception {
    when(traineeProfileServiceMock.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1))
        .thenReturn(traineeProfile);
    when(traineeProfileServiceMock.hidePastProgrammes(traineeProfile)).thenReturn(traineeProfile);
    when(traineeProfileServiceMock.hidePastPlacements(traineeProfile)).thenReturn(traineeProfile);
    when(traineeProfileMapperMock.toDto(traineeProfile)).thenReturn(traineeProfileDto);
    this.mockMvc.perform(
        MockMvcRequestBuilders.get("/api/trainee-profile/trainee/{traineeId}", DEFAULT_TIS_ID_1)
            .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
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
