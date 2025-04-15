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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.nhs.hee.trainee.details.api.util.AuthTokenUtil;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;

/**
 * An interceptor for creating a {@link TraineeIdentity} from a request.
 */
@Slf4j
public class TraineeIdentityInterceptor implements HandlerInterceptor {

  private final TraineeIdentity traineeIdentity;

  public TraineeIdentityInterceptor(TraineeIdentity traineeIdentity) {
    this.traineeIdentity = traineeIdentity;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    String authToken = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authToken != null) {
      try {
        String traineeId = AuthTokenUtil.getTraineeTisId(authToken);
        traineeIdentity.setTraineeId(traineeId);
      } catch (IOException e) {
        log.warn("Unable to extract trainee ID from authorization token.", e);
      }
    }

    // Non-CCT endpoints are a mix of authenticated (public) and unauthenticated (internal), limit
    // trainee ID verification to CCT endpoints for now.
    String requestUri = request.getRequestURI();
    boolean identityRequired = requestUri.matches("^/api/cct(/.+)?$")
        || requestUri.equals("/api/features");

    if (identityRequired && traineeIdentity.getTraineeId() == null) {
      response.setStatus(HttpStatus.FORBIDDEN.value());
      return false;
    }

    return true;
  }
}
