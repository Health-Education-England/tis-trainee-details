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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthTokenUtilTest {

  private static final String STRING_ATTRIBUTE = "custom:my-string";
  private static final String ARRAY_ATTRIBUTE = "my-array";

  @Test
  void getAttributesShouldHandleUrlCharactersInToken() {
    // The payload is specifically crafted to include an underscore, the group is 123.
    String token = "aGVhZGVy.eyJteS1hcnJheSI6WyIxMjMiXSwibmFtZSI6IkpvaG4gRG_DqyJ9.c2ln";

    Set<String> groups = assertDoesNotThrow(
        () -> AuthTokenUtil.getAttributes(token, ARRAY_ATTRIBUTE));

    assertThat("Unexpected attribute count.", groups, hasSize(1));
    assertThat("Unexpected attributes.", groups, hasItem("123"));
  }

  @Test
  void getAttributesShouldThrowExceptionWhenTokenPayloadNotMap() {
    String encodedPayload = Base64.getEncoder()
        .encodeToString("[]".getBytes(StandardCharsets.UTF_8));
    String token = String.format("aa.%s.cc", encodedPayload);

    assertThrows(IOException.class, () -> AuthTokenUtil.getAttributes(token, ARRAY_ATTRIBUTE));
  }

  @Test
  void getAttributesShouldReturnNullWhenArrayNotInToken() throws IOException {
    String encodedPayload = Base64.getEncoder()
        .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
    String token = String.format("aa.%s.cc", encodedPayload);

    Set<String> attributes = AuthTokenUtil.getAttributes(token, ARRAY_ATTRIBUTE);

    assertThat("Unexpected attribute values.", attributes, nullValue());
  }

  @Test
  void getAttributesShouldReturnEmptyWhenArrayEmptyInToken() throws IOException {
    String encodedPayload = Base64.getEncoder()
        .encodeToString("""
             {
                "my-array": []
             }
            """
            .getBytes(StandardCharsets.UTF_8));
    String token = String.format("aa.%s.cc", encodedPayload);

    Set<String> attributes = AuthTokenUtil.getAttributes(token, ARRAY_ATTRIBUTE);

    assertThat("Unexpected attribute count.", attributes, hasSize(0));
  }

  @Test
  void getAttributesShouldReturnSetWhenArrayInToken() throws IOException {
    String encodedPayload = Base64.getEncoder()
        .encodeToString("""
             {
                "my-array": [
                  "123456",
                  "ABCDEF"
                ]
             }
            """
            .getBytes(StandardCharsets.UTF_8));
    String token = String.format("aa.%s.cc", encodedPayload);

    Set<String> attributes = AuthTokenUtil.getAttributes(token, ARRAY_ATTRIBUTE);

    assertThat("Unexpected attribute count.", attributes, hasSize(2));
    assertThat("Unexpected attributes.", attributes, hasItems("123456", "ABCDEF"));
  }

  @Test
  void getAttributeShouldHandleUrlCharactersInToken() {
    // The payload is specifically crafted to include an underscore, the ID is 123.
    String token = "aGVhZGVy.eyJjdXN0b206bXktc3RyaW5nIjoiMTIzIiwibmFtZSI6IkpvaG4gRG_DqyJ9.c2ln";

    String attribute = assertDoesNotThrow(
        () -> AuthTokenUtil.getAttribute(token, STRING_ATTRIBUTE));

    assertThat("Unexpected attribute.", attribute, is("123"));
  }

  @Test
  void getAttributeShouldThrowExceptionWhenTokenPayloadNotMap() {
    String encodedPayload = Base64.getEncoder()
        .encodeToString("[]".getBytes(StandardCharsets.UTF_8));
    String token = String.format("aa.%s.cc", encodedPayload);

    assertThrows(IOException.class, () -> AuthTokenUtil.getAttribute(token, STRING_ATTRIBUTE));
  }

  @Test
  void getAttributeShouldReturnNullWhenAttributeNotInToken() throws IOException {
    String encodedPayload = Base64.getEncoder()
        .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
    String token = String.format("aa.%s.cc", encodedPayload);

    String attribute = AuthTokenUtil.getAttribute(token, STRING_ATTRIBUTE);

    assertThat("Unexpected attribute.", attribute, nullValue());
  }

  @Test
  void getAttributeShouldReturnIdWhenTisIdInToken() throws IOException {
    String payload = String.format("{\"%s\":\"%s\"}", STRING_ATTRIBUTE, "40");
    String encodedPayload = Base64.getEncoder()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    String token = String.format("aa.%s.cc", encodedPayload);

    String attribute = AuthTokenUtil.getAttribute(token, STRING_ATTRIBUTE);

    assertThat("Unexpected attribute.", attribute, is("40"));
  }
}
