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

package uk.nhs.hee.trainee.details.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A utility for getting attribute from authentication tokens.
 */
public class AuthTokenUtil {

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Hide the constructor.
   */
  private AuthTokenUtil() {

  }

  /**
   * Get a set of strings from the provided token.
   *
   * @param token          The token to use.
   * @param arrayAttribute The attribute key of an array in the token.
   * @return A set of strings from the token array.
   * @throws IOException If the token's payload was not a Map.
   */
  public static Set<String> getAttributes(String token, String arrayAttribute) throws IOException {
    List<String> groups = (List<String>) getTokenPayload(token).get(arrayAttribute);
    return groups == null ? null : groups.stream().collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Get a string attribute value from the provided token.
   *
   * @param token     The token to use.
   * @param attribute The attribute key.
   * @return The string value of the attribute.
   * @throws IOException If the token's payload was not a Map.
   */
  public static String getAttribute(String token, String attribute) throws IOException {
    return (String) getTokenPayload(token).get(attribute);
  }

  /**
   * Get the payload from the provided token.
   *
   * @param token The token to use.
   * @return The extracted payload.
   * @throws IOException If the token's payload was not a Map.
   */
  private static Map<?, ?> getTokenPayload(String token) throws IOException {
    String[] tokenSections = token.split("\\.");
    byte[] payloadBytes = Base64.getUrlDecoder()
        .decode(tokenSections[1].getBytes(StandardCharsets.UTF_8));

    return mapper.readValue(payloadBytes, Map.class);
  }
}
