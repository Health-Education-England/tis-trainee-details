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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import uk.nhs.hee.trainee.details.dto.BodyValidationError;
import uk.nhs.hee.trainee.details.dto.CctCalculateRequestDto;
import uk.nhs.hee.trainee.details.dto.CctCalculateResponseDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationDetailDto;
import uk.nhs.hee.trainee.details.dto.CctCalculationInput;
import uk.nhs.hee.trainee.details.dto.CctCalculationSaveRequestDto;
import uk.nhs.hee.trainee.details.dto.CctChangeDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto;
import uk.nhs.hee.trainee.details.dto.CctProgrammeDetailsDto.CctTrainingTimeDto;
import uk.nhs.hee.trainee.details.dto.CctSpecialtyDto;
import uk.nhs.hee.trainee.details.dto.TraineeIdentity;
import uk.nhs.hee.trainee.details.dto.enumeration.CctTrainingGrade;
import uk.nhs.hee.trainee.details.exception.NotRecordOwnerException;
import uk.nhs.hee.trainee.details.mapper.CctMapper;
import uk.nhs.hee.trainee.details.model.CctCalculation;
import uk.nhs.hee.trainee.details.repository.CctCalculationRepository;
import uk.nhs.hee.trainee.details.service.CctGradeProgression.Year;

/**
 * A service for CCT related functionality.
 */
@Slf4j
@Service
public class CctService {

  private static final DateTimeFormatter MESSAGE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final TraineeIdentity traineeIdentity;

  private final CctCalculationRepository calculationRepository;
  private final CctMapper mapper;
  private final CctCalculator calculator;
  private final CctSpecialtyService specialtyService;
  private final CctGradeProgression gradeProgression;

  private static final Method CALCULATE_METHOD;
  private static final MethodParameter CALCULATE_METHOD_PARAMETER;

