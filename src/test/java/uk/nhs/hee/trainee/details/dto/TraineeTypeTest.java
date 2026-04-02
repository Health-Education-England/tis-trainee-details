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

package uk.nhs.hee.trainee.details.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.nhs.hee.trainee.details.service.FeatureService.ACADEMIC_FOUNDATION_CURRICULUM_NAME;
import static uk.nhs.hee.trainee.details.service.FeatureService.FOUNDATION_SPECIALTY;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;

class TraineeTypeTest {

  @ParameterizedTest
  @ValueSource(strings = {FOUNDATION_SPECIALTY, "foundation"})
  void shouldReturnFoundationWhenCurriculumSpecialtyIsFoundation(String specialty) {
    ProgrammeMembership pm = new ProgrammeMembership();
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialty(specialty);
    pm.setCurricula(List.of(curriculum));

    TraineeType result = TraineeType.from(pm);

    assertThat("Unexpected trainee type.", result, is(TraineeType.FOUNDATION));
  }

  @ParameterizedTest
  @ValueSource(strings = {ACADEMIC_FOUNDATION_CURRICULUM_NAME, "academic foundation training"})
  void shouldReturnFoundationWhenCurriculumNameIsAcademicFoundationTraining(String name) {
    ProgrammeMembership pm = new ProgrammeMembership();
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumName(name);
    pm.setCurricula(List.of(curriculum));

    TraineeType result = TraineeType.from(pm);

    assertThat("Unexpected trainee type.", result, is(TraineeType.FOUNDATION));
  }

  @Test
  void shouldReturnFoundationBeforePublicHealthWhenBothMatch() {
    ProgrammeMembership pm = new ProgrammeMembership();
    Curriculum foundationCurriculum = new Curriculum();
    foundationCurriculum.setCurriculumSpecialty(FOUNDATION_SPECIALTY);
    Curriculum publicHealthCurriculum = new Curriculum();
    publicHealthCurriculum.setCurriculumSpecialty("Public Health Medicine");
    pm.setCurricula(List.of(foundationCurriculum, publicHealthCurriculum));

    TraineeType result = TraineeType.from(pm);

    assertThat("Unexpected trainee type.", result, is(TraineeType.FOUNDATION));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Public Health Medicine", "public health medicine"})
  void shouldReturnPublicHealthWhenCurriculumSpecialtyIsPublicHealthMedicine(String specialty) {
    ProgrammeMembership pm = new ProgrammeMembership();
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialty(specialty);
    curriculum.setCurriculumSubType("MEDICAL_CURRICULUM");
    pm.setCurricula(List.of(curriculum));

    TraineeType result = TraineeType.from(pm);

    assertThat("Unexpected trainee type.", result, is(TraineeType.PUBLIC_HEALTH));
  }

  @Test
  void shouldReturnSpecialtyWhenNoCurriculaMatchFoundationOrPublicHealth() {
    ProgrammeMembership pm = new ProgrammeMembership();
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialty("General Medicine");
    pm.setCurricula(List.of(curriculum));

    TraineeType result = TraineeType.from(pm);

    assertThat("Unexpected trainee type.", result, is(TraineeType.SPECIALTY));
  }

  @Test
  void shouldReturnSpecialtyWhenCurriculaIsEmpty() {
    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setCurricula(new ArrayList<>());

    TraineeType result = TraineeType.from(pm);

    assertThat("Unexpected trainee type.", result, is(TraineeType.SPECIALTY));
  }

  @Test
  void shouldReturnSpecialtyWhenCurriculumSpecialtyIsNull() {
    ProgrammeMembership pm = new ProgrammeMembership();
    Curriculum curriculum = new Curriculum();
    curriculum.setCurriculumSpecialty(null);
    pm.setCurricula(List.of(curriculum));

    TraineeType result = TraineeType.from(pm);

    assertThat("Unexpected trainee type.", result, is(TraineeType.SPECIALTY));
  }
}
