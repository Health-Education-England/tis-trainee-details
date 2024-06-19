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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

/**
 * A service for handling trainee NTNs/DRNs.
 */
@Slf4j
@Service
public class NtnGenerator {

  /**
   * Populate the NTN for all programme memberships in the trainee profile.
   *
   * @param traineeProfile The trainee profile to populate with NTNs.
   */
  public void populateNtns(TraineeProfile traineeProfile) {
    PersonalDetails personalDetails = traineeProfile.getPersonalDetails();

    if (isExcluded(personalDetails)) {
      return;
    }

    traineeProfile.getProgrammeMemberships().forEach(pm -> populateNtn(personalDetails, pm));
  }

  /**
   * Populate the NTN for the given programme membership.
   *
   * @param personalDetails     The personal details to use for NTN generation.
   * @param programmeMembership The programme membership to generate the NTN for.
   */
  private void populateNtn(PersonalDetails personalDetails,
      ProgrammeMembership programmeMembership) {
    log.info("Populating NTN for programme membership '{}'.", programmeMembership.getTisId());

    if (isExcluded(programmeMembership)) {
      return;
    }

    String parentOrganization = getParentOrganization(programmeMembership);
    String specialtyConcat = getSpecialtyConcat(programmeMembership);
    String referenceNumber = getReferenceNumber(personalDetails);
    String suffix = getSuffix(programmeMembership);
    String ntn = parentOrganization + "/" + specialtyConcat + "/" + referenceNumber + "/" + suffix;
    programmeMembership.setNtn(ntn);
    log.info("Populated NTN: {}.", ntn);
  }

  /**
   * Get the parent organization for the given programme membership.
   *
   * @param programmeMembership The programme membership to calculate the parent organization for.
   * @return The calculated parent organization.
   */
  private String getParentOrganization(ProgrammeMembership programmeMembership) {
    String managingDeanery = programmeMembership.getManagingDeanery();
    log.info("Calculating parent organization for managing deanery '{}'.", managingDeanery);

    String parentOrganization = managingDeanery == null ? null : switch (managingDeanery) {
      case "Defence Postgraduate Medical Deanery" -> "TSD";
      case "Health Education England East Midlands" -> "EMD";
      case "Health Education England East of England" -> "EAN";
      case "Health Education England Kent, Surrey and Sussex" -> "KSS";
      case "Health Education England North Central and East London",
          "Health Education England South London",
          "Health Education England North West London",
          "London LETBs" -> "LDN";
      case "Health Education England North East" -> "NTH";
      case "Health Education England North West" -> "NWE";
      case "Health Education England South West" ->
          getSouthWestParentOrganization(programmeMembership);
      case "Health Education England Thames Valley" -> "OXF";
      case "Health Education England Wessex" -> "WES";
      case "Health Education England West Midlands" -> "WMD";
      case "Health Education England Yorkshire and The Humber" -> "YHD";
      case "Severn Deanery" -> "SEV";
      case "South West Peninsula Deanery" -> "PEN";
      default -> null;
    };

    if (parentOrganization == null) {
      throw new IllegalArgumentException("Unable to calculate the parent organization.");
    }

    log.info("Calculated parent organization: '{}'.", parentOrganization);
    return parentOrganization;
  }

  /**
   * Get the parent organization for a programme in the South West.
   *
   * @param programmeMembership The SW programme membership.
   * @return The calculated parent organization.
   */
  private String getSouthWestParentOrganization(ProgrammeMembership programmeMembership) {
    String programmeNumber = programmeMembership.getProgrammeNumber();
    log.info("Using programme number '{}' to calculate parent organization.", programmeNumber);
    return programmeNumber.startsWith("SWP") ? "PEN" : programmeNumber.substring(0, 3);
  }

  /**
   * Get the concatenated specialty string for the programme membership's NTN.
   *
   * @param programmeMembership The programme membership to get the specialty string for.
   * @return The concatenated specialty string.
   */
  private String getSpecialtyConcat(ProgrammeMembership programmeMembership) {
    log.info("Calculating specialty concat.");
    List<Curriculum> curricula = programmeMembership.getCurricula();
    List<Curriculum> sortedCurricula = filterAndSortCurricula(curricula);

    StringBuilder sb = new StringBuilder();

    for (ListIterator<Curriculum> curriculaIterator = sortedCurricula.listIterator();
        curriculaIterator.hasNext(); ) {
      int index = curriculaIterator.nextIndex();
      Curriculum curriculum = curriculaIterator.next();
      String specialtyCode = curriculum.getCurriculumSpecialtyCode();

      if (index > 0) {
        if (curriculum.getCurriculumSubType().equals("SUB_SPECIALTY")) {
          log.info("Appending sub-specialty '{}'.", specialtyCode);
          sb.append(".");
        } else {
          log.info("Appending specialty '{}'.", specialtyCode);
          sb.append("-");
        }
      } else {
        log.info("Using '{}' as first specialty.", specialtyCode);
      }

      sb.append(specialtyCode);

      if (index == 0 && Objects.equals(curriculum.getCurriculumName(), "AFT")) {
        sb.append("-FND");
        break;
      }
    }

    log.info("Calculated specialty concat: '{}'.", sb);
    return sb.toString();
  }

