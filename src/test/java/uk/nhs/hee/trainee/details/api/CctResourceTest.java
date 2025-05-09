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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
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
  void shouldNotGetCalculationDetailsWhenCalculationsNotExist() {
    UUID id = UUID.randomUUID();
    when(service.getCalculation(id)).thenReturn(Optional.empty());

    ResponseEntity<List<CctCalculationDetailDto>> response = controller.getCalculationDetails();

    assertThat("Unexpected response code.", response.getStatusCode(), is(OK));
    assertThat("Unexpected response body.", response.getBody(), is(List.of()));
  }

  @Test
  void shouldGetCalculationDetailsWhenCalculationsExist() {
    UUID id1 = UUID.randomUUID();
    CctCalculationDetailDto dto1 = CctCalculationDetailDto.builder()
        .id(id1)
        .name("Test Calculation 1")
        .build();
    UUID id2 = UUID.randomUUID();
    CctCalculationDetailDto dto2 = CctCalculationDetailDto.builder()
        .id(id2)
        .name("Test Calculation 2")
        .build();
    when(service.getCalculations()).thenReturn(List.of(dto1, dto2));

    ResponseEntity<List<CctCalculationDetailDto>> response = controller.getCalculationDetails();

    assertThat("Unexpected response code.", response.getStatusCode(), is(OK));

    List<CctCalculationDetailDto> responseDtos = response.getBody();
    assertThat("Unexpected response DTO count.", responseDtos, hasSize(2));

    CctCalculationDetailDto responseDto1 = responseDtos.get(0);
    assertThat("Unexpected ID.", responseDto1.id(), is(id1));
    assertThat("Unexpected name.", responseDto1.name(), is("Test Calculation 1"));

    CctCalculationDetailDto responseDto2 = responseDtos.get(1);
    assertThat("Unexpected ID.", responseDto2.id(), is(id2));
    assertThat("Unexpected name.", responseDto2.name(), is("Test Calculation 2"));
  }

  @Test
  void shouldNotGetCalculationDetailWhenCalculationNotExists() {
    UUID id = UUID.randomUUID();
    when(service.getCalculation(id)).thenReturn(Optional.empty());

    ResponseEntity<CctCalculationDetailDto> response = controller.getCalculationDetails(id);

    assertThat("Unexpected response code.", response.getStatusCode(), is(NOT_FOUND));
    assertThat("Unexpected response body.", response.getBody(), nullValue());
  }

  @Test
  void shouldGetCalculationDetailWhenCalculationExists() {
    UUID id = UUID.randomUUID();
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

    UUID id = UUID.randomUUID();
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

    UUID id = UUID.randomUUID();
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
  void shouldReturnUpdatedCalculation() throws MethodArgumentNotValidException {
    UUID id = UUID.randomUUID();
    Instant created = Instant.now();
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .id(id)
        .name("Test Calculation")
        .created(created)
        .lastModified(created)
        .build();

    Instant modified = created.plusSeconds(1);
    when(service.updateCalculation(any(), any())).thenAnswer(inv -> {
      CctCalculationDetailDto arg = inv.getArgument(1);
      return Optional.of(CctCalculationDetailDto.builder()
          .id(arg.id())
          .name(arg.name())
          .created(arg.created())
          .lastModified(modified)
          .build());
    });

    ResponseEntity<CctCalculationDetailDto> response = controller.updateCalculationDetails(id, dto);

    assertThat("Unexpected response code.", response.getStatusCode(), is(OK));

    CctCalculationDetailDto responseBody = response.getBody();
    assertThat("Unexpected response body.", responseBody, notNullValue());
    assertThat("Unexpected ID.", responseBody.id(), is(id));
    assertThat("Unexpected name.", responseBody.name(), is("Test Calculation"));
    assertThat("Unexpected created.", responseBody.created(), is(created));
    assertThat("Unexpected last modified.", responseBody.lastModified(), is(modified));
  }

  @Test
  void shouldReturnBadRequestWhenUpdateIsInconsistent() throws MethodArgumentNotValidException {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .id(id2)
        .name("Test Calculation")
        .build();

    ResponseEntity<CctCalculationDetailDto> response = controller.updateCalculationDetails(id1,
        dto);

    assertThat("Unexpected response code.", response.getStatusCode(), is(BAD_REQUEST));

    CctCalculationDetailDto responseBody = response.getBody();
    assertThat("Unexpected response body.", responseBody, nullValue());
  }

  @Test
  void shouldReturnBadRequestWhenUpdateHasNoEntityId() throws MethodArgumentNotValidException {
    UUID id = UUID.randomUUID();
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .name("Test Calculation")
        .build();

    ResponseEntity<CctCalculationDetailDto> response = controller.updateCalculationDetails(id, dto);

    assertThat("Unexpected response code.", response.getStatusCode(), is(BAD_REQUEST));

    CctCalculationDetailDto responseBody = response.getBody();
    assertThat("Unexpected response body.", responseBody, nullValue());
  }

  @Test
  void shouldReturnNotFoundWhenCalculationCannotBeUpdated() throws MethodArgumentNotValidException {
    UUID id = UUID.randomUUID();
    Instant created = Instant.now();
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .id(id)
        .name("Test Calculation")
        .created(created)
        .lastModified(created)
        .build();

    //e.g. because not owned by the user, or non-existent
    when(service.updateCalculation(any(), any())).thenReturn(Optional.empty());

    ResponseEntity<CctCalculationDetailDto> response = controller.updateCalculationDetails(id, dto);

    assertThat("Unexpected response code.", response.getStatusCode(), is(NOT_FOUND));

    CctCalculationDetailDto responseBody = response.getBody();
    assertThat("Unexpected response body.", responseBody, nullValue());
  }

  @Test
  void shouldNotCatchValidationErrorWhenUpdateCalculationValidationFails()
      throws MethodArgumentNotValidException {
    UUID id = UUID.randomUUID();
    Instant created = Instant.now();
    CctCalculationDetailDto dto = CctCalculationDetailDto.builder()
        .id(id)
        .name("Test Calculation")
        .created(created)
        .lastModified(created)
        .build();

    when(service.updateCalculation(any(), any())).thenThrow(MethodArgumentNotValidException.class);

    assertThrows(MethodArgumentNotValidException.class,
        () -> controller.updateCalculationDetails(id, dto));
  }

  @Test
  void shouldDeleteCalculationWhenCalculationFound() {
    UUID id = UUID.randomUUID();
    when(service.deleteCalculation(id))
        .thenReturn(true);

    ResponseEntity<Boolean> result = controller.deleteCalculation(id);

    assertThat("Unexpected result.", result.getBody(), is(true));
  }


  @Test
  void shouldThrowBadRequestWhenDeletePlacementIllegalArgumentException() {
    UUID id = UUID.randomUUID();
    when(service
        .deleteCalculation(id))
        .thenThrow(new IllegalArgumentException());

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> controller.deleteCalculation(id));
    assertThat("Unexpected status code.", exception.getStatusCode(), is(BAD_REQUEST));
  }

  @Test
  void shouldThrowBadRequestWhenDeletePlacementInvalidDataAccessApiUsageException() {
    UUID id = UUID.randomUUID();
    when(service
        .deleteCalculation(id))
        .thenThrow(new InvalidDataAccessApiUsageException("error"));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> controller.deleteCalculation(id));
    assertThat("Unexpected status code.", exception.getStatusCode(), is(BAD_REQUEST));
  }

  @Test
  void shouldThrowNotFoundWhenCannotDeletePlacement() {
    UUID id = UUID.randomUUID();
    when(service
        .deleteCalculation(id))
        .thenReturn(false);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> controller.deleteCalculation(id));
    assertThat("Unexpected status code.", exception.getStatusCode(), is(NOT_FOUND));
  }
}
