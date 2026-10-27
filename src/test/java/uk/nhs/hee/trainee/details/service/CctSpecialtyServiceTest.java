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

package uk.nhs.hee.trainee.details.service;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.CT1;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.DFT;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.F1;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST1;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST3;
import static uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade.ST4;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.dto.CctSpecialtyDto;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;

class CctSpecialtyServiceTest {

  private CctSpecialtyService service;

  @BeforeEach
  void setUp() throws IOException {
    service = new CctSpecialtyService(new ObjectMapper());
  }

  @Test
  void shouldGetEverySpecialtyWithUniqueName() {
    List<CctSpecialtyDto> specialties = service.getSpecialties();

    assertThat("Unexpected specialty count.", specialties, hasSize(175));
    long uniqueNames = specialties.stream().map(CctSpecialtyDto::name).distinct().count();
    assertThat("Unexpected unique name count.", uniqueNames, is(175L));
  }

  @Test
  void shouldGetSpecialtiesInListedOrder() {
    List<CctSpecialtyDto> specialties = service.getSpecialties();

    assertThat("Unexpected first specialty.", specialties.get(0),
        is(new CctSpecialtyDto("ACCS anaesthetics", 48, CT1)));
    assertThat("Unexpected last specialty.", specialties.get(specialties.size() - 1),
        is(new CctSpecialtyDto("Vascular surgery (run-through)", 96, ST1)));
  }

  @Test
  void shouldGetSpecialtyLengthsBetween12And102Months() {
    List<CctSpecialtyDto> specialties = service.getSpecialties();

    boolean inRange = specialties.stream()
        .allMatch(specialty -> specialty.lengthMonths() >= 12 && specialty.lengthMonths() <= 102);
    assertThat("Unexpected specialty length.", inRange, is(true));
  }

  @Test
  void shouldGetSpecialtyEntryGrades() {
    Map<CctTrainingGrade, Long> entryGrades = service.getSpecialties().stream()
        .collect(groupingBy(CctSpecialtyDto::entryGrade, counting()));

    assertThat("Unexpected entry grade counts.", entryGrades,
        is(Map.of(ST4, 67L, ST3, 61L, ST1, 35L, CT1, 10L, F1, 1L, DFT, 1L)));
  }

  @Test
  void shouldGetLocalOfficeRadiologySpecialties() {
    List<CctSpecialtyDto> specialties = service.getSpecialties();

    assertThat("Unexpected radiology specialties.", specialties, hasItems(
        new CctSpecialtyDto("Clinical radiology with Interventional radiology", 72, ST3),
        new CctSpecialtyDto("Interventional radiology with Clinical radiology", 72, ST3)));
  }
}
