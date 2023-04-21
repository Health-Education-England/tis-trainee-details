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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AuthTokenUtilTest {

  private static final String TIS_ID_ATTRIBUTE = "custom:tisId";

  @Test
  void getTraineeTisIdShouldThrowExceptionWhenTokenPayloadNotMap() {
    String encodedPayload = Base64.getEncoder()
        .encodeToString("[]".getBytes(StandardCharsets.UTF_8));
    String token = String.format("aa.%s.cc", encodedPayload);

    assertThrows(IOException.class, () -> AuthTokenUtil.getTraineeTisId(token));
  }

  @Test
  void getTraineeTisIdShouldReturnNullWhenTisIdNotInToken() throws IOException {
    String encodedPayload = Base64.getEncoder()
        .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
    String token = String.format("aa.%s.cc", encodedPayload);

    String tisId = AuthTokenUtil.getTraineeTisId(token);

    assertThat("Unexpected trainee TIS ID", tisId, nullValue());
  }

  @Test
  void getTraineeTisIdShouldReturnIdWhenTisIdInToken() throws IOException {
    String payload = String.format("{\"%s\":\"%s\"}", TIS_ID_ATTRIBUTE, "40");
    String encodedPayload = Base64.getEncoder()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    String token = String.format("aa.%s.cc", encodedPayload);

    String tisId = AuthTokenUtil.getTraineeTisId(token);

    assertThat("Unexpected trainee TIS ID", tisId, is("40"));
  }

  @Test
  void getTraineeTisIdShouldHandleUrlCharactersInToken() {
    // The payload is specifically crafted to include an underscore, the ID is 12.
    String token = "aGVhZGVy.eyJjdXN0b206dGlzSWQiOiAiMTIiLCJuYW1lIjogIkpvaG4gRG_DqyJ9.c2lnbmF0dXJl";
    String tisId = assertDoesNotThrow(() -> AuthTokenUtil.getTraineeTisId(token));
    assertThat("Unexpected trainee TIS ID.", tisId, is("12"));
  }
}
