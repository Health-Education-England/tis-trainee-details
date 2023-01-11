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

import java.time.Duration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import uk.nhs.hee.trainee.details.dto.signature.SignedDto;

/**
 * Configuration properties for DTO signatures.
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "application.signature")
public final class SignatureConfigurationProperties {

  @Getter
  private final String secretKey;
  private final Map<String, Duration> expireAfter;

  /**
   * Create a configuration profiles object for signature configuration values.
   *
   * @param secretKey   The secret key to use when generating a HMAC signature.
   * @param expireAfter A map where the value is a DTO class name and the value is how long, in
   *                    minutes, the signature for that DTO should be valid.
   */
  public SignatureConfigurationProperties(String secretKey, Map<String, Long> expireAfter) {
    this.secretKey = secretKey;
    this.expireAfter = expireAfter.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> Duration.ofMinutes(e.getValue())));
  }

  /**
   * Get the duration after which to expire a signature for the given DTO type.
   *
   * @param dto The type of DTO to get the expiry for.
   * @return The configured expiration for the DTO type, or the default expiry if the DTO is not
   * configured.
   */
  public Duration getExpireAfter(SignedDto dto) {
    return expireAfter.getOrDefault(dto.getClass().getName(), expireAfter.get("default"));
  }
}
