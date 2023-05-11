/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.xray.entities.Subsegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.config.EcsMetadataConfiguration.EcsMetadata;
import uk.nhs.hee.trainee.details.config.EcsMetadataConfiguration.EcsMetadata.ContainerMetadata;
import uk.nhs.hee.trainee.details.config.EcsMetadataConfiguration.EcsMetadata.ContainerMetadata.LogOptions;
import uk.nhs.hee.trainee.details.config.EcsMetadataConfiguration.EcsMetadata.TaskMetadata;

class AwsXrayInterceptorTest {

  private ObjectMapper objectMapper;
  private ProceedingJoinPoint pjp;
  private Subsegment subsegment;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    subsegment = mock(Subsegment.class);

    pjp = mock(ProceedingJoinPoint.class);
    when(pjp.getTarget()).thenReturn(Object.class);
  }

  @Test
  void shouldGenerateNullEcsMetadataWhenNotAvailable() {
    AwsXrayInterceptor interceptor = new AwsXrayInterceptor(Optional.empty(), objectMapper);

    Map<String, Map<String, Object>> metadata = interceptor.generateMetadata(pjp, subsegment);

    assertThat("Unexpected X-Ray metadata.", metadata, notNullValue());

    Map<String, Object> ecsMetadataMap = metadata.get("EcsMetadata");
    assertThat("Unexpected ECS metadata.", ecsMetadataMap, nullValue());
  }

  @Test
  void shouldGenerateEcsMetadataWhenAvailable() {
    EcsMetadata ecsMetadata = new EcsMetadata(
        new TaskMetadata("cluster", "taskArn", "family", "revision"),
        new ContainerMetadata("containerArn", new LogOptions("logGroup", "region", "logStream")));
    AwsXrayInterceptor interceptor = new AwsXrayInterceptor(Optional.of(ecsMetadata), objectMapper);

    Map<String, Map<String, Object>> metadata = interceptor.generateMetadata(pjp, subsegment);

    assertThat("Unexpected X-Ray metadata.", metadata, notNullValue());

    Map<String, Object> ecsMetadataMap = metadata.get("EcsMetadata");
    assertThat("Unexpected ECS metadata.", ecsMetadataMap, notNullValue());

    Map<String, Object> taskMetadataMap = (Map<String, Object>) ecsMetadataMap.get("TaskMetadata");
    assertThat("Unexpected task metadata.", taskMetadataMap, notNullValue());
    assertThat("Unexpected cluster.", taskMetadataMap.get("Cluster"), is("cluster"));
    assertThat("Unexpected task ARN.", taskMetadataMap.get("TaskARN"), is("taskArn"));
    assertThat("Unexpected family.", taskMetadataMap.get("Family"), is("family"));

    Map<String, Object> containerMetadataMap = (Map<String, Object>) ecsMetadataMap.get(
        "ContainerMetadata");
    assertThat("Unexpected container metadata.", containerMetadataMap, notNullValue());
    assertThat("Unexpected container ARN.", containerMetadataMap.get("ContainerARN"),
        is("containerArn"));

    Map<String, Object> logOptionsMap = (Map<String, Object>) containerMetadataMap.get(
        "LogOptions");
    assertThat("Unexpected log options metadata.", logOptionsMap, notNullValue());
    assertThat("Unexpected log group.", logOptionsMap.get("awslogs-group"), is("logGroup"));
    assertThat("Unexpected log region.", logOptionsMap.get("awslogs-region"), is("region"));
    assertThat("Unexpected log stream.", logOptionsMap.get("awslogs-stream"), is("logStream"));
  }
}
