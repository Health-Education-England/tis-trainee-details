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

package uk.nhs.hee.trainee.details.service;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.dto.CctSpecialtyDto;

/**
 * A service providing the specialties that CCT dates can be calculated for.
 */
@Slf4j
@Service
@XRayEnabled
public class CctSpecialtyService {

  private static final String SPECIALTIES_PATH = "/cct/specialties.json";

  private final List<CctSpecialtyDto> specialties;

  /**
   * Create a service providing the specialties that CCT dates can be calculated for.
   *
   * @param mapper The object mapper to read the specialties with.
   * @throws IOException If the specialties could not be read.
   */
  public CctSpecialtyService(ObjectMapper mapper) throws IOException {
    try (InputStream specialtiesJson = CctSpecialtyService.class.getResourceAsStream(
        SPECIALTIES_PATH)) {
      specialties = List.of(mapper.readValue(specialtiesJson, CctSpecialtyDto[].class));
    }
    log.info("Loaded {} CCT specialties.", specialties.size());
  }

  /**
   * Get the specialties that CCT dates can be calculated for.
   *
   * @return The specialties.
   */
  public List<CctSpecialtyDto> getSpecialties() {
    return specialties;
  }
}
