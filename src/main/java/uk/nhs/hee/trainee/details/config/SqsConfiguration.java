package uk.nhs.hee.trainee.details.config;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Custom configuration for the SQS messaging
 */
@Configuration
public class SqsConfiguration {

  @Bean
  public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
    return SqsTemplate.builder()
        .sqsAsyncClient(sqsAsyncClient)
        .configureDefaultConverter(converter
            -> converter.setPayloadTypeHeaderValueFunction(message -> null))
        .build();
        //this prevents the inclusion of the JavaType header which can cause issues for listeners
        //which do not have the appropriate class to deserialize the message.
        //e.g. without this custom configuration:
        /*
        ...
        "MessageAttributes":
          "JavaType":
            "StringValue": "uk.nhs.hee.trainee.details.event.ProfileCreateEvent",
            "DataType": "String",
          "contentType":
            "StringValue": "application/json",
            "DataType": "String"
        */
        //e.g. with custom configuration:
        /*
        ...
        "MessageAttributes":
          "contentType":
             "StringValue": "application/json",
             "DataType": "String"
        */
  }

}
