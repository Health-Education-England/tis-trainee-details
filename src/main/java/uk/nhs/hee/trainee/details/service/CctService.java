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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.nhs.hee.trainee.details.dto.CctCalculateResponseDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationSaveRequestDto;
import uk.nhs.hee.trainee.details.dto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
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

  private static final DateTimeFormatter MESSAGE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final TraineeIdentity traineeIdentity;

  private final CctCalculationRepository calculationRepository;
  private final CctMapper mapper;
  private final CctEngine engine;

  private static final Method CALCULATE_METHOD;
  private static final MethodParameter CALCULATE_METHOD_PARAMETER;

  static {
    try {
      CALCULATE_METHOD = CctService.class.getDeclaredMethod(
          "calculate", CctCalculationSaveRequestDto.class);
      CALCULATE_METHOD_PARAMETER = new MethodParameter(CALCULATE_METHOD, 0);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Unable to reflect calculate method.", e);
    }
  }

  /**
   * Create a service for CCT functionality.
   *
   * @param traineeIdentity       The identity of the current user.
   * @param calculationRepository The repository for CCT calculations.
   * @param mapper                A mapper to convert between CCT data types.
   * @param engine                A calculator for CCT dates.
   */
  public CctService(TraineeIdentity traineeIdentity, CctCalculationRepository calculationRepository,
      CctMapper mapper, CctEngine engine) {
    this.traineeIdentity = traineeIdentity;
    this.calculationRepository = calculationRepository;
    this.mapper = mapper;
    this.engine = engine;
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

    return mapper.toDetailDtos(entities);
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
    return entity.map(mapper::toDetailDto);
  }

  /**
   * Create a new CCT calculation, with the CCT date calculated from its programme details and
   * changes.
   *
   * @param dto The detail of the CCT calculation.
   * @return The created CCT calculation.
   * @throws MethodArgumentNotValidException If a change starts before the programme, or starts or
   *                                         ends after the calculated CCT date.
   */
  public CctCalculationDetailDto createCalculation(CctCalculationSaveRequestDto dto)
      throws MethodArgumentNotValidException {
    log.info("Creating CCT calculation [{}]", dto.name());
    CctCalculateResponseDto calculation = calculate(dto);
    CctCalculation entity = mapper.toEntity(dto, calculation, traineeIdentity.getTraineeId());
    entity = calculationRepository.insert(entity);

    log.info("Created CCT calculation [{}] with id [{}]", dto.name(), entity.id());
    return mapper.toDetailDto(entity);
  }

  /**
   * Update a CCT calculation, with the CCT date recalculated from its programme details and
   * changes.
   *
   * @param id  The ID of the CCT calculation to update
   * @param dto The detail of the CCT calculation.
   * @return The updated CCT calculation, or optional empty if not found.
   * @throws MethodArgumentNotValidException If a change starts before the programme, or starts or
   *                                         ends after the calculated CCT date.
   */
  public Optional<CctCalculationDetailDto> updateCalculation(UUID id,
      CctCalculationSaveRequestDto dto) throws MethodArgumentNotValidException {
    log.info("Updating CCT calculation [{}] with id [{}]", dto.name(), id);

    Optional<CctCalculationDetailDto> existingCalc = getCalculation(id);
    if (existingCalc.isPresent()) {
      CctCalculateResponseDto calculation = calculate(dto);
      CctCalculation entity = mapper.toEntity(dto, calculation, traineeIdentity.getTraineeId())
          .withCreated(existingCalc.get().created());
      entity = calculationRepository.save(entity);
      log.info("Updated CCT calculation [{}] with id [{}]", dto.name(), entity.id());
      return Optional.of(mapper.toDetailDto(entity));
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
   * Calculate the CCT date for a CCT calculation being saved, and validate its changes against it.
   *
   * @param dto The detail of the CCT calculation.
   * @return The changes, with the months they credit, and the CCT date calculated from them.
   * @throws MethodArgumentNotValidException If a change starts before the programme, or starts or
   *                                         ends after the calculated CCT date.
   */
  private CctCalculateResponseDto calculate(CctCalculationSaveRequestDto dto)
      throws MethodArgumentNotValidException {
    CctCalculateResponseDto calculation = engine.calculate(dto.programmeDetails(), dto.changes());

    BeanPropertyBindingResult validationResult = validateChangeDates(dto,
        calculation.result().cctDate());
    if (validationResult.hasErrors()) {
      log.warn("CCT calculation [{}] cannot be saved: invalid change dates.", dto.name());
      throw new MethodArgumentNotValidException(CALCULATE_METHOD_PARAMETER, validationResult);
    }

    return calculation;
  }

  /**
   * Validate the {@link CctChangeDto} dates against the programme start and the CCT date.
   *
   * @param dto     The CCT calculation being saved.
   * @param cctDate The CCT date calculated for it.
   * @return The validation result.
   */
  private BeanPropertyBindingResult validateChangeDates(CctCalculationSaveRequestDto dto,
      LocalDate cctDate) {
    BeanPropertyBindingResult result = new BeanPropertyBindingResult(dto, "dto");
    LocalDate programmeStartDate = dto.programmeDetails().startDate();
    String cctDateLimit = "cannot be after the current projected Completion of Training Date (%s)."
        .formatted(cctDate.format(MESSAGE_DATE_FORMAT));

    List<CctChangeDto> changes = dto.changes();
    for (int index = 0; index < changes.size(); index++) {
      CctChangeDto change = changes.get(index);

      if (change.startDate().isBefore(programmeStartDate)) {
        addChangeError(result, index, "startDate",
            "Start date cannot be before programme start (%s)."
                .formatted(programmeStartDate.format(MESSAGE_DATE_FORMAT)));
      } else if (change.startDate().isAfter(cctDate)) {
        addChangeError(result, index, "startDate", "Start date " + cctDateLimit);
      }

      if (change.endDate() != null && change.endDate().isAfter(cctDate)) {
        addChangeError(result, index, "endDate", "End date " + cctDateLimit);
      }
    }

    return result;
  }

  /**
   * Add an error for a {@link CctChangeDto} field to a validation result.
   *
   * @param result  The validation result.
   * @param index   The index of the change.
   * @param field   The name of the field.
   * @param message The error message.
   */
  private static void addChangeError(BeanPropertyBindingResult result, int index, String field,
      String message) {
    String path = "changes[%d].%s".formatted(index, field);
    result.addError(new FieldError("CctChangeDto", path, message));
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
