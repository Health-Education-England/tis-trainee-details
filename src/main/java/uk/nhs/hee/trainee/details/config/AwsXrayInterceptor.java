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

import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.spring.aop.AbstractXRayInterceptor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.nhs.hee.trainee.details.config.EcsMetadataConfiguration.EcsMetadata;

@Aspect
@Component
@ConditionalOnExpression("!T(org.springframework.util.StringUtils)"
    + ".isEmpty('${com.amazonaws.xray.emitters.daemon-address}')")
public class AwsXrayInterceptor extends AbstractXRayInterceptor {

  private final EcsMetadata ecsMetadata;
  private final ObjectMapper mapper;

  AwsXrayInterceptor(Optional<EcsMetadata> ecsMetadata, ObjectMapper mapper) {
    this.ecsMetadata = ecsMetadata.orElse(null);
    this.mapper = mapper;
  }

  @Override
  protected Map<String, Map<String, Object>> generateMetadata(ProceedingJoinPoint pjp,
      Subsegment subsegment) {
    Map<String, Map<String, Object>> metadata = super.generateMetadata(pjp, subsegment);

    Map<String, Object> taskMetadataMap = mapper.convertValue(ecsMetadata, new TypeReference<>() {
    });
    metadata.put("EcsMetadata", taskMetadataMap);

    return metadata;
  }

  @Override
  @Pointcut(
      "@within(com.amazonaws.xray.spring.aop.XRayEnabled) && (bean(*Resource) || bean(*Service))")
  public void xrayEnabledClasses() {

  }
}
