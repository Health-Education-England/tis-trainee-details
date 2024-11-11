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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Custom configuration for the SQS messaging.
 */
@Configuration
public class SqsConfiguration {

  /**
   * Build the custom-configured SQS template.
   *
   * @param sqsAsyncClient The SQS client to use.
   * @return The configured SQS template.
   */
  @Bean
  public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
    return SqsTemplate.builder()
        .sqsAsyncClient(sqsAsyncClient)
        .configureDefaultConverter(converter
            -> converter.setPayloadTypeHeaderValueFunction(message -> null))
        .build();
  /*
  This prevents the inclusion of the JavaType header which can cause issues for listeners
  which do not have the appropriate class to deserialize the message.

  e.g. without this custom configuration:
  ...
  "MessageAttributes":
    "JavaType":
      "StringValue": "uk.nhs.hee.trainee.details.event.ProfileCreateEvent",
      "DataType": "String",
    "contentType":
      "StringValue": "application/json",
      "DataType": "String"

  e.g. with custom configuration:
  ...
  "MessageAttributes":
    "contentType":
       "StringValue": "application/json",
       "DataType": "String"

  Note: later versions of spring-cloud-dependencies should simplify this with
  messageConverter.doNotSendPayloadTypeHeader()
  */
  }

}
