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

import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;

/**
 * Caching annotations do not work within the same class, this delegate provides a lightweight
 * caching interface for services.
 */
@Component
class CachingDelegate {

  private static final String CONDITIONS_OF_JOINING = "ConditionsOfJoining";

  /**
   * Cache a Conditions of Joining for later retrieval.
   *
   * @param key                 The cache key.
   * @param conditionsOfJoining The Conditions of Joining to cache.
   * @return The cached Conditions of Joining.
   */
  @CachePut(cacheNames = CONDITIONS_OF_JOINING, key = "#key")
  public ConditionsOfJoining cacheConditionsOfJoining(String key,
      ConditionsOfJoining conditionsOfJoining) {
    return conditionsOfJoining;
  }

  /**
   * Get the Conditions of Joining associated with the given key, any cached value will be removed.
   *
   * @param key The cache key.
   * @return The cached Conditions of Joining, or an empty optional if not found.
   */
  @Cacheable(cacheNames = CONDITIONS_OF_JOINING)
  @CacheEvict(CONDITIONS_OF_JOINING)
  public Optional<ConditionsOfJoining> getConditionsOfJoining(String key) {
    return Optional.empty();
  }
}
