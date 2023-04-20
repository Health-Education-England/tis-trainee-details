package uk.nhs.hee.trainee.details.api.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
}
