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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.trainee.details.dto.TraineeProfileDto;
import uk.nhs.hee.trainee.details.mapper.TraineeProfileMapper;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = TraineeProfileResource.class)
public class TraineeProfileResourceTest {

  private static final String DEFAULT_ID_1 = "5e00c7942749a84794644f83";
  private static final String DEFAULT_TIS_ID_1 = "123";

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  private MockMvc mockMvc;

  @MockBean
  private TraineeProfileService traineeProfileServiceMock;

  @MockBean
  private TraineeProfileMapper traineeProfileMapperMock;

  private TraineeProfile traineeProfile;
  private TraineeProfileDto traineeProfileDto;

  @BeforeEach
  public void setup() {
    TraineeProfileResource traineeProfileResource = new TraineeProfileResource(
      traineeProfileServiceMock, traineeProfileMapperMock);
      this.mockMvc = MockMvcBuilders.standaloneSetup(traineeProfileResource)
          .setMessageConverters(jacksonMessageConverter)
          .build();

    traineeProfile = new TraineeProfile();
    traineeProfile.setId(DEFAULT_ID_1);
    traineeProfile.setTraineeTisId(DEFAULT_TIS_ID_1);

    traineeProfileDto = new TraineeProfileDto();
    traineeProfileDto.setId(DEFAULT_ID_1);
    traineeProfileDto.setTraineeTisId(DEFAULT_TIS_ID_1);
  }

//  public void initData() {
//    traineeProfile = new TraineeProfile();
//    traineeProfile.setId(DEFAULT_ID_1);
//    traineeProfile.setTraineeTisId(DEFAULT_TIS_ID_1);
//
//    traineeProfileDto = new TraineeProfileDto();
//    traineeProfileDto.setId(DEFAULT_ID_1);
//    traineeProfileDto.setTraineeTisId(DEFAULT_TIS_ID_1);
//  }

  @Test
  public void testGetTraineeProfileById() throws Exception {
    when(traineeProfileServiceMock.getTraineeProfileByTraineeTisId(DEFAULT_TIS_ID_1)).thenReturn(traineeProfile);
    when(traineeProfileServiceMock.hidePastProgrammes(traineeProfile)).thenReturn(traineeProfile);
    when(traineeProfileServiceMock.hidePastPlacements(traineeProfile)).thenReturn(traineeProfile);
    when(traineeProfileMapperMock.toDto(traineeProfile)).thenReturn(traineeProfileDto);
    this.mockMvc.perform(MockMvcRequestBuilders.get("api/trainee-profile/trainee/123")
        .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }
}
