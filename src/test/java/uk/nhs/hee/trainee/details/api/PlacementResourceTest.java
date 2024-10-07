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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.nhs.hee.trainee.details.dto.PlacementDto;
import uk.nhs.hee.trainee.details.dto.enumeration.Status;
import uk.nhs.hee.trainee.details.dto.signature.Signature;
import uk.nhs.hee.trainee.details.dto.signature.SignedDto;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.mapper.PlacementMapperImpl;
import uk.nhs.hee.trainee.details.mapper.SignatureMapperImpl;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.Site;
import uk.nhs.hee.trainee.details.model.Specialty;
import uk.nhs.hee.trainee.details.service.PlacementService;
import uk.nhs.hee.trainee.details.service.SignatureService;

@ContextConfiguration(classes = {PlacementMapperImpl.class, SignatureMapperImpl.class})
@ExtendWith(SpringExtension.class)
@WebMvcTest(PlacementResource.class)
class PlacementResourceTest {

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private PlacementMapper placementMapper;

  private MockMvc mockMvc;

  @MockBean
  private PlacementService service;

  @MockBean
  private SignatureService signatureService;

  @BeforeEach
  void setUp() {
    PlacementResource resource = new PlacementResource(service, placementMapper);
    mockMvc = MockMvcBuilders.standaloneSetup(resource)
        .setMessageConverters(jacksonMessageConverter)
        .build();
  }

  @Test
  void shouldReturnBadRequestWhenIdIsNull() throws Exception {
    when(service.updatePlacementForTrainee("40", new Placement())).thenReturn(Optional.empty());

    mockMvc.perform(patch("/api/placement/{traineeTisId}", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(new PlacementDto())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnNotFoundStatusWhenTraineeNotFound() throws Exception {
    when(service.updatePlacementForTrainee("40", new Placement())).thenReturn(Optional.empty());

    PlacementDto dto = new PlacementDto();
    dto.setTisId("tisIdValue");

    mockMvc.perform(patch("/api/placement/{traineeTisId}", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(dto)))
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Trainee not found."));
  }

  @Test
  void shouldUpdatePlacementWhenTraineeFound() throws Exception {
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusYears(1);

    Placement placement = new Placement();
    placement.setTisId("tisIdValue");
    placement.setStartDate(start);
    placement.setEndDate(end);
    placement.setGrade("gradeValue");
    placement.setSpecialty("specialtyValue");
    placement.setSubSpecialty("subSpecialtyValue");
    placement.setPostAllowsSubspecialty(true);
    placement.setPlacementType("placementTypeValue");
    placement.setStatus(Status.CURRENT);

    Specialty specialty = new Specialty();
    specialty.setName("otherSpecialtyValue");
    placement.setOtherSpecialties(Set.of(specialty));

    Site site = new Site();
    site.setName("siteValue");
    site.setKnownAs("siteKnownAsValue");
    site.setLocation("siteLocationValue");
    placement.setSite(site);

    when(service.updatePlacementForTrainee(eq("40"), any(Placement.class)))
        .thenReturn(Optional.of(placement));

    Signature signature = new Signature(Duration.ofMinutes(60));
    signature.setHmac("not-really-a-hmac");
    doAnswer(inv -> {
      SignedDto dto = inv.getArgument(0);
      dto.setSignature(signature);
      return null;
    }).when(signatureService).signDto(any());

    PlacementDto dto = new PlacementDto();
    dto.setTisId("tisIdValue");

    mockMvc.perform(patch("/api/placement/{traineeTisId}", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(dto)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.tisId").value(is("tisIdValue")))
        .andExpect(jsonPath("$.startDate").value(is(start.toString())))
        .andExpect(jsonPath("$.endDate").value(is(end.toString())))
        .andExpect(jsonPath("$.site").value(is("siteValue")))
        .andExpect(jsonPath("$.siteLocation").value(is("siteLocationValue")))
        .andExpect(jsonPath("$.siteKnownAs").value(is("siteKnownAsValue")))
        .andExpect(jsonPath("$.grade").value(is("gradeValue")))
        .andExpect(jsonPath("$.specialty").value(is("specialtyValue")))
        .andExpect(jsonPath("$.subSpecialty").value(is("subSpecialtyValue")))
        .andExpect(jsonPath("$.postAllowsSubspecialty").value(is(true)))
        .andExpect(jsonPath("$.otherSpecialties.length()").value(is(1)))
        .andExpect(jsonPath("$.otherSpecialties.[0].name").value(is("otherSpecialtyValue")))
        .andExpect(jsonPath("$.placementType").value(is("placementTypeValue")))
        .andExpect(jsonPath("$.status").value(is("CURRENT")))
        .andExpect(jsonPath("$.signature.hmac").value(signature.getHmac()))
        .andExpect(jsonPath("$.signature.signedAt").value(signature.getSignedAt().toString()))
        .andExpect(jsonPath("$.signature.validUntil").value(signature.getValidUntil().toString()));
    ;
  }

  @Test
  void shouldDeletePlacementWhenTraineeFound() throws Exception {
    when(service
        .deletePlacementForTrainee("40", "1"))
        .thenReturn(true);

    MvcResult result = mockMvc.perform(
            delete("/api/placement/{traineeTisId}/{placementTisId}", 40, 1)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Boolean resultBoolean = Boolean.parseBoolean(result.getResponse().getContentAsString());
    assertThat("Unexpected result.", resultBoolean, is(true));
  }

  @Test
  void shouldNotDeletePlacementWhenTraineeOrPlacementNotFound() throws Exception {
    when(service
        .deletePlacementForTrainee("40", "1"))
        .thenReturn(false);

    mockMvc.perform(
            delete("/api/placement/{traineeTisId}/{placementTisId}", 40, 1)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldThrowBadRequestWhenDeletePlacementException() throws Exception {
    when(service
        .deletePlacementForTrainee("triggersError", "1"))
        .thenThrow(new IllegalArgumentException());

    mockMvc.perform(
            delete("/api/placement/{traineeTisId}/{placementTisId}", "triggersError", "1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }


  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldReturnPlacementPilot2024WhenTraineeFound(boolean isPilot2024)
      throws Exception {
    when(service
        .isPilot2024("40", "1"))
        .thenReturn(isPilot2024);

    mockMvc.perform(
            get("/api/placement/ispilot2024/{traineeTisId}/{placementId}",
                "40", "1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string(String.valueOf(isPilot2024)))
        .andReturn();
  }

  @Test
  void shouldThrowBadRequestWhenPlacementPilot2024Exception() throws Exception {
    when(service
        .isPilot2024("triggersError", "1"))
        .thenThrow(new IllegalArgumentException());

    mockMvc.perform(
            get("/api/placement/ispilot2024/{traineeTisId}/{placementId}",
                "triggersError", "1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldReturnPlacementPilotRollout2024WhenTraineeFound(boolean isPilotRollout2024)
      throws Exception {
    when(service
        .isPilotRollout2024("40", "1"))
        .thenReturn(isPilotRollout2024);

    mockMvc.perform(
            get("/api/placement/isrollout2024/{traineeTisId}/{placementId}",
                "40", "1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string(String.valueOf(isPilotRollout2024)))
        .andReturn();
  }

  @Test
  void shouldThrowBadRequestWhenPlacementPilotRollout2024Exception() throws Exception {
    when(service
        .isPilot2024("triggersError", "1"))
        .thenThrow(new IllegalArgumentException());

    mockMvc.perform(
            get("/api/placement/isrollout2024/{traineeTisId}/{placementId}",
                "triggersError", "1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }
}
