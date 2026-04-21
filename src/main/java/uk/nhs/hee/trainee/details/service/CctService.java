/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
import uk.nhs.hee.trainee.details.dto.enumeration.CctChangeType;
import uk.nhs.hee.trainee.details.exception.NotRecordOwnerException;
import uk.nhs.hee.trainee.details.mapper.CctMapper;
import uk.nhs.hee.trainee.details.model.CctCalculation;
import uk.nhs.hee.trainee.details.repository.CctCalculationRepository;

/**
 * A service for CCT related functionality.
 */
@Slf4j
@Service
@XRayEnabled
public class CctService {

  protected static final double WTE_EPSILON = 0.01; //minimum WTE value

  private final TraineeIdentity traineeIdentity;

  private final CctCalculationRepository calculationRepository;
  private final CctMapper mapper;

  private static final Method UPDATE_CALCULATION_METHOD;
  private static final MethodParameter UPDATE_CALCULATION_METHOD_PARAMETER;

  static {
    try {
      UPDATE_CALCULATION_METHOD = CctService.class.getDeclaredMethod(
          "updateCalculation", UUID.class, CctCalculationDetailDto.class);
      UPDATE_CALCULATION_METHOD_PARAMETER = new MethodParameter(
          UPDATE_CALCULATION_METHOD, 1); // 1 = calculation parameter
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Unabled to reflect updateCalculation method.", e);
    }
  }

  /**
   * Create a service for CCT functionality.
   *
   * @param traineeIdentity       The identity of the current user.
   * @param calculationRepository The repository for CCT calculations.
   * @param mapper                A mapper to convert between CCT data types.
   */
  public CctService(TraineeIdentity traineeIdentity, CctCalculationRepository calculationRepository,
      CctMapper mapper) {
    this.traineeIdentity = traineeIdentity;
    this.calculationRepository = calculationRepository;
    this.mapper = mapper;
  }

  /**
   * Get a list of CCT calculations for the current user.
   *
   * @return Details of the found CCT calculations, or empty when none found.
   */
  public List<CctCalculationDetailDto> getCalculations() {
    String traineeId = traineeIdentity.getTraineeId();
    log.info("Getting CCT calculations for trainee [{}]", traineeId);

    List<CctCalculation> entities = calculationRepository.findByTraineeIdOrderByLastModified(
        traineeId);
    log.info("Found {} CCT calculations for trainee [{}]", entities.size(), traineeId);

    List<CctCalculationDetailDto> entityDtos = new ArrayList<>();
    entities.forEach(entity ->
        entityDtos.add(mapper.toDetailDto(entity, calculateCctDate(entity))));
    return entityDtos;
  }

  /**
   * Get a CCT calculation with the given ID, it must belong to the currently authenticated user.
   *
   * @param id The ID of the CCT calculation to retrieve.
   * @return The found CCT calculation, or empty when not found.
   * @throws NotRecordOwnerException If the found CCT calculation does not belong to the current
   *                                 user.
   */
  public Optional<CctCalculationDetailDto> getCalculation(UUID id) {
    log.info("Getting CCT calculation [{}]", id);
    String traineeId = traineeIdentity.getTraineeId();
    Optional<CctCalculation> entity = calculationRepository.findById(id);

    if (entity.isPresent() && !entity.get().traineeId().equals(traineeId)) {
      String message = "CCT Calculation [%s] does not belong to authenticated user [%s]."
          .formatted(id, traineeId);
      throw new NotRecordOwnerException(message);
    }

    log.info("CCT calculation found: [{}]", entity.isPresent());
    return entity.map(cctCalculation
            -> mapper.toDetailDto(cctCalculation, calculateCctDate(cctCalculation)))
        .or(() -> entity.map(mapper::toDetailDto));
  }

  /**
   * Create a new CCT calculation.
   *
   * @param dto The detail of the CCT calculation.
   * @return The created CCT calculation.
   */
  public CctCalculationDetailDto createCalculation(CctCalculationDetailDto dto) {
    log.info("Creating CCT calculation [{}]", dto.name());
    CctCalculation entity = mapper.toEntity(dto, traineeIdentity.getTraineeId());
    entity = calculationRepository.insert(entity);

    log.info("Created CCT calculation [{}] with id [{}]", dto.name(), entity.id());
    return mapper.toDetailDto(entity, calculateCctDate(entity));
  }