  /**
   * Get the GMC/GDC reference number for the given personal details.
   *
   * @param personalDetails The personal details to get the reference number for.
   * @return The GMC/GDC number, depending on which is valid.
   */
  private String getReferenceNumber(PersonalDetails personalDetails) {
    String gmcNumber = personalDetails.getGmcNumber();
    return gmcNumber.matches("\\d{7}") ? gmcNumber : personalDetails.getGdcNumber();
  }

  /**
   * Get the NTN suffix for the given programme membership.
   *
   * @param programmeMembership The programme membership to get the suffix for.
   * @return The calculated suffix for the programme membership's NTN.
   */
  private String getSuffix(ProgrammeMembership programmeMembership) {
    log.info("Calculating suffix.");
    String trainingPathway = programmeMembership.getTrainingPathway();
    log.info("Using training pathway '{}' to calculate suffix.", trainingPathway);

    String suffix = switch (trainingPathway) {
      case "CCT" -> "C";
      case "CESR" -> "CP";
      default -> {
        List<Curriculum> curricula = programmeMembership.getCurricula();
        List<Curriculum> sortedCurricula = filterAndSortCurricula(curricula);
        String firstSpecialtyCode = sortedCurricula.get(0).getCurriculumSpecialtyCode();
        log.info("Using specialty code '{}' to calculate suffix.", trainingPathway);

        yield Objects.equals(firstSpecialtyCode, "ACA") ? "C" : "D";
      }
    };

    log.info("Calculated suffix: '{}'.", suffix);
    return suffix;
  }

  /**
   * Filter the given curricula to only those that are current and sort them alphanumerically.
   *
   * @param curricula The curricula to filter and sort.
   * @return The current curricula, sorted alphanumerically by subtype and code.
   */
  private List<Curriculum> filterAndSortCurricula(List<Curriculum> curricula) {
    LocalDate now = LocalDate.now();

    return curricula.stream()
        .filter(c ->
            c.getCurriculumSpecialtyCode() != null && !c.getCurriculumSpecialtyCode().isBlank())
        .filter(c -> c.getCurriculumStartDate().isBefore(now))
        .filter(c -> c.getCurriculumEndDate().isAfter(now))
        .sorted(Comparator
            .comparing(Curriculum::getCurriculumSubType)
            .reversed()
            .thenComparing(Curriculum::getCurriculumSpecialtyCode)
            .reversed()
        )
        .toList();
  }

  /**
   * Check whether the given personal details excludes NTN generation.
   *
   * @param personalDetails The personal details to check.
   * @return true if NTN generated cannot continue, else false.
   */
  private boolean isExcluded(PersonalDetails personalDetails) {
    if (personalDetails == null) {
      log.info("Skipping NTN population as personal details not available.");
      return true;
    }

    String gmcNumber = personalDetails.getGmcNumber();

    if (gmcNumber == null || !gmcNumber.matches("\\d{7}")) {
      String gdcNumber = personalDetails.getGdcNumber();

      if (gdcNumber == null || !gdcNumber.matches("\\d{5}.*")) {
        log.info("Skipping NTN population as reference number not valid.");
        return true;
      }
    }

    return false;
  }

  /**
   * Check whether the given programme membership is excluded for NTN generation.
   *
   * @param programmeMembership The programme membership to check.
   * @return true if NTN generated cannot continue, else false.
   */
  private boolean isExcluded(ProgrammeMembership programmeMembership) {
    String programmeNumber = programmeMembership.getProgrammeNumber();
    if (programmeNumber == null || programmeNumber.isBlank()) {
      log.info("Skipping NTN population as programme number is blank.");
      return true;
    }

    String programmeName = programmeMembership.getProgrammeName();
    if (programmeName == null || programmeName.isBlank()) {
      log.info("Skipping NTN population as programme name is blank.");
      return true;
    }

    String lowerProgrammeName = programmeName.toLowerCase();
    if (lowerProgrammeName.contains("foundation")) {
      log.info("Skipping NTN population as programme name '{}' is excluded.",
          programmeMembership.getProgrammeName());
      return true;
    }

    List<Curriculum> validCurricula = filterAndSortCurricula(programmeMembership.getCurricula());
    if (validCurricula.isEmpty()) {
      log.info("Skipping NTN population as there are no valid curricula.");
      return true;
    }

    if (programmeMembership.getTrainingPathway() == null) {
      log.error("Unable to generate NTN as training pathway was null.");
      return true;
    }

    return false;
  }
}