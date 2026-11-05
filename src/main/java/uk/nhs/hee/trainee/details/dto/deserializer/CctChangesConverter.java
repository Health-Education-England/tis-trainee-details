/*
 * The MIT License (MIT)
 *
 * Copyright 2026 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.dto.deserializer;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.List;
import uk.nhs.hee.trainee.details.dto.CctCalculationInput;
import uk.nhs.hee.trainee.details.dto.CctChangeDto;

/**
 * A converter that keeps one change past the limit, so an oversized list fails its size check
 * without every change being validated.
 */
public class CctChangesConverter extends StdConverter<List<CctChangeDto>, List<CctChangeDto>> {

  @Override
  public List<CctChangeDto> convert(List<CctChangeDto> changes) {
    return changes.size() > CctCalculationInput.MAX_CHANGES
        ? changes.subList(0, CctCalculationInput.MAX_CHANGES + 1)
        : changes;
  }
}