  /**
   * Update a CCT calculation.
   *
   * @param id                  The ID of the CCT calculation to update
   * @param dto                 The detail of the CCT calculation.
   *
   * @return The updated CCT calculation, or optional empty if error.
   */
  public Optional<CctCalculationDetailDto> updateCalculation(UUID id, CctCalculationDetailDto dto)
      throws MethodArgumentNotValidException {
    log.info("Updating CCT calculation [{}] with id [{}]", dto.name(), id);

    Optional<CctCalculationDetailDto> existingCalc = getCalculation(id);
    if (existingCalc.isPresent()) {
      BeanPropertyBindingResult validationResult =
          validateChangeIds(dto.changes(), existingCalc.get().changes());
      if (validationResult.hasErrors()) {
        log.warn("CCT calculation [{}] cannot be updated: invalid changes.", id);
        throw new MethodArgumentNotValidException(UPDATE_CALCULATION_METHOD_PARAMETER,
            validationResult);
      }

      CctCalculation entity = mapper.toEntity(dto, traineeIdentity.getTraineeId());
      entity = calculationRepository.save(entity);
      log.info("Updated CCT calculation [{}] with id [{}]", dto.name(), entity.id());
      return Optional.of(mapper.toDetailDto(entity, calculateCctDate(entity)));
    } else {
      log.warn("CCT calculation [{}] cannot be updated: not found.", id);
    }
    return Optional.empty();
  }

  /**
   * Delete a CCT calculation.
   *
   * @param id  The ID of the CCT calculation to delete
   * @return True if deleted, otherwise false.
   */
  public boolean deleteCalculation(UUID id) {
    log.info("Deleting CCT calculation with id [{}]", id);

    Optional<CctCalculationDetailDto> existingCalc = getCalculation(id);
    if (existingCalc.isPresent()) {
      calculationRepository.deleteById(id);
      log.info("Deleted CCT calculation with id [{}]", id);
      return true;
    } else {
      log.warn("CCT calculation cannot be deleted: "
          + "id {} not found or does not belong to current user.", id);
    }
    return false;
  }

  /**
   * Validation the {@link CctChangeDto} IDs.
   *
   * @param newChanges      The new changes being saved.
   * @param existingChanges The existing changes from the database.
   * @return True if valid, else false.
   */
  private BeanPropertyBindingResult validateChangeIds(List<CctChangeDto> newChanges,
      List<CctChangeDto> existingChanges) {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(newChanges, "newChanges");

    Set<UUID> existingChangeIds = Set.of();
    if (existingChanges != null) {
      existingChangeIds = existingChanges.stream()
          .map(CctChangeDto::id)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    }

    Set<UUID> uniqueNewChangeIds = new HashSet<>();

    for (var iterator = newChanges.listIterator(); iterator.hasNext(); ) {
      CctChangeDto newChange = iterator.next();
      UUID newChangeId = newChange.id();

      // Add an error if the caller has generated change IDs themselves.
      if (newChangeId != null && !existingChangeIds.contains(newChangeId)) {
        String field = "changes[%d].id".formatted(iterator.previousIndex());
        result.addError(
            new FieldError("CctChangeDto", field, "must be null or match existing value"));
      }

      // Add an error if change IDs are not unique
      if (uniqueNewChangeIds.contains(newChangeId)) {
        String field = "changes[%d].id".formatted(iterator.previousIndex());
        result.addError(new FieldError("CctChangeDto", field, "must be unique"));
      }
      uniqueNewChangeIds.add(newChangeId);
    }

    return result;
  }

  /**
   * Calculate the CCT end date and insert it into a CCT Calculation DTO.
   *
   * @param dto The CctCalculationDetailDto to use to calculate the CCT end date.
   * @return A copy of the DTO with the CCT end date set.
   */
  public Optional<CctCalculationDetailDto> insertCctDate(CctCalculationDetailDto dto) {
    CctCalculation entity = mapper.toEntity(dto, null);
    return Optional.of(mapper.toDetailDto(entity, calculateCctDate(entity)));
  }

