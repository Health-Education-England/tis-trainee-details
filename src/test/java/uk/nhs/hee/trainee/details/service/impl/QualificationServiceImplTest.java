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

package uk.nhs.hee.trainee.details.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.nhs.hee.trainee.details.mapper.QualificationMapper;
import uk.nhs.hee.trainee.details.model.Qualification;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;
import uk.nhs.hee.trainee.details.service.QualificationService;

class QualificationServiceImplTest {

  private static final LocalDate DATE = LocalDate.EPOCH;
  private static final String QUALIFICATION = "qualification-";
  private static final String MEDICAL_SCHOOL = "medicalSchool-";
  private static final String TRAINEE_TIS_ID = "40";
  private static final String MODIFIED_SUFFIX = "post";
  private static final String ORIGINAL_SUFFIX = "pre";
  private static final String NEW_QUALIFICATION_ID = "1";
  private static final String EXISTING_QUALIFICATION_ID = "2";

  private QualificationService service;
  private TraineeProfileRepository repository;

  @BeforeEach
  void setUp() {
    repository = mock(TraineeProfileRepository.class);
    TraineeProfileServiceImpl profileService = new TraineeProfileServiceImpl(repository);
    service = new QualificationServiceImpl(profileService,
        Mappers.getMapper(QualificationMapper.class));
  }

  @Test
  void shouldNotUpdateQualificationWhenTraineeIdNotFound() {
    Optional<Qualification> qualification = service
        .updateQualificationByTisId("notFound", new Qualification());

    assertThat("Unexpected optional isEmpty flag.", qualification.isEmpty(), is(true));
    verify(repository).findByTraineeTisId("notFound");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldAddQualificationWhenTraineeFoundAndNoQualificationsExists() {
    TraineeProfile traineeProfile = new TraineeProfile();

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Qualification> qualification = service.updateQualificationByTisId(TRAINEE_TIS_ID,
        createQualification(NEW_QUALIFICATION_ID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", qualification.isEmpty(), is(false));

    Qualification expectedQualification = new Qualification();
    expectedQualification.setTisId(NEW_QUALIFICATION_ID);
    expectedQualification.setQualification(QUALIFICATION + MODIFIED_SUFFIX);
    expectedQualification.setDateAttained(DATE.plusDays(100));
    expectedQualification.setMedicalSchool(MEDICAL_SCHOOL + MODIFIED_SUFFIX);

    assertThat("Unexpected qualification.", qualification.get(), is(expectedQualification));
  }

  @Test
  void shouldAddQualificationWhenTraineeFoundAndQualificationNotExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getQualifications()
        .add(createQualification(EXISTING_QUALIFICATION_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Qualification> qualification = service.updateQualificationByTisId(TRAINEE_TIS_ID,
        createQualification(NEW_QUALIFICATION_ID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", qualification.isEmpty(), is(false));

    Qualification expectedQualification = new Qualification();
    expectedQualification.setTisId(NEW_QUALIFICATION_ID);
    expectedQualification.setQualification(QUALIFICATION + MODIFIED_SUFFIX);
    expectedQualification.setDateAttained(DATE.plusDays(100));
    expectedQualification.setMedicalSchool(MEDICAL_SCHOOL + MODIFIED_SUFFIX);

    assertThat("Unexpected qualification.", qualification.get(), is(expectedQualification));
  }

  @Test
  void shouldUpdateQualificationWhenTraineeFoundAndQualificationExists() {
    TraineeProfile traineeProfile = new TraineeProfile();
    traineeProfile.getQualifications()
        .add(createQualification(EXISTING_QUALIFICATION_ID, ORIGINAL_SUFFIX, 0));

    when(repository.findByTraineeTisId(TRAINEE_TIS_ID)).thenReturn(traineeProfile);
    when(repository.save(traineeProfile)).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<Qualification> qualification = service.updateQualificationByTisId(TRAINEE_TIS_ID,
        createQualification(EXISTING_QUALIFICATION_ID, MODIFIED_SUFFIX, 100));

    assertThat("Unexpected optional isEmpty flag.", qualification.isEmpty(), is(false));

    Qualification expectedQualification = createQualification(EXISTING_QUALIFICATION_ID,
        ORIGINAL_SUFFIX, 0);
    expectedQualification.setTisId(EXISTING_QUALIFICATION_ID);
    expectedQualification.setQualification(QUALIFICATION + MODIFIED_SUFFIX);
    expectedQualification.setDateAttained(DATE.plusDays(100));
    expectedQualification.setMedicalSchool(MEDICAL_SCHOOL + MODIFIED_SUFFIX);

    assertThat("Unexpected qualification.", qualification.get(), is(expectedQualification));
  }

  /**
   * Create an instance of Qualification with default dummy values.
   *
   * @param tisId              The TIS ID to set on the qualification.
   * @param stringSuffix       The suffix to use for string values.
   * @param dateAdjustmentDays The number of days to add to dates.
   * @return The dummy entity.
   */
  private Qualification createQualification(String tisId, String stringSuffix,
      int dateAdjustmentDays) {
    Qualification qualification = new Qualification();
    qualification.setTisId(tisId);
    qualification.setQualification(QUALIFICATION + stringSuffix);
    qualification.setDateAttained(DATE.plusDays(dateAdjustmentDays));
    qualification.setMedicalSchool(MEDICAL_SCHOOL + stringSuffix);

    return qualification;
  }
}