  static {
    try {
      CALCULATE_METHOD = CctService.class.getDeclaredMethod(
          "calculateForSave", CctCalculationSaveRequestDto.class);
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
   * @param calculator            A calculator for CCT dates.
   * @param specialtyService      A service providing the CCT specialties.
   * @param gradeProgression      The grade progression for CCT programmes.
   */
  public CctService(TraineeIdentity traineeIdentity, CctCalculationRepository calculationRepository,
      CctMapper mapper, CctCalculator calculator, CctSpecialtyService specialtyService,
      CctGradeProgression gradeProgression) {
    this.traineeIdentity = traineeIdentity;
    this.calculationRepository = calculationRepository;
    this.mapper = mapper;
    this.calculator = calculator;
    this.specialtyService = specialtyService;
    this.gradeProgression = gradeProgression;
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
   *                                         ends after the calculated CCT date, or the specialty is
   *                                         not listed, or its length or start grade is overridden
   *                                         without notes, or a grade is not in the grade
   *                                         progression, or a grade's accelerated training time is
   *                                         not less than its length.
   */
  public CctCalculationDetailDto createCalculation(CctCalculationSaveRequestDto dto)
      throws MethodArgumentNotValidException {
    log.info("Creating CCT calculation [{}]", dto.name());
    CctCalculateResponseDto calculation = calculateForSave(dto);
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
   *                                         ends after the calculated CCT date, or the specialty is
   *                                         not listed, or its length or start grade is overridden
   *                                         without notes, or a grade is not in the grade
   *                                         progression, or a grade's accelerated training time is
   *                                         not less than its length.
   */
  public Optional<CctCalculationDetailDto> updateCalculation(UUID id,
      CctCalculationSaveRequestDto dto) throws MethodArgumentNotValidException {
    log.info("Updating CCT calculation [{}] with id [{}]", dto.name(), id);

    Optional<CctCalculationDetailDto> existingCalc = getCalculation(id);
    if (existingCalc.isPresent()) {
      CctCalculateResponseDto calculation = calculateForSave(dto);
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
   * @param id The ID of the CCT calculation to delete
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
   * Calculate the CCT date for a programme and its changes, and validate the changes against it
   * and the programme details against the grade progression.
   *
   * @param request The programme details and changes to calculate the CCT date for.
   * @return The calculation, with any errors that would stop it being saved.
   */
  public CctCalculateResponseDto calculate(CctCalculateRequestDto request) {
    CctCalculateResponseDto calculation = calculator.calculate(request.programmeDetails(),
        request.changes());

    BeanPropertyBindingResult validationResult = validateChangeDates(request,
        calculation.result().cctDate());
    validateGradeProgression(request.programmeDetails(), validationResult);
    return calculation.withErrors(
        BodyValidationError.fromFieldErrors(validationResult.getFieldErrors()));
  }

  /**
   * Calculate the CCT date for a CCT calculation being saved, and validate its changes against it
   * and its programme details against the specialty and the grade progression.
   *
   * @param dto The detail of the CCT calculation.
   * @return The changes, with the months they credit, and the CCT date calculated from them.
   * @throws MethodArgumentNotValidException If a change starts before the programme, or starts or
   *                                         ends after the calculated CCT date, or the specialty is
   *                                         not listed, or its length or start grade is overridden
   *                                         without notes, or a grade is not in the grade
   *                                         progression, or a grade's accelerated training time is
   *                                         not less than its length.
   */
  private CctCalculateResponseDto calculateForSave(CctCalculationSaveRequestDto dto)
      throws MethodArgumentNotValidException {
    CctCalculateResponseDto calculation = calculator.calculate(dto.programmeDetails(),
        dto.changes());

    BeanPropertyBindingResult validationResult = validateChangeDates(dto,
        calculation.result().cctDate());
    validateSpecialty(dto.programmeDetails(), validationResult);
    validateGradeProgression(dto.programmeDetails(), validationResult);
    if (validationResult.hasErrors()) {
      log.warn("CCT calculation [{}] cannot be saved: invalid change dates, specialty or grades.",
          dto.name());
      throw new MethodArgumentNotValidException(CALCULATE_METHOD_PARAMETER, validationResult);
    }

    return calculation;
  }

  /**
   * Validate the {@link CctChangeDto} dates against the programme start and the CCT date.
   *
   * @param dto     The programme details and changes the CCT date was calculated from.
   * @param cctDate The CCT date calculated from them.
   * @return The validation result.
   */
  private BeanPropertyBindingResult validateChangeDates(CctCalculationInput dto,
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
   * Validate the programme details against the specialty they name: it must be listed, and a length
   * or start grade that differs from the specialty's must have notes.
   *
   * @param details The programme details.
   * @param result  The validation result to add any errors to.
   */
  private void validateSpecialty(CctProgrammeDetailsDto details,
      BeanPropertyBindingResult result) {
    Optional<CctSpecialtyDto> listed = specialtyService.getSpecialties().stream()
        .filter(specialty -> specialty.name().equals(details.specialty()))
        .findFirst();

    if (listed.isEmpty()) {
      addProgrammeDetailsError(result, "specialty", "Please choose a specialty from the list.");
      return;
    }

    CctSpecialtyDto specialty = listed.get();
    if (details.lengthMonths() != specialty.lengthMonths()
        && !StringUtils.hasText(details.lengthOverrideNotes())) {
      addProgrammeDetailsError(result, "lengthOverrideNotes",
          "Please enter a reason for a different programme length.");
    }
    if (details.startGrade() != specialty.entryGrade()
        && !StringUtils.hasText(details.startGradeOverrideNotes())) {
      addProgrammeDetailsError(result, "startGradeOverrideNotes",
          "Please enter a reason for overriding the default start grade.");
    }
  }

  /**
   * Validate the programme details against the grade progression: each grade they name must be in
   * it, and the accelerated training time in a grade must be less than the grade's length plus its
   * additional training time.
   *
   * @param details The programme details.
   * @param result  The validation result to add any errors to.
   */
  private void validateGradeProgression(CctProgrammeDetailsDto details,
      BeanPropertyBindingResult result) {
    List<Year> years = gradeProgression.years(details);
    Set<CctTrainingGrade> grades = gradesIn(years);
    validateTrainingTimeGrades(details.additionalTrainingTime(), "additionalTrainingTime", grades,
        result);
    validateTrainingTimeGrades(details.acceleratedTrainingTime(), "acceleratedTrainingTime",
        grades, result);

    CctTrainingGrade skippedGrade = details.skippedGrade();
    if (skippedGrade != null
        && !gradesIn(gradeProgression.years(details, null, null)).contains(skippedGrade)) {
      addProgrammeDetailsError(result, "skippedGrade", notInProgression(skippedGrade));
    }
    CctTrainingGrade eighteenMonthFinalGrade = details.eighteenMonthFinalGrade();
    if (eighteenMonthFinalGrade != null
        && !gradesIn(gradeProgression.years(details, skippedGrade, null))
        .contains(eighteenMonthFinalGrade)) {
      addProgrammeDetailsError(result, "eighteenMonthFinalGrade",
          notInProgression(eighteenMonthFinalGrade));
    } else if (eighteenMonthFinalGrade != null && !endsInEighteenMonthYear(years)) {
      addProgrammeDetailsError(result, "eighteenMonthFinalGrade",
          "An 18-month final year must be the last full year of a programme length that includes "
              + "a half year, such as 66 months.");
    }

    Map<CctTrainingGrade, Integer> gradeMonths = years.stream()
        .filter(year -> year.grade() != null)
        .collect(groupingBy(Year::grade, summingInt(Year::gradeMonths)));
    List<CctTrainingTimeDto> accelerated = details.acceleratedTrainingTime();
    Set<CctTrainingGrade> checked = new HashSet<>();
    for (int index = 0; index < accelerated.size(); index++) {
      CctTrainingGrade grade = accelerated.get(index).grade();
      if (gradeMonths.containsKey(grade) && checked.add(grade)) {
        int limit = gradeMonths.get(grade) + monthsIn(details.additionalTrainingTime(), grade);
        if (monthsIn(accelerated, grade) >= limit) {
          addProgrammeDetailsError(result,
              "acceleratedTrainingTime[%d].months".formatted(index),
              "Accelerated training time in %s must be less than %d months."
                  .formatted(grade, limit));
        }
      }
    }
  }

  /**
   * Validate that each training time entry's grade is in the grade progression.
   *
   * @param entries The training time entries.
   * @param field   The name of the programme details field holding them.
   * @param grades  The grades in the grade progression.
   * @param result  The validation result to add any errors to.
   */
  private static void validateTrainingTimeGrades(List<CctTrainingTimeDto> entries, String field,
      Set<CctTrainingGrade> grades, BeanPropertyBindingResult result) {
    for (int index = 0; index < entries.size(); index++) {
      CctTrainingGrade grade = entries.get(index).grade();
      if (!grades.contains(grade)) {
        addProgrammeDetailsError(result, "%s[%d].grade".formatted(field, index),
            notInProgression(grade));
      }
    }
  }

  private static boolean endsInEighteenMonthYear(List<Year> years) {
    Year last = years.get(years.size() - 1);
    return last.extendedToEighteenMonths() && last.gradeMonths() == 18;
  }

  private static Set<CctTrainingGrade> gradesIn(List<Year> years) {
    return years.stream()
        .map(Year::grade)
        .filter(Objects::nonNull)
        .collect(toSet());
  }

  private static int monthsIn(List<CctTrainingTimeDto> entries, CctTrainingGrade grade) {
    return entries.stream()
        .filter(entry -> entry.grade() == grade)
        .mapToInt(CctTrainingTimeDto::months)
        .sum();
  }

  private static String notInProgression(CctTrainingGrade grade) {
    return grade + " is not part of this programme's grade progression.";
  }

  /**
   * Add an error for a {@link CctProgrammeDetailsDto} field to a validation result.
   *
   * @param result  The validation result.
   * @param field   The name of the field.
   * @param message The error message.
   */
  private static void addProgrammeDetailsError(BeanPropertyBindingResult result, String field,
      String message) {
    result.addError(new FieldError("CctProgrammeDetailsDto", "programmeDetails." + field, message));
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
