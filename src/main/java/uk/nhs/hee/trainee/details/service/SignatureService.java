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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.config.SignatureConfigurationProperties;
import uk.nhs.hee.trainee.details.dto.signature.Signature;
import uk.nhs.hee.trainee.details.dto.signature.SignedDto;

/**
 * A service for handling DTO signature functionality.
 */
@Service
public class SignatureService {

  private final ObjectMapper mapper;
  private final SignatureConfigurationProperties signatureConfigurationProperties;

  /**
   * Create an instance of the signature service for handling DTO signature functionality.
   *
   * @param mapper                           The object mapper to use for converting the DTO.
   * @param signatureConfigurationProperties The configuration properties to use when signing.
   */
  SignatureService(ObjectMapper mapper,
      SignatureConfigurationProperties signatureConfigurationProperties) {
    this.mapper = mapper;
    this.signatureConfigurationProperties = signatureConfigurationProperties;
  }

  /**
   * Signs a {@link SignedDto} by populating its {@link Signature}.
   *
   * @param dto The DTO to sign.
   * @throws JsonProcessingException If the DTO could not be read as JSON.
   */
  public void signDto(SignedDto dto) throws JsonProcessingException {
    Duration expireAfter = signatureConfigurationProperties.getExpireAfter(dto);
    Signature signature = new Signature(expireAfter);
    dto.setSignature(signature);

    String secretKey = signatureConfigurationProperties.getSecretKey();
    byte[] dtoBytes = mapper.writeValueAsBytes(dto);
    String hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey).hmacHex(dtoBytes);
    signature.setHmac(hmac);
  }
}
