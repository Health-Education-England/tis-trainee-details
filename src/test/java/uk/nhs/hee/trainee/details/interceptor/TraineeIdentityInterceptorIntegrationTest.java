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

package uk.nhs.hee.trainee.details.interceptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import org.junit.jupiter.api.Test;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.config.InterceptorConfiguration;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
import uk.nhs.hee.trainee.details.interceptor.TraineeIdentityInterceptorIntegrationTest.InterceptorTestController;

@WebMvcTest(InterceptorTestController.class)
@Import(InterceptorConfiguration.class)
class TraineeIdentityInterceptorIntegrationTest {

  private static final String API_PATH = "/test/interceptor";
  private static final String ID_1 = "40";
  private static final String ID_2 = "41";

  private static final String BEAN_NAME = "traineeIdentity";
  private static final String TARGET_BEAN_NAME = ScopedProxyUtils.getTargetBeanName(BEAN_NAME);
  private static final String TRAINEE_ID = "traineeId";

  @Autowired
  private MockMvc mockMvc;

  @SpyBean
  private TraineeIdentityInterceptor interceptor;

  @Test
  void shouldAddTraineeIdToRequest() throws Exception {
    mockMvc.perform(get(API_PATH)
            .header(HttpHeaders.AUTHORIZATION, TestJwtUtil.generateTokenForTisId(ID_1)))
        .andExpect(request().attribute(BEAN_NAME, nullValue()))
        .andExpect(request().attribute(TARGET_BEAN_NAME, hasProperty(TRAINEE_ID, is(ID_1))));

    verify(interceptor).preHandle(any(), any(), any());
  }

  @Test
  void shouldAddNewTraineeIdOnEachRequest() throws Exception {
    mockMvc.perform(get(API_PATH)
            .header(HttpHeaders.AUTHORIZATION, TestJwtUtil.generateTokenForTisId(ID_1)))
        .andExpect(request().attribute(BEAN_NAME, nullValue()))
        .andExpect(request().attribute(TARGET_BEAN_NAME, hasProperty(TRAINEE_ID, is(ID_1))));

    mockMvc.perform(get(API_PATH)
            .header(HttpHeaders.AUTHORIZATION, TestJwtUtil.generateTokenForTisId(ID_2)))
        .andExpect(request().attribute(BEAN_NAME, nullValue()))
        .andExpect(request().attribute(TARGET_BEAN_NAME, hasProperty(TRAINEE_ID, is(ID_2))));

    verify(interceptor, times(2)).preHandle(any(), any(), any());
  }

  @Test
  void shouldMakeTraineeIdentityAvailableToControllers() throws Exception {
    mockMvc.perform(get(API_PATH)
            .header(HttpHeaders.AUTHORIZATION, TestJwtUtil.generateTokenForTisId(ID_1)))
        .andExpect(content().string(ID_1));

    mockMvc.perform(get(API_PATH)
            .header(HttpHeaders.AUTHORIZATION, TestJwtUtil.generateTokenForTisId(ID_2)))
        .andExpect(content().string(ID_2));
  }

  @SpringBootApplication
  @RestController
  public static class InterceptorTestController {

    private final TraineeIdentity traineeIdentity;

    public InterceptorTestController(TraineeIdentity traineeIdentity) {
      this.traineeIdentity = traineeIdentity;
    }

    @GetMapping(API_PATH)
    public String testInterceptor() {
      assertThat("Unexpected trainee identity.", traineeIdentity, notNullValue());
      return traineeIdentity.getTraineeId();
    }
  }
}
