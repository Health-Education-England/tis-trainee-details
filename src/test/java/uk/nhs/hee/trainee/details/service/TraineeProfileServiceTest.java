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

package uk.nhs.hee.trainee.details.service;

import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;
import uk.nhs.hee.trainee.details.service.impl.TraineeProfileServiceImpl;

@ExtendWith(MockitoExtension.class)
class TraineeProfileServiceTest {

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

  private static final String PLACEMENT_TISID1 = "1";
  private static final String PLACEMENT_SITE1 = "Addenbrookes Hospital";
  private static final Status PLACEMENT_STATUS1 = Status.CURRENT;
  private static final String PLACEMENT_TISID2 = "2";
  private static final String PLACEMENT_SITE2 = "Addenbrookes Hospital";
  private static final Status PLACEMENT_STATUS2 = Status.PAST;

  @InjectMocks
  private TraineeProfileServiceImpl traineeProfileServiceImpl;

  @Mock
  private TraineeProfileRepository traineeProfileRepositoryMock;

  private TraineeProfile traineeProfile;
  private PersonalDetails personalDetails;
  private ProgrammeMembership programmeMembership;
  private Curriculum curriculum;
  private Placement placement1;
  private Placement placement2;

  /**
   * Set up mocks before each test.
   */
  @BeforeEach
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
    traineeProfile.setPlacements(Lists.newArrayList(placement1, placement2));
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
    placement1 = new Placement();
    placement1.setPlacementTisId(PLACEMENT_TISID1);
    placement1.setSite(PLACEMENT_SITE1);
    placement1.setStatus(PLACEMENT_STATUS1);

    placement2 = new Placement();
    placement2.setPlacementTisId(PLACEMENT_TISID2);
    placement2.setSite(PLACEMENT_SITE2);
    placement2.setStatus(PLACEMENT_STATUS2);
  }

  @Test
  void getTraineeProfileByTraineeTisIdShouldReturnTraineeProfile() {
    when(traineeProfileRepositoryMock.findByTraineeTisId(DEFAULT_TIS_ID_1))
        .thenReturn(traineeProfile);
    TraineeProfile returnedTraineeProfile = traineeProfileServiceImpl
        .getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1);
    Assert.assertEquals(returnedTraineeProfile, traineeProfile);
  }

  @Test
  void hidePastProgrammesShouldHidePastProgrammes() {
    TraineeProfile returnedTraineeProfile = traineeProfileServiceImpl
        .hidePastProgrammes(traineeProfile);
    Assert.assertEquals(returnedTraineeProfile, traineeProfile);
  }

  @Test
  void hidePastPlacementsShouldHidePastPlacements() {
    TraineeProfile returnedTraineeProfile = traineeProfileServiceImpl
        .hidePastPlacements(traineeProfile);
    Assert.assertTrue(returnedTraineeProfile.getPlacements().contains(placement1));
    Assert.assertFalse(returnedTraineeProfile.getPlacements().contains(placement2));
  }
}
