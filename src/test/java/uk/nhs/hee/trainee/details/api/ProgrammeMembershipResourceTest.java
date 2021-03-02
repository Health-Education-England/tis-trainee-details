/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.trainee.details.dto.ProgrammeMembershipDto;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.service.ProgrammeMembershipService;

@ContextConfiguration(classes = {ProgrammeMembershipMapper.class})
@ExtendWith(SpringExtension.class)
@WebMvcTest(ProgrammeMembershipResource.class)
class ProgrammeMembershipResourceTest {

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  @Autowired
  private ObjectMapper mapper;

  private MockMvc mockMvc;

  @MockBean
  private ProgrammeMembershipService service;

  @BeforeEach
  void setUp() {
    ProgrammeMembershipMapper mapper = Mappers.getMapper(ProgrammeMembershipMapper.class);
    ProgrammeMembershipResource resource = new ProgrammeMembershipResource(service, mapper);
    mockMvc = MockMvcBuilders.standaloneSetup(resource)
        .setMessageConverters(jacksonMessageConverter)
        .build();
  }

  @Test
  void shouldReturnBadRequestWhenIdIsNull() throws Exception {
    when(service.updateProgrammeMembershipForTrainee("40", new ProgrammeMembership()))
        .thenReturn(Optional.empty());

    mockMvc.perform(patch("/api/programme-membership/{traineeTisId}", 40)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(new ProgrammeMembershipDto())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnNotFoundStatusWhenTraineeNotFound() throws Exception {
    when(service.updateProgrammeMembershipForTrainee("40", new ProgrammeMembership()))
        .thenReturn(Optional.empty());

    ProgrammeMembershipDto dto = new ProgrammeMembershipDto();
    dto.setTisId("tisIdValue");

    mockMvc.perform(patch("/api/programme-membership/{traineeTisId}", 40)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(dto)))
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Trainee not found."));
  }

  @Test
  void shouldUpdateProgrammeMembershipWhenTraineeFound() throws Exception {
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusYears(1);
    LocalDate completion = end.plusYears(1);

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("tisIdValue");
    programmeMembership.setProgrammeTisId("programmeTisIdValue");
    programmeMembership.setProgrammeName("programmeNameValue");
    programmeMembership.setProgrammeNumber("programmeNumberValue");
    programmeMembership.setManagingDeanery("managingDeaneryValue");
    programmeMembership.setProgrammeMembershipType("programmeMembershipTypeValue");
    programmeMembership.setStartDate(start);
    programmeMembership.setEndDate(end);
    programmeMembership.setProgrammeCompletionDate(completion);

    when(service.updateProgrammeMembershipForTrainee(eq("40"), any(ProgrammeMembership.class)))
        .thenReturn(Optional.of(programmeMembership));

    ProgrammeMembershipDto dto = new ProgrammeMembershipDto();
    dto.setTisId("tisIdValue");

    mockMvc.perform(patch("/api/programme-membership/{traineeTisId}", 40)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(dto)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.tisId").value(is("tisIdValue")))
        .andExpect(jsonPath("$.programmeTisId").value(is("programmeTisIdValue")))
        .andExpect(jsonPath("$.programmeName").value(is("programmeNameValue")))
        .andExpect(jsonPath("$.programmeNumber").value(is("programmeNumberValue")))
        .andExpect(jsonPath("$.managingDeanery").value(is("managingDeaneryValue")))
        .andExpect(jsonPath("$.programmeMembershipType").value(is("programmeMembershipTypeValue")))
        .andExpect(jsonPath("$.startDate").value(is(start.toString())))
        .andExpect(jsonPath("$.endDate").value(is(end.toString())))
        .andExpect(jsonPath("$.programmeCompletionDate").value(is(completion.toString())));
  }

  @Test
  void shouldDeleteProgrammeMembershipWhenTraineeFound() throws Exception {

    when(service
        .deleteProgrammeMembershipsForTrainee("40"))
        .thenReturn(true);

    ProgrammeMembershipDto dto = new ProgrammeMembershipDto();
    dto.setTisId("tisIdValue");

    MvcResult result = mockMvc.perform(
        patch("/api/programme-membership/{traineeTisId}/delete", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(dto)))
        .andExpect(status().isOk())
        .andReturn();

    Boolean resultBoolean = Boolean.parseBoolean(result.getResponse().getContentAsString());
    assertThat("Unexpected result.", resultBoolean, is(true));
  }

  @Test
  void shouldNotDeleteProgrammeMembershipWhenTraineeNotFound() throws Exception {
    when(service
        .deleteProgrammeMembershipsForTrainee("40"))
        .thenReturn(false);

    ProgrammeMembershipDto dto = new ProgrammeMembershipDto();
    dto.setTisId("tisIdValue");

    mockMvc.perform(
        patch("/api/programme-membership/{traineeTisId}/delete", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(dto)))
        .andExpect(status().isNotFound());
  }
}
