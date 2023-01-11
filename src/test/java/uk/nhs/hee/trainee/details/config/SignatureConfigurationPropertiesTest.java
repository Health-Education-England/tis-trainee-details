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
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.dto.PlacementDto;
import uk.nhs.hee.trainee.details.dto.ProgrammeMembershipDto;
import uk.nhs.hee.trainee.details.dto.signature.Signature;
import uk.nhs.hee.trainee.details.dto.signature.SignedDto;

class SignatureConfigurationPropertiesTest {

  private static final String SECRET_KEY = "not-so-secret-key";

  private SignatureConfigurationProperties configurationProperties;

  @BeforeEach
  void setUp() {
    Map<String, Long> expireAfter = Map.of(
        "default", 10L,
        PlacementDto.class.getName(), 20L
    );

    configurationProperties = new SignatureConfigurationProperties(SECRET_KEY, expireAfter);
  }

  @Test
  void shouldGetSecretKey() {
    String secretKey = configurationProperties.getSecretKey();
    assertThat("Unexpected secret key.", secretKey, is(SECRET_KEY));
  }

  @Test
  void shouldGetDefaultExpiryWhenNoConfigFound() {
    Duration expiry = configurationProperties.getExpireAfter(new ProgrammeMembershipDto());
    assertThat("Unexpected expiry.", expiry, is(Duration.ofMinutes(10)));
  }

  @Test
  void shouldGetClassExpiryWhenConfigFound() {
    Duration expiry = configurationProperties.getExpireAfter(new PlacementDto());
    assertThat("Unexpected expiry.", expiry, is(Duration.ofMinutes(20)));
  }
}