  /**
   * Calculate the CCT end date for a CCT Calculation.
   *
   * @param entity The CctCalculation to use to calculate the CCT end date.
   * @return the CCT end date, or null if this is not possible to calculate.
   */
  public LocalDate calculateCctDate(CctCalculation entity) {
    if (entity == null) {
      return null;
    }

    if (entity.programmeMembership() == null || entity.programmeMembership().endDate() == null) {
      log.warn("CCT date cannot be calculated, programme membership end date is missing.");
      return null;
    }

    LocalDate pmEndDate = entity.programmeMembership().endDate();
    long totalExtensionDays = 0;

    List<CctCalculation.CctChange> changes = entity.changes() == null ? List.of()
        : entity.changes();
    List<CctCalculation.CctChange> orderedChanges = changes.stream()
        .sorted(Comparator.comparing(CctCalculation.CctChange::startDate,
            Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    for (CctCalculation.CctChange c : orderedChanges) {
      if (c == null) {
        log.warn("CCT date cannot be calculated, change is null.");
        return null;
      }

      LocalDate startDate = c.startDate();
      if (startDate == null) {
        log.warn("CCT date cannot be calculated, start date for change {} is null.", c);
        return null;
      }

      LocalDate endDate = c.endDate() == null ? pmEndDate : c.endDate();
      if (endDate.isBefore(startDate)) {
        log.warn("CCT date cannot be calculated, end date for change {} before start date {}.",
            c, startDate);
        return null;
      }

      if (c.type() == null) {
        log.warn("CCT date cannot be calculated, type for change {} is null.", c);
        return null;
      }

      Optional<Long> extensionDays = calculateExtensionDays(c.type(), startDate, endDate,
          c.wte());
      if (extensionDays.isEmpty()) {
        log.warn("CCT date cannot be calculated, WTE for change {} is less than minimum.", c);
        return null;
      }
      totalExtensionDays += extensionDays.get();
    }

    return pmEndDate.plusDays(totalExtensionDays);
  }

  /**
   * Calculate the CCT end date for a CCT Calculation DTO.
   *
   * @param dto The CctCalculation DTO to use to calculate the CCT end date.
   * @return the CCT end date, or null if this is not possible to calculate.
   */
  public LocalDate calculateCctDate(CctCalculationDetailDto dto) {
    return calculateCctDate(mapper.toEntity(dto, null));
  }

  Optional<Long> calculateExtensionDays(CctChangeType type, LocalDate startDate, LocalDate endDate,
      Double wte) {
    if (type == null || startDate == null || endDate == null || endDate.isBefore(startDate)) {
      return Optional.empty();
    }

    long changeDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

    if (type != CctChangeType.LTFT) {
      return Optional.of(changeDays);
    }

    if (wte == null || wte < WTE_EPSILON) {
      return Optional.empty();
    }

    double wteDays = changeDays * wte;
    return Optional.of(Math.round(changeDays - wteDays));
  }

  /**
   * Move all CCT calculations from one trainee to another. Assumes that toTraineeId is valid.
   *
   * @param fromTraineeId The trainee ID to move calculations from.
   * @param toTraineeId   The trainee ID to move calculations to.
   */
  public void moveCalculations(String fromTraineeId, String toTraineeId) {
    List<CctCalculation> calculations = calculationRepository
        .findByTraineeIdOrderByLastModified(fromTraineeId);

    AtomicReference<Integer> movedCount = new AtomicReference<>(0);
    calculations.forEach(c -> {
      log.debug("Moving CCT calculation [{}] from trainee [{}] to trainee [{}].",
          c.id(), fromTraineeId, toTraineeId);
      calculationRepository.save(c.withTraineeId(toTraineeId));
      movedCount.getAndSet(movedCount.get() + 1);
    });
    log.info("Moved {} of expected {} CCT calculations from trainee [{}] to trainee [{}].",
        movedCount.get(), calculations.size(), fromTraineeId, toTraineeId);
  }
}
