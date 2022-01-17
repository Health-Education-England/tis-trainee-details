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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.trainee.details.dto.QualificationDto;
import uk.nhs.hee.trainee.details.mapper.QualificationMapper;
import uk.nhs.hee.trainee.details.model.Qualification;
import uk.nhs.hee.trainee.details.service.QualificationService;

@ContextConfiguration(classes = {QualificationMapper.class})
@ExtendWith(SpringExtension.class)
@WebMvcTest(QualificationResource.class)
class QualificationResourceTest {

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  @Autowired
  private ObjectMapper mapper;

  private MockMvc mockMvc;

  @MockBean
  private QualificationService service;

  @BeforeEach
  void setUp() {
    QualificationMapper mapper = Mappers.getMapper(QualificationMapper.class);
    QualificationResource resource = new QualificationResource(service, mapper);
    mockMvc = MockMvcBuilders.standaloneSetup(resource)
        .setMessageConverters(jacksonMessageConverter)
        .build();
  }

  @Test
  void shouldReturnBadRequestWhenIdIsNull() throws Exception {
    when(service.updateQualificationByTisId("40", new Qualification()))
        .thenReturn(Optional.empty());

    mockMvc.perform(patch("/api/qualification/{traineeTisId}", 40)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(new QualificationDto())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnNotFoundStatusWhenTraineeNotFound() throws Exception {
    when(service.updateQualificationByTisId("40", new Qualification()))
        .thenReturn(Optional.empty());

    QualificationDto dto = new QualificationDto();
    dto.setTisId("tisIdValue");

    mockMvc.perform(patch("/api/qualification/{traineeTisId}", 40)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(dto)))
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Trainee not found."));
  }

  @Test
  void shouldUpdateQualificationWhenTraineeFound() throws Exception {
    LocalDate dateAttained = LocalDate.now();

    Qualification qualification = new Qualification();
    qualification.setTisId("tisIdValue");
    qualification.setQualification("qualificationValue");
    qualification.setDateAttained(dateAttained);
    qualification.setMedicalSchool("medicalSchoolValue");

    when(service.updateQualificationByTisId(eq("40"), any(Qualification.class)))
        .thenReturn(Optional.of(qualification));

    QualificationDto dto = new QualificationDto();
    dto.setTisId("tisIdValue");

    mockMvc.perform(patch("/api/qualification/{traineeTisId}", 40)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(dto)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.tisId").value(is("tisIdValue")))
        .andExpect(jsonPath("$.qualification").value(is("qualificationValue")))
        .andExpect(jsonPath("$.dateAttained").value(is(dateAttained.toString())))
        .andExpect(jsonPath("$.medicalSchool").value(is("medicalSchoolValue")));
  }

  @Test
  void shouldReturnNoContentWhenQualificationDeleted() throws Exception {
    when(service.deleteQualification("40", "140")).thenReturn(true);

    mockMvc.perform(delete("/api/qualification/{traineeTisId}/{qualificationId}", 40, 140)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNotFoundWhenQualificationNotFound() throws Exception {
    when(service.deleteQualification("40", "140")).thenReturn(false);

    mockMvc.perform(delete("/api/qualification/{traineeTisId}/{qualificationId}", 40, 140)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }
}
