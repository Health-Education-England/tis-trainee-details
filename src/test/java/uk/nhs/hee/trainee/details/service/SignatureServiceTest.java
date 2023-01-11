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

package uk.nhs.hee.trainee.details.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.trainee.details.config.SignatureConfigurationProperties;
import uk.nhs.hee.trainee.details.dto.PlacementDto;
import uk.nhs.hee.trainee.details.dto.signature.Signature;

class SignatureServiceTest {

  private static final String SECRET_KEY = "not-so-secret-key";

  private SignatureService service;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = spy(new ObjectMapper());
    objectMapper.registerModule(new JavaTimeModule());

    Map<String, Long> expiresAfter = Map.of(PlacementDto.class.getName(), 1440L);
    var properties = new SignatureConfigurationProperties(SECRET_KEY, expiresAfter);

    service = new SignatureService(objectMapper, properties);
  }

  @Test
  void shouldThrowExceptionWhenDtoCanNotBeRead() throws JsonProcessingException {
    when(objectMapper.writeValueAsBytes(any())).thenThrow(JsonProcessingException.class);

    assertThrows(JsonProcessingException.class, () -> service.signDto(new PlacementDto()));
  }

  @Test
  void shouldSignDto() throws JsonProcessingException, InterruptedException {
    PlacementDto dto = new PlacementDto();

    final Instant start = Instant.now().minus(Duration.ofNanos(1));
    service.signDto(dto);
    final Instant end = Instant.now().plus(Duration.ofNanos(1));

    Signature signature = dto.getSignature();
    assertThat("Unexpected signature.", signature, notNullValue());

    Instant signedAt = signature.getSignedAt();
    assertThat("Signed at timestamp is too early.", signedAt.isAfter(start), is(true));
    assertThat("Signed at timestamp is too late.", signedAt.isBefore(end), is(true));

    long expiresAfter = signedAt.until(signature.getValidUntil(), ChronoUnit.MINUTES);
    assertThat("Unexpected valid duration.", expiresAfter, is(1440L));

    String hmac = signature.getHmac();
    assertThat("Unexpected hmac.", hmac, notNullValue());
    assertThat("Unexpected hmac length.", hmac.length(), is(64));
  }

  @Test
  void shouldNotValidateWhenHmacIncluded() throws JsonProcessingException, InterruptedException {
    PlacementDto dto = new PlacementDto();
    dto.setTisId("123");

    service.signDto(dto);

    String serviceHmac = dto.getSignature().getHmac();

    byte[] dtoBytes = objectMapper.writeValueAsBytes(dto);
    String testHmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, SECRET_KEY).hmacHex(dtoBytes);

    assertThat("Unexpected hmac.", serviceHmac, not(is(testHmac)));
  }

  @Test
  void shouldValidateWhenHmacExcluded() throws JsonProcessingException, InterruptedException {
    PlacementDto dto = new PlacementDto();
    dto.setTisId("123");

    service.signDto(dto);

    Signature signature = dto.getSignature();
    String serviceHmac = signature.getHmac();

    signature.setHmac(null);
    byte[] dtoBytes = objectMapper.writeValueAsBytes(dto);
    String testHmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, SECRET_KEY).hmacHex(dtoBytes);

    assertThat("Unexpected hmac.", serviceHmac, is(testHmac));
  }
}
