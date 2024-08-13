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
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapper;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapperImpl;
import uk.nhs.hee.trainee.details.mapper.SignatureMapperImpl;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.service.PersonalDetailsService;
import uk.nhs.hee.trainee.details.service.SignatureService;

@ContextConfiguration(classes = {PersonalDetailsMapperImpl.class, SignatureMapperImpl.class})
@ExtendWith(SpringExtension.class)
@WebMvcTest(BasicDetailsResource.class)
class BasicDetailsResourceTest {

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private PersonalDetailsMapper personalDetailsMapper;


  private MockMvc mockMvc;

  @MockBean
  private PersonalDetailsService service;

  @MockBean
  private SignatureService signatureService;

  @BeforeEach
  void setUp() {
    BasicDetailsResource resource = new BasicDetailsResource(service, personalDetailsMapper);
    mockMvc = MockMvcBuilders.standaloneSetup(resource)
        .setMessageConverters(jacksonMessageConverter)
        .build();
  }

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
    this.mockMvc
        .perform(
            put("/api/basic-details/gmc-number/1234567").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldNotUpdateGmcNumberWhenPayloadNotMap() throws Exception {
    String token = TestJwtUtil.generateToken("[]");

    this.mockMvc.perform(put("/api/basic-details/gmc-number/1234567")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldNotUpdateGmcNumberWhenTisIdNotInToken() throws Exception {
    String token = TestJwtUtil.generateToken("{}");

    this.mockMvc.perform(put("/api/basic-details/gmc-number/1234567")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound());
  }

  @Test
  void getShouldNotUpdateGmcNumberWhenTisIdNotExists() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId("40");

    when(service.updateGmcDetailsByTisId(any(), any(), anyBoolean())).thenReturn(Optional.empty());

    this.mockMvc.perform(put("/api/basic-details/gmc-number/1234567")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldUpdateGmcNumberWhenAuthorizedWhenTisIdInToken() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId("40");

    ArgumentCaptor<PersonalDetails> personalDetailsCaptor = ArgumentCaptor.captor();
    when(service.updateGmcDetailsByTisId(eq("40"), personalDetailsCaptor.capture(), anyBoolean()))
        .thenAnswer(inv -> Optional.of(inv.getArgument(1)));

    this.mockMvc.perform(put("/api/basic-details/gmc-number/1234567")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gmcNumber", is("1234567")));

    PersonalDetails entity = personalDetailsCaptor.getValue();
    assertThat("Unexpected GMC number.", entity.getGmcNumber(), is("1234567"));
  }

  @Test
  void shouldSetGmcNumberProvidedByTraineeWhenAuthorizedWhenTisIdInToken() throws Exception {
    String token = TestJwtUtil.generateTokenForTisId("40");

    this.mockMvc.perform(put("/api/basic-details/gmc-number/1234567")
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, token));

    verify(service).updateGmcDetailsByTisId(any(), any(), eq(true));
  }
}
