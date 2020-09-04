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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.trainee.details.dto.PersonalDetailsDto;
import uk.nhs.hee.trainee.details.mapper.PersonalDetailsMapper;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.service.PersonalDetailsService;

@ContextConfiguration(classes = {PersonalDetailsMapper.class})
@ExtendWith(SpringExtension.class)
@WebMvcTest(ContactDetailsResource.class)
class PersonalInfoResourceTest {

  public static final String GENDER = "x";
  private static final String URL_TEMPLATE = "/api/personal-info/{traineeId}";
  private static final LocalDate DATE = LocalDate.MIN;
  @Captor
  ArgumentCaptor<PersonalDetails> personalDetailsCaptor;
  @Captor
  private ArgumentCaptor<String> stringCaptor;
  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;
  @Autowired
  private ObjectMapper mapper;
  private MockMvc mockMvc;
  @MockBean
  private PersonalDetailsService service;

  @BeforeEach
  void setUp() {
    PersonalDetailsMapper mapper = Mappers.getMapper(PersonalDetailsMapper.class);
    PersonalInfoResource personalInfoResource = new PersonalInfoResource(service, mapper);
    mockMvc = MockMvcBuilders.standaloneSetup(personalInfoResource)
        .setMessageConverters(jacksonMessageConverter)
        .build();
  }

  @Test
  void shouldThrowExceptionWhenTraineeNotFound() throws Exception {
    when(service.updatePersonalInfoByTisId(eq("40"), personalDetailsCaptor.capture()))
        .thenReturn(Optional.empty());

    PersonalDetailsDto personalDetailsDto = new PersonalDetailsDto();
    personalDetailsDto.setDateOfBirth(DATE);
    personalDetailsDto.setGender(GENDER);

    mockMvc.perform(patch(URL_TEMPLATE, 40)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(personalDetailsDto)))
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Trainee not found."));

    PersonalDetails expectedPersonalDetails = new PersonalDetails();
    expectedPersonalDetails.setDateOfBirth(DATE);
    expectedPersonalDetails.setGender(GENDER);
    assertThat("Unexpected personal details.", personalDetailsCaptor.getValue(),
        is(expectedPersonalDetails));


  }

  @Test
  void shouldUpdatePersonalInfoWhenTraineeFound() throws Exception {
    PersonalDetails personalDetails = new PersonalDetails();
    String forenames = "John";
    personalDetails.setForenames(forenames);
    String surname = "Doe";
    personalDetails.setSurname(surname);

    when(service.updatePersonalInfoByTisId(eq("40"), personalDetailsCaptor.capture()))
        .thenReturn(Optional.of(personalDetails));

    PersonalDetailsDto personalDetailsDto = new PersonalDetailsDto();
    personalDetailsDto.setDateOfBirth(DATE);
    personalDetailsDto.setGender(GENDER);

    mockMvc.perform(patch(URL_TEMPLATE, 40)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(personalDetailsDto)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.forenames").value(is(forenames)))
        .andExpect(jsonPath("$.surname").value(is(surname)));

    PersonalDetails expectedPersonalDetails = new PersonalDetails();
    expectedPersonalDetails.setDateOfBirth(DATE);
    expectedPersonalDetails.setGender(GENDER);
    assertThat("Unexpected personal details.", personalDetailsCaptor.getValue(),
        is(expectedPersonalDetails));
  }
}
