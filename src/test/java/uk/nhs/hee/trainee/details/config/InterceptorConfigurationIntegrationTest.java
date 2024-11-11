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

package uk.nhs.hee.trainee.details.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;

@SpringBootTest
@ContextConfiguration(classes = InterceptorConfiguration.class)
@WebAppConfiguration
class InterceptorConfigurationIntegrationTest {

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private MockHttpServletRequest request;

  @Autowired
  private TraineeIdentity traineeIdentity;

  @Test
  void shouldHaveRequestScopeOnTraineeIdentity() {
    assertThat("Unexpected trainee ID.", traineeIdentity.getTraineeId(), nullValue());

    TraineeIdentity requestIdentity = (TraineeIdentity) request.getAttribute(
        "scopedTarget.traineeIdentity");
    assertThat("Unexpected trainee ID.", requestIdentity.getTraineeId(), nullValue());

    TraineeIdentity contextIdentity = context.getBean("traineeIdentity", TraineeIdentity.class);
    assertThat("Unexpected trainee ID.", contextIdentity.getTraineeId(), nullValue());

    traineeIdentity.setTraineeId("40");

    assertThat("Unexpected trainee ID.", requestIdentity.getTraineeId(), is("40"));
    assertThat("Unexpected trainee ID.", contextIdentity.getTraineeId(), is("40"));
  }
}
