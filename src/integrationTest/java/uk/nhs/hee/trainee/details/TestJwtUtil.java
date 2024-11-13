/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A utility for generating test JWT tokens.
 */
public class TestJwtUtil {

  public static final String TIS_ID_ATTRIBUTE = "custom:tisId";

  /**
   * Generate a token with the given payload.
   *
   * @param payload The payload to inject in to the token.
   * @return The generated token.
   */
  public static String generateToken(String payload) {
    String encodedPayload = Base64.getUrlEncoder()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    return String.format("aGVhZGVy.%s.c2lnbmF0dXJl", encodedPayload);
  }

  /**
   * Generate a token with the TIS ID attribute as the payload.
   *
   * @param traineeTisId The TIS ID to inject in to the payload.
   * @return The generated token.
   */
  public static String generateTokenForTisId(String traineeTisId) {
    String payload = String.format("{\"%s\":\"%s\"}", TIS_ID_ATTRIBUTE, traineeTisId);
    return generateToken(payload);
  }
}
