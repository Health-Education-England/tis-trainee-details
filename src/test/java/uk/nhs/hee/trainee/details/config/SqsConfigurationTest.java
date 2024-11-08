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

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@RunWith(MockitoJUnitRunner.class)
class SqsConfigurationTest {

  SqsConfiguration sqsConfiguration;

  @BeforeEach
  void setUp() {
    sqsConfiguration = new SqsConfiguration();
  }


  @Test
  void shouldRemovePayloadTypeHeader() {
    SqsTemplate sqsTemplate = sqsConfiguration.sqsTemplate(new SqsAsyncClient() {
      @Override
      public String serviceName() {
        return "";
      }

      @Override
      public void close() {

      }
    });
    //hmmm
  }
}
