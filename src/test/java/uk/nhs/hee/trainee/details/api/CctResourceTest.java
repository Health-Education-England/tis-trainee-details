/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationSummaryDto;
import uk.nhs.hee.trainee.details.service.CctService;

class CctResourceTest {

  private CctResource controller;
  private CctService service;

  @BeforeEach
  void setUp() {
    service = mock(CctService.class);
    controller = new CctResource(service);
  }

  @Test
  void shouldNotGetCalculationSummariesWhenCalculationsNotExist() {
    ObjectId id = ObjectId.get();
    when(service.getCalculation(id)).thenReturn(Optional.empty());

    ResponseEntity<List<CctCalculationSummaryDto>> response = controller.getCalculationSummaries();

    assertThat("Unexpected response code.", response.getStatusCode(), is(OK));
    assertThat("Unexpected response body.", response.getBody(), is(List.of()));
  }

  @Test
  void shouldGetCalculationSummariesWhenCalculationsExist() {
    ObjectId id1 = ObjectId.get();
    CctCalculationSummaryDto dto1 = CctCalculationSummaryDto.builder()
        .id(id1)
        .name("Test Calculation 1")
        .build();
    ObjectId id2 = ObjectId.get();
    CctCalculationSummaryDto dto2 = CctCalculationSummaryDto.builder()
        .id(id2)
        .name("Test Calculation 2")
        .build();
    when(service.getCalculations()).thenReturn(List.of(dto1, dto2));

    ResponseEntity<List<CctCalculationSummaryDto>> response = controller.getCalculationSummaries();

    assertThat("Unexpected response code.", response.getStatusCode(), is(OK));

    List<CctCalculationSummaryDto> responseDtos = response.getBody();
    assertThat("Unexpected response DTO count.", responseDtos, hasSize(2));

    CctCalculationSummaryDto responseDto1 = responseDtos.get(0);
    assertThat("Unexpected ID.", responseDto1.id(), is(id1));
    assertThat("Unexpected name.", responseDto1.name(), is("Test Calculation 1"));

    CctCalculationSummaryDto responseDto2 = responseDtos.get(1);
    assertThat("Unexpected ID.", responseDto2.id(), is(id2));
    assertThat("Unexpected name.", responseDto2.name(), is("Test Calculation 2"));
  }

  @Test
  void shouldNotGetCalculationDetailWhenCalculationNotExists() {
    ObjectId id = ObjectId.get();
    when(service.getCalculation(id)).thenReturn(Optional.empty());

    ResponseEntity<CctCalculationDetailDto> response = controller.getCalculationDetails(id);

    assertThat("Unexpected response code.", response.getStatusCode(), is(NOT_FOUND));
    assertThat("Unexpected response body.", response.getBody(), nullValue());
  }

  @Test
  void shouldGetCalculationDetailWhenCalculationExists() {
    ObjectId id = ObjectId.get();
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .id(id)
        .name("Test Calculation")
        .build();
    when(service.getCalculation(id)).thenReturn(Optional.of(dto));

    ResponseEntity<CctCalculationDetailDto> response = controller.getCalculationDetails(id);

    assertThat("Unexpected response code.", response.getStatusCode(), is(OK));
    assertThat("Unexpected response body.", response.getBody(), sameInstance(dto));
  }

  @Test
  void shouldReturnCreatedCalculation() {
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .name("Test Calculation")
        .build();

    ObjectId id = ObjectId.get();
    when(service.createCalculation(any())).thenAnswer(inv -> {
      CctCalculationDetailDto arg = inv.getArgument(0);
      return CctCalculationDetailDto.builder()
          .id(id)
          .name(arg.name())
          .build();
    });

    ResponseEntity<CctCalculationDetailDto> response = controller.createCalculation(dto);

    assertThat("Unexpected response code.", response.getStatusCode(), is(CREATED));

    CctCalculationDetailDto responseBody = response.getBody();
    assertThat("Unexpected response body.", responseBody, notNullValue());
    assertThat("Unexpected ID.", responseBody.id(), is(id));
    assertThat("Unexpected name.", responseBody.name(), is("Test Calculation"));


  }

  @Test
  void shouldReturnCreatedCalculationLocation() {
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .name("Test Calculation")
        .build();

    ObjectId id = ObjectId.get();
    when(service.createCalculation(any())).thenAnswer(inv -> {
      CctCalculationDetailDto arg = inv.getArgument(0);
      return CctCalculationDetailDto.builder()
          .id(id)
          .name(arg.name())
          .build();
    });

    ResponseEntity<CctCalculationDetailDto> response = controller.createCalculation(dto);

    HttpHeaders headers = response.getHeaders();
    assertThat("Unexpected response code.", headers.getLocation(),
        is(URI.create("/api/cct/calculation/" + id)));
  }

  @Test
  void shouldReturnCctCalculation() {
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .name("Test Calculation")
        .build();

    ObjectId id = ObjectId.get();
    when(service.calculateCctDate(any())).thenAnswer(inv -> {
      CctCalculationDetailDto arg = inv.getArgument(0);
      return Optional.of(CctCalculationDetailDto.builder()
          .id(id)
          .name(arg.name())
          .cctDate(LocalDate.MAX)
          .build());
    });

    ResponseEntity<CctCalculationDetailDto> response = controller.calculateCctDate(dto);

    assertThat("Unexpected response code.", response.getStatusCode(), is(OK));

    CctCalculationDetailDto responseBody = response.getBody();
    assertThat("Unexpected response body.", responseBody, notNullValue());
    assertThat("Unexpected CCT date.", responseBody.cctDate(), is(LocalDate.MAX));
    assertThat("Unexpected name.", responseBody.name(), is("Test Calculation"));
  }

  @Test
  void shouldReturnBadRequestIfErrorInCctCalculation() {
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .name("Test Calculation")
        .build();

    when(service.calculateCctDate(any())).thenReturn(Optional.empty());

    ResponseEntity<CctCalculationDetailDto> response = controller.calculateCctDate(dto);

    assertThat("Unexpected response code.", response.getStatusCode(), is(BAD_REQUEST));

    CctCalculationDetailDto responseBody = response.getBody();
    assertThat("Unexpected response body.", responseBody, nullValue());
  }
}