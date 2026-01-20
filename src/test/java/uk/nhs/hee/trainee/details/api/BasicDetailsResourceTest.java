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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.config.InterceptorConfiguration;
import uk.nhs.hee.trainee.details.dto.EmailUpdateDto;
import uk.nhs.hee.trainee.details.dto.GmcDetailsDto;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapperImpl;
import uk.nhs.hee.trainee.details.mapper.SignatureMapperImpl;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.PersonalDetailsUpdated;
import uk.nhs.hee.trainee.details.service.PersonalDetailsService;
import uk.nhs.hee.trainee.details.service.SignatureService;

// Explicit import seems required when resource not included in Context Configuration.
@Import(BasicDetailsResource.class)
@ContextConfiguration(classes = {PersonalDetailsMapperImpl.class, SignatureMapperImpl.class,
    RestResponseEntityExceptionHandler.class, InterceptorConfiguration.class})
@WebMvcTest(BasicDetailsResource.class)
class BasicDetailsResourceTest {

  private static final String GMC_NUMBER = "1234567";
  private static final String DEFAULT_GMC_STATUS = "Registered with Licence";

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PersonalDetailsService service;

  @MockBean
  private SignatureService signatureService;

  @Test
  void shouldUpdateBasicDetailsWhenTraineeFound() throws Exception {
    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setPublicHealthNumber("phnValue");

    when(service.createProfileOrUpdateBasicDetailsByTisId(eq("40"), any(PersonalDetails.class)))
        .thenReturn(personalDetails);

    mockMvc.perform(patch("/api/basic-details/{tisId}", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(new PersonalDetailsDto())))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.publicHealthNumber").value(is("phnValue")));
  }

  @Test
  void getShouldNotUpdateGmcNumberWhenTokenNotFound() throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber(GMC_NUMBER)
        .build();

    this.mockMvc
        .perform(put("/api/basic-details/gmc-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(gmcDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldNotUpdateGmcNumberWhenPayloadNotMap() throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber(GMC_NUMBER)
        .build();

    String token = TestJwtUtil.generateToken("[]");

    this.mockMvc.perform(put("/api/basic-details/gmc-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(gmcDetails))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldNotUpdateGmcNumberWhenTisIdNotInToken() throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber(GMC_NUMBER)
        .build();

    String token = TestJwtUtil.generateToken("{}");

    this.mockMvc.perform(put("/api/basic-details/gmc-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(gmcDetails))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldNotUpdateGmcNumberWhenTisIdNotExists() throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber(GMC_NUMBER)
        .build();

    String token = TestJwtUtil.generateTokenForTisId("40");

    when(service.updateGmcDetailsByTisId(any(), any())).thenReturn(
        new PersonalDetailsUpdated(false, Optional.empty()));

    this.mockMvc.perform(put("/api/basic-details/gmc-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(gmcDetails))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldUpdateGmcNumberWhenAuthorized() throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber(GMC_NUMBER)
        .build();

    String token = TestJwtUtil.generateTokenForTisId("40");

    ArgumentCaptor<GmcDetailsDto> gmcDetailsCaptor = ArgumentCaptor.captor();
    when(service.updateGmcDetailsWithTraineeProvidedDetails(eq("40"), gmcDetailsCaptor.capture()))
        .thenAnswer(inv -> {
          GmcDetailsDto gmcDetailsArg = inv.getArgument(1);
          PersonalDetails personalDetails = new PersonalDetails();
          personalDetails.setGmcNumber(gmcDetailsArg.gmcNumber());
          personalDetails.setGmcStatus(gmcDetailsArg.gmcStatus());
          return Optional.of(personalDetails);
        });

    this.mockMvc.perform(put("/api/basic-details/gmc-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(gmcDetails))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gmcNumber", is("1234567")));

    GmcDetailsDto updatedDetails = gmcDetailsCaptor.getValue();
    assertThat("Unexpected GMC number.", updatedDetails.gmcNumber(), is("1234567"));
  }

  @Test
  void shouldSetGmcNumberProvidedByTraineeWhenAuthorized() throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber(GMC_NUMBER)
        .build();

    String token = TestJwtUtil.generateTokenForTisId("40");

    this.mockMvc.perform(put("/api/basic-details/gmc-number")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(gmcDetails))
        .header(HttpHeaders.AUTHORIZATION, token));

    verify(service).updateGmcDetailsWithTraineeProvidedDetails(any(), any());
  }

  @Test
  void shouldNotUpdateGmcNumberWhenGmcNumberNull() throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .build();

    String token = TestJwtUtil.generateTokenForTisId("40");

    this.mockMvc.perform(put("/api/basic-details/gmc-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(gmcDetails))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.type", is("about:blank")))
        .andExpect(jsonPath("$.title", is("Validation failure")))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.instance", is("/api/basic-details/gmc-number")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/gmcNumber")))
        .andExpect(jsonPath("$.errors[0].detail", is("must not be null")));
  }

  @ParameterizedTest
  @EmptySource
  @ValueSource(strings = {"123456", "12345678", "abcdefg"})
  void shouldNotUpdateGmcNumberWhenGmcNumberNotValid(String gmcNumber) throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber(gmcNumber)
        .build();

    String token = TestJwtUtil.generateTokenForTisId("40");

    this.mockMvc.perform(put("/api/basic-details/gmc-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(gmcDetails))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.type", is("about:blank")))
        .andExpect(jsonPath("$.title", is("Validation failure")))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.instance", is("/api/basic-details/gmc-number")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/gmcNumber")))
        .andExpect(jsonPath("$.errors[0].detail", is("must be 7 digits")));
  }

  @Test
  void shouldNotUpdateGmcNumberWhenGmcStatusNotNull() throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber(GMC_NUMBER)
        .gmcStatus("notNull")
        .build();

    String token = TestJwtUtil.generateTokenForTisId("40");

    this.mockMvc.perform(put("/api/basic-details/gmc-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(gmcDetails))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.type", is("about:blank")))
        .andExpect(jsonPath("$.title", is("Validation failure")))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.instance", is("/api/basic-details/gmc-number")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/gmcStatus")))
        .andExpect(jsonPath("$.errors[0].detail", is("must be null")));
  }

  @Test
  void shouldNotUpdateGmcNumberWhenGmcNumberNullAndGmcStatusNotValid()
      throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcStatus("notNull")
        .build();

    String token = TestJwtUtil.generateTokenForTisId("40");

    this.mockMvc.perform(put("/api/basic-details/gmc-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(gmcDetails))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.type", is("about:blank")))
        .andExpect(jsonPath("$.title", is("Validation failure")))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.instance", is("/api/basic-details/gmc-number")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors", hasSize(2)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/gmcNumber")))
        .andExpect(jsonPath("$.errors[0].detail", is("must not be null")))
        .andExpect(jsonPath("$.errors[1].pointer", is("#/gmcStatus")))
        .andExpect(jsonPath("$.errors[1].detail", is("must be null")));
  }

  @ParameterizedTest
  @EmptySource
  @ValueSource(strings = {"123456", "12345678", "abcdefg"})
  void shouldNotUpdateGmcNumberWhenGmcNumberNotValidAndGmcStatusNotValid(String gmcNumber)
      throws Exception {
    GmcDetailsDto gmcDetails = GmcDetailsDto.builder()
        .gmcNumber(gmcNumber)
        .gmcStatus("notNull")
        .build();

    String token = TestJwtUtil.generateTokenForTisId("40");

    this.mockMvc.perform(put("/api/basic-details/gmc-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(gmcDetails))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.type", is("about:blank")))
        .andExpect(jsonPath("$.title", is("Validation failure")))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.instance", is("/api/basic-details/gmc-number")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors", hasSize(2)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/gmcNumber")))
        .andExpect(jsonPath("$.errors[0].detail", is("must be 7 digits")))
        .andExpect(jsonPath("$.errors[1].pointer", is("#/gmcStatus")))
        .andExpect(jsonPath("$.errors[1].detail", is("must be null")));
  }

  @Test
  void shouldNotUpdateEmailWhenTraineeIdNotInToken() throws Exception {
    EmailUpdateDto emailUpdateDto = new EmailUpdateDto();
    emailUpdateDto.setEmail("test@example.com");

    // Simulate no trainee ID in token
    String token = TestJwtUtil.generateToken("{}");

    this.mockMvc.perform(put("/api/basic-details/email-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(emailUpdateDto))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldNotUpdateEmailWhenEmailNotUnique() throws Exception {
    EmailUpdateDto emailUpdateDto = new EmailUpdateDto();
    emailUpdateDto.setEmail("duplicate@example.com");

    String token = TestJwtUtil.generateTokenForTisId("40");

    when(service.isEmailUnique("40", "duplicate@example.com")).thenReturn(false);

    this.mockMvc.perform(put("/api/basic-details/email-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(emailUpdateDto))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldUpdateEmailWhenAuthorizedAndUnique() throws Exception {
    EmailUpdateDto emailUpdateDto = new EmailUpdateDto();
    emailUpdateDto.setEmail("unique@example.com");

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setEmail("unique@example.com");
    when(service.updateEmailWithTraineeProvidedDetails(eq("40"), any(EmailUpdateDto.class)))
        .thenReturn(Optional.of(personalDetails));

    String token = TestJwtUtil.generateTokenForTisId("40");

    when(service.isEmailUnique("40", "unique@example.com")).thenReturn(true);

    this.mockMvc.perform(put("/api/basic-details/email-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(emailUpdateDto))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email", is("unique@example.com")));
  }

  @Test
  void shouldReturnNotFoundWhenTraineeNotFoundForEmailUpdate() throws Exception {
    EmailUpdateDto emailUpdateDto = new EmailUpdateDto();
    emailUpdateDto.setEmail("notfound@example.com");

    String token = TestJwtUtil.generateTokenForTisId("40");

    when(service.isEmailUnique("40", "notfound@example.com")).thenReturn(true);
    when(service.updateEmailWithTraineeProvidedDetails(eq("40"), any(EmailUpdateDto.class)))
        .thenReturn(Optional.empty());

    this.mockMvc.perform(put("/api/basic-details/email-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(emailUpdateDto))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid-email", "user@@com.com", "user.com", "user@.com"})
  void shouldNotUpdateEmailWhenEmailInvalid(String email) throws Exception {
    EmailUpdateDto emailUpdateDto = new EmailUpdateDto();
    emailUpdateDto.setEmail(email);

    String token = TestJwtUtil.generateTokenForTisId("40");

    this.mockMvc.perform(put("/api/basic-details/email-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(emailUpdateDto))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.type", is("about:blank")))
        .andExpect(jsonPath("$.title", is("Validation failure")))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.instance", is("/api/basic-details/email-address")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/email")))
        .andExpect(jsonPath("$.errors[0].detail", is("Email address must be valid.")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldNotUpdateEmailWhenEmailBlank(String email) throws Exception {
    EmailUpdateDto emailUpdateDto = new EmailUpdateDto();
    emailUpdateDto.setEmail(email);

    String token = TestJwtUtil.generateTokenForTisId("40");

    this.mockMvc.perform(put("/api/basic-details/email-address")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(emailUpdateDto))
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(jsonPath("$.type", is("about:blank")))
        .andExpect(jsonPath("$.title", is("Validation failure")))
        .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
        .andExpect(jsonPath("$.instance", is("/api/basic-details/email-address")))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer", is("#/email")))
        .andExpect(jsonPath("$.errors[0].detail", is("Email address must not be blank.")));
  }
}
