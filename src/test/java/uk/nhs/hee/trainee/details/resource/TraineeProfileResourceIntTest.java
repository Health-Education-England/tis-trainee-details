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

package uk.nhs.hee.trainee.details.resource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import uk.nhs.hee.trainee.details.TisTraineeDetailsApplication;
import uk.nhs.hee.trainee.details.api.TraineeProfileResource;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

@Ignore("Current requires a local DB instance, ignore until in-memory test DB is set up")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TisTraineeDetailsApplication.class)
public class TraineeProfileResourceIntTest {

  private static final String DEFAULT_SURNAME = "defaultSurname";
  private static final String DEFAULT_FORENAMES = "defaultForenames";
  private static final String DEFAULT_KNOWN_AS = "defaultKnownAs";
  private static final String DEFAULT_MAIDEN_NAME = "defaultMaidenName";
  private static final String DEFAULT_TITLE = "defaultTitle";
  private static final String DEFAULT_CONTACT_PHONE_NR_1 = "0123456789";
  private static final String DEFAULT_CONTACT_PHONE_NR_2 = "9876543210";
  private static final String DEFAULT_EMAIL = "defaultEmail@test.com";
  private static final String DEFAULT_ADDRESS1 = "defaultAddress1";
  private static final String DEFAULT_ADDRESS2 = "defaultAddress2";
  private static final String DEFAULT_ADDRESS3 = "defaultAddress3";
  private static final String DEFAULT_ADDRESS4 = "defaultAddress4";
  private static final String DEFAULT_POST_CODE = "defaultPostCode";
  @Autowired
  TraineeProfileResource traineeProfileResource;
  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;
  @Autowired
  private PageableHandlerMethodArgumentResolver pageableArgumentResolver;
  @Autowired
  private TraineeProfileRepository repository;

  private MockMvc mvc;

  private TraineeProfile traineeProfile;

  /**
   * Create a {@link TraineeProfile} entity with default values for all fields.
   * @return The create {@code TraineeProfile}.
   */
  public static TraineeProfile createEntity() {
    return new TraineeProfile().builder()
        .id("101")
        .traineeTisId("2222")
        .build();
  }

  /**
   * Set up mocks before each test.
   */
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.mvc = MockMvcBuilders.standaloneSetup(traineeProfileResource)
        .setCustomArgumentResolvers(pageableArgumentResolver)
        .setMessageConverters(jacksonMessageConverter).build();

    traineeProfile = createEntity();
  }

  @Test
  @Transactional
  public void getTraineeProfileById() throws Exception {
    // Given
    repository.save(traineeProfile);

    // When and Then
    mvc.perform(MockMvcRequestBuilders.get("/api/traineeprofile/{id}", traineeProfile.getId())
        .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(traineeProfile.getId()))
        .andExpect(jsonPath("$.traineeTisId").value(traineeProfile.getTraineeTisId()))
        .andExpect(jsonPath("$.surname").value(DEFAULT_SURNAME))
        .andExpect(jsonPath("$.forenames").value(DEFAULT_FORENAMES))
        .andExpect(jsonPath("$.knownAs").value(DEFAULT_KNOWN_AS))
        .andExpect(jsonPath("$.maidenName").value(DEFAULT_MAIDEN_NAME))
        .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
        .andExpect(jsonPath("$.telephoneNumber").value(DEFAULT_CONTACT_PHONE_NR_1))
        .andExpect(jsonPath("$.mobileNumber").value(DEFAULT_CONTACT_PHONE_NR_2))
        .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
        .andExpect(jsonPath("$.address1").value(DEFAULT_ADDRESS1))
        .andExpect(jsonPath("$.address2").value(DEFAULT_ADDRESS2))
        .andExpect(jsonPath("$.address3").value(DEFAULT_ADDRESS3))
        .andExpect(jsonPath("$.address4").value(DEFAULT_ADDRESS4))
        .andExpect(jsonPath("$.postCode").value(DEFAULT_POST_CODE));
  }
}
