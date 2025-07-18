/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

import static uk.nhs.hee.trainee.details.model.HrefType.ABSOLUTE_URL;
import static uk.nhs.hee.trainee.details.model.HrefType.NON_HREF;
import static uk.nhs.hee.trainee.details.model.HrefType.PROTOCOL_EMAIL;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.templatemode.TemplateMode;
import uk.nhs.hee.trainee.details.dto.enumeration.GoldGuideVersion;
import uk.nhs.hee.trainee.details.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.trainee.details.model.ConditionsOfJoining;
import uk.nhs.hee.trainee.details.model.Curriculum;
import uk.nhs.hee.trainee.details.model.LocalOfficeContactType;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;
import uk.nhs.hee.trainee.details.repository.TraineeProfileRepository;

/**
 * Programme Membership Service.
 */
@Service
@XRayEnabled
@Slf4j
public class ProgrammeMembershipService {

  protected static final String API_GET_OWNER_CONTACT
      = "/api/local-office-contact-by-lo-name/{localOfficeName}";
  protected static final String DEFAULT_NO_CONTACT_MESSAGE
      = "your local office";
  protected static final List<String> MEDICAL_CURRICULA
      = List.of("DENTAL_CURRICULUM", "DENTAL_POST_CCST", "MEDICAL_CURRICULUM");
  protected static final List<String> TSS_CURRICULA
      = List.of("MEDICAL_CURRICULUM", "MEDICAL_SPR");
  protected static final List<String> NOT_TSS_SPECIALTIES
      = List.of("Public Health Medicine", "Foundation");
  protected static final List<String> NON_RELEVANT_PROGRAMME_MEMBERSHIP_TYPES
      = List.of("VISITOR", "LAT");
  protected static final Long PROGRAMME_BREAK_DAYS = 355L;
  protected static final String OWNER_FIELD = "localOfficeName";
  protected static final String CONTACT_TYPE_FIELD = "contactTypeName";
  protected static final String CONTACT_FIELD = "contact";
  protected static final int PM_CONFIRM_WEEKS = 12;

  protected static final List<String> PILOT_2024_LOCAL_OFFICES_ALL_PROGRAMMES
      = List.of("London LETBs",
      "North Central and East London",
      "South London",
      "North West London",
      "Kent, Surrey and Sussex",
      "East Midlands",
      "West Midlands",
      "East of England",
      "Wessex");

  protected static final List<String> PILOT_2024_ROLLOUT_LOCAL_OFFICES
      = List.of("London LETBs",
      "North Central and East London",
      "South London",
      "North West London",
      "Kent, Surrey and Sussex",
      "East Midlands",
      "West Midlands",
      "East of England",
      "Wessex",
      "Yorkshire and the Humber",
      "South West",
      "North East",
      "North West",
      "Thames Valley");

  protected static final List<String> PILOT_2024_NW_SPECIALTIES = List.of(
      "Cardiothoracic surgery",
      "Core surgical training",
      "General surgery",
      "Neurosurgery",
      "Ophthalmology",
      "Oral and maxillofacial surgery",
      "Otolaryngology",
      "Paediatric Surgery",
      "Plastic Surgery",
      "Trauma and Orthopaedic Surgery",
      "Urology",
      "Vascular surgery");

  private static final String PM_CONFIRMATION_TEMPLATE_PATH = "programme-confirmation";

  private final TraineeProfileRepository repository;
  private final ProgrammeMembershipMapper mapper;
  private final CachingDelegate cachingDelegate;
  private final PdfGeneratingService pdfService;
  private final RestTemplate restTemplate;
  private final String referenceUrl;
  private final String templateVersion;

  ProgrammeMembershipService(TraineeProfileRepository repository, ProgrammeMembershipMapper mapper,
      CachingDelegate cachingDelegate, PdfGeneratingService pdfService, RestTemplate restTemplate,
      @Value("${service.reference.url}") String referenceUrl,
      @Value("${application.template-versions.programme-confirmation}") String templateVersion) {
    this.repository = repository;
    this.mapper = mapper;
    this.cachingDelegate = cachingDelegate;
    this.pdfService = pdfService;
    this.restTemplate = restTemplate;
    this.referenceUrl = referenceUrl;
    this.templateVersion = templateVersion;
  }

  /**
   * Update the programme membership for the trainee with the given TIS ID.
   *
   * @param traineeTisId        The TIS id of the trainee.
   * @param programmeMembership The programme membership to update for the trainee.
   * @return The updated programme membership or empty if a trainee with the ID was not found.
   */
  public Optional<ProgrammeMembership> updateProgrammeMembershipForTrainee(String traineeTisId,
      ProgrammeMembership programmeMembership) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return Optional.empty();
    }

    List<ProgrammeMembership> existingProgrammeMemberships = traineeProfile
        .getProgrammeMemberships();

    if (programmeMembership.getConditionsOfJoining() == null
        || programmeMembership.getConditionsOfJoining().signedAt() == null) {

      // Restore the Conditions of Joining if it exists. This covers the (generally short-term)
      // case when a CoJ has just been signed, but the data has not yet made the round-trip to TIS
      // and tis-trainee-sync, enriching the incoming programme membership with this information.

      UUID uuid = UUID.fromString(programmeMembership.getTisId());
      ProgrammeMembership savedProgrammeMembership
          = existingProgrammeMemberships.stream()
          .filter(i -> i.getTisId().equals(uuid.toString()))
          .findAny()
          .orElse(null);
      if (savedProgrammeMembership != null
          && savedProgrammeMembership.getConditionsOfJoining() != null) {
        ConditionsOfJoining savedCoj = savedProgrammeMembership.getConditionsOfJoining();
        programmeMembership.setConditionsOfJoining(savedCoj);
      }
    }

    for (ProgrammeMembership existingProgrammeMembership : existingProgrammeMemberships) {

      if (existingProgrammeMembership.getTisId().equals(programmeMembership.getTisId())) {
        mapper.updateProgrammeMembership(existingProgrammeMembership, programmeMembership);
        repository.save(traineeProfile);
        return Optional.of(existingProgrammeMembership);
      }
    }

    existingProgrammeMemberships.add(programmeMembership);
    repository.save(traineeProfile);
    return Optional.of(programmeMembership);
  }

  /**
   * Delete the programme memberships for the trainee with the given TIS ID.
   *
   * @param traineeTisId The TIS id of the trainee.
   * @return True, or False if a trainee with the ID was not found.
   */
  public boolean deleteProgrammeMembershipsForTrainee(String traineeTisId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return false;
    }
    List<ProgrammeMembership> existingProgrammeMemberships = traineeProfile
        .getProgrammeMemberships();

    // Cache any signed Conditions of Joining so that it can be restored later.
    existingProgrammeMemberships.stream()
        .filter(pm -> pm.getConditionsOfJoining() != null
            && pm.getConditionsOfJoining().signedAt() != null)
        .forEach(pm -> {
          UUID uuid = UUID.fromString(pm.getTisId());
          cachingDelegate.cacheConditionsOfJoining(uuid.toString(),
              pm.getConditionsOfJoining());
        });

    existingProgrammeMemberships.clear();
    repository.save(traineeProfile);

    return true;
  }

  /**
   * Delete the matching programme membership for the trainee with the given TIS ID.
   *
   * @param traineeTisId          The TIS id of the trainee.
   * @param programmeMembershipId The ID of the programme membership to delete.
   * @return True, or False if a trainee with the ID was not found.
   */
  public boolean deleteProgrammeMembershipForTrainee(String traineeTisId,
      String programmeMembershipId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return false;
    }

    for (var iter = traineeProfile.getProgrammeMemberships().iterator(); iter.hasNext(); ) {
      ProgrammeMembership programmeMembership = iter.next();

      if (programmeMembership.getTisId().equals(programmeMembershipId)) {
        iter.remove();
        repository.save(traineeProfile);
        return true;
      }
    }

    return false;
  }

  /**
   * Sign Condition of Joining with the given programme membership ID.
   *
   * @param programmeMembershipId The ID of the programme membership for signing COJ.
   * @return The updated programme membership or empty if the programme membership with the ID was
   *     not found.
   */
  public Optional<ProgrammeMembership> signProgrammeMembershipCoj(
      String traineeTisId, String programmeMembershipId) {

    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile != null) {
      List<ProgrammeMembership> existingProgrammeMemberships = traineeProfile
          .getProgrammeMemberships();

      for (ProgrammeMembership existingProgrammeMembership : existingProgrammeMemberships) {
        if (existingProgrammeMembership.getTisId().equals(programmeMembershipId)) {
          ConditionsOfJoining conditionsOfJoining =
              new ConditionsOfJoining(Instant.now(), GoldGuideVersion.getLatest(), null);
          existingProgrammeMembership.setConditionsOfJoining(conditionsOfJoining);
          repository.save(traineeProfile);
          return Optional.of(existingProgrammeMembership);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Assess if the programme membership for a trainee is a new starter.
   *
   * @param traineeTisId          The TIS id of the trainee.
   * @param programmeMembershipId The ID of the programme membership to assess.
   * @return True, or False if the programme membership is not a new starter.
   */
  public boolean isNewStarter(String traineeTisId, String programmeMembershipId) {
    TraineeProfile traineeProfile = getProfileWithMedicalProgrammeMemberships(traineeTisId);

    if (traineeProfile == null) {
      log.info("New starter: [false] trainee profile {} not found", traineeTisId);
      return false;
    }

    ProgrammeMembership programmeMembership = getCandidateProgrammeMembership(
        traineeProfile.getProgrammeMemberships(), programmeMembershipId);
    if (programmeMembership == null) {
      log.info("New starter: [false] programme membership {} does not exist, is non-medical or "
              + "has wrong type",
          programmeMembershipId);
      return false;
    }

    //it cannot be a new starter if it has already finished
    if (programmeMembership.getEndDate() == null
        || programmeMembership.getEndDate().isBefore(LocalDate.now())) {
      log.info("New starter: [false] programme membership {} finished {}",
          programmeMembershipId, programmeMembership.getEndDate());
      return false;
    }

    List<ProgrammeMembership> otherPms
        = traineeProfile.getProgrammeMemberships().stream()
        .filter(pm -> !pm.getTisId().equals(programmeMembershipId)).toList();

    //if there are no preceding PMs, it is a new starter
    List<ProgrammeMembership> precedingPms = getRecentPrecedingPms(programmeMembership, otherPms);
    if (precedingPms.isEmpty()) {
      log.info("New starter: [true] there are no preceding programme memberships "
          + "that ended within {} days", PROGRAMME_BREAK_DAYS);
      return true;
    }

    //if none of the preceding PMs are intra-deanery transfer or rota PMs, it is a new starter
    List<ProgrammeMembership> intraOrRotaPms = getIntraOrRotaPms(programmeMembership, precedingPms);
    log.info("New starter: [{}] there are {} preceding intra-deanery / rota programme memberships ",
        intraOrRotaPms.isEmpty() ? "true" : "false", intraOrRotaPms.size());
    return intraOrRotaPms.isEmpty();
    //otherwise it is not a new starter
  }

  /**
   * Check whether a trainee can be onboarded based on the given programme membership.
   *
   * @param programmeMembership The programme membership to assess.
   * @return Whether the trainee can be onboarded based on this placement.
   */
  public boolean canBeOnboarded(ProgrammeMembership programmeMembership) {
    if (!hasProgrammeMembershipTypeOfInterest(programmeMembership)) {
      log.info("Programme Membership not valid for onboarding with type '{}'.",
          programmeMembership.getProgrammeMembershipType());
      return false;
    }

    return !getPmsTssCurricula(List.of(programmeMembership)).isEmpty();
  }

  /**
   * Assess if the programme membership for a trainee is in the 2024 pilot. Hopefully a temporary
   * kludge.
   *
   * @param traineeTisId          The TIS id of the trainee.
   * @param programmeMembershipId The ID of the programme membership to assess.
   * @return True, or False if the programme membership is not in the 2024 pilot.
   */
  public boolean isPilot2024(String traineeTisId, String programmeMembershipId) {
    TraineeProfile traineeProfile = getProfileWithTssProgrammeMemberships(traineeTisId);

    if (traineeProfile == null) {
      log.info("2024 pilot: [false] trainee profile {} not found", traineeTisId);
      return false;
    }

    ProgrammeMembership programmeMembership = getCandidateProgrammeMembership(
        traineeProfile.getProgrammeMemberships(), programmeMembershipId);
    if (programmeMembership == null) {
      log.info("2024 pilot: [false] programme membership {} does not exist, is non-medical or "
              + "has wrong type",
          programmeMembershipId);
      return false;
    }

    String managingDeanery = programmeMembership.getManagingDeanery();
    LocalDate startDate = programmeMembership.getStartDate();
    LocalDate dayBefore01082024 = LocalDate.of(2024, 7, 31);
    LocalDate dayAfter31102024 = LocalDate.of(2024, 11, 1);
    if ((PILOT_2024_LOCAL_OFFICES_ALL_PROGRAMMES.stream()
        .anyMatch(lo -> lo.equalsIgnoreCase(managingDeanery)))
        && (startDate.isAfter(dayBefore01082024) && startDate.isBefore(dayAfter31102024))) {
      return true;
    }

    if (managingDeanery.equalsIgnoreCase("Yorkshire and the Humber")
        && (startDate.isAfter(dayBefore01082024) && startDate.isBefore(dayAfter31102024))
        && programmeMembership.getCurricula().stream().noneMatch(
        c -> c.getCurriculumSpecialty().equalsIgnoreCase("General Practice"))) {
      return true;
    }

    if (managingDeanery.equalsIgnoreCase("South West")
        && (startDate.isAfter(dayBefore01082024) && startDate.isBefore(dayAfter31102024))
        && programmeMembership.getCurricula().stream().noneMatch(
        c -> c.getCurriculumSpecialty().equalsIgnoreCase("General Practice"))) {
      return true;
    }

    LocalDate dayAfter31082024 = LocalDate.of(2024, 9, 1);
    return managingDeanery.equalsIgnoreCase("North West")
        && (startDate.isAfter(dayBefore01082024) && startDate.isBefore(dayAfter31082024))
        && (programmeMembership.getCurricula().stream().anyMatch(c ->
        PILOT_2024_NW_SPECIALTIES.stream().anyMatch(
            s -> s.equalsIgnoreCase(c.getCurriculumSpecialty())))
        || programmeMembership.getProgrammeName()
        .equalsIgnoreCase("Cardio-thoracic surgery (run through)")
        || programmeMembership.getProgrammeName()
        .equalsIgnoreCase("Oral and maxillo-facial surgery (run through)"));
  }

  /**
   * Assess if the programme membership for a trainee is in the 2024 pilot rollout. Hopefully a
   * temporary kludge.
   *
   * @param traineeTisId          The TIS id of the trainee.
   * @param programmeMembershipId The ID of the programme membership to assess.
   * @return True, or False if the programme membership is not in the 2024 pilot rollout.
   */
  public boolean isPilotRollout2024(String traineeTisId, String programmeMembershipId) {
    TraineeProfile traineeProfile = getProfileWithTssProgrammeMemberships(traineeTisId);

    if (traineeProfile == null) {
      log.info("2024 pilot rollout: [false] trainee profile {} not found", traineeTisId);
      return false;
    }

    ProgrammeMembership programmeMembership = getCandidateProgrammeMembership(
        traineeProfile.getProgrammeMemberships(), programmeMembershipId);
    if (programmeMembership == null) {
      log.info("2024 pilot rollout: [false] programme membership {} does not exist, is non-medical "
              + "or has wrong type",
          programmeMembershipId);
      return false;
    }

    String managingDeanery = Objects.toString(programmeMembership.getManagingDeanery(), "");
    LocalDate startDate = programmeMembership.getStartDate();
    if (startDate == null) {
      log.info("2024 pilot rollout: [false] start date is null for {}", programmeMembershipId);
      return false;
    }

    LocalDate notificationEpoch = LocalDate.of(2024, 10, 31);
    if (managingDeanery.equalsIgnoreCase("Thames Valley")) {
      notificationEpoch = LocalDate.of(2025, 1, 31);
    }
    if (managingDeanery.equalsIgnoreCase("North East")) {
      notificationEpoch = LocalDate.of(2025, 4, 13);
    }
    return ((PILOT_2024_ROLLOUT_LOCAL_OFFICES.stream()
        .anyMatch(lo -> lo.equalsIgnoreCase(managingDeanery)))
        && startDate.isAfter(notificationEpoch));
  }

  /**
   * Get a trainee profile with programme memberships with only medical curricula.
   *
   * @param traineeTisId The TIS id of the trainee.
   * @return The filtered trainee profile .
   */
  public TraineeProfile getProfileWithMedicalProgrammeMemberships(String traineeTisId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return null;
    }

    //get the list of programme memberships with only medical curricula attached
    List<ProgrammeMembership> pmsToConsider
        = getPmsMedicalCurricula(traineeProfile.getProgrammeMemberships());

    traineeProfile.setProgrammeMemberships(pmsToConsider);
    return traineeProfile;
  }

  /**
   * Get a trainee profile with programme memberships with TSS-relevant curricula.
   *
   * @param traineeTisId The TIS id of the trainee.
   * @return The filtered trainee profile .
   */
  public TraineeProfile getProfileWithTssProgrammeMemberships(String traineeTisId) {
    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile == null) {
      return null;
    }

    //get the list of programme memberships with only TSS-applicable attached
    List<ProgrammeMembership> pmsToConsider
        = getPmsTssCurricula(traineeProfile.getProgrammeMemberships());

    traineeProfile.setProgrammeMemberships(pmsToConsider);
    return traineeProfile;
  }

  /**
   * Generate programme confirmation PDF of a programme membership.
   *
   * @param programmeMembershipId The ID of the programme membership for generating PDF.
   * @return The generated Programme Membership confirmation PDF.
   */
  public byte[] generateProgrammeMembershipPdf(
      String traineeTisId, String programmeMembershipId) throws IOException {

    TraineeProfile traineeProfile = repository.findByTraineeTisId(traineeTisId);

    if (traineeProfile != null) {
      PersonalDetails personalDetails = traineeProfile.getPersonalDetails();
      List<ProgrammeMembership> existingProgrammeMemberships = traineeProfile
          .getProgrammeMemberships();

      // Only generate when PM start day is within 12 weeks
      for (ProgrammeMembership programmeMembership : existingProgrammeMemberships) {
        if (programmeMembership.getTisId().equals(programmeMembershipId)) {
          if (programmeMembership.getStartDate().minusWeeks(PM_CONFIRM_WEEKS).minusDays(1)
              .isBefore(LocalDate.now())) {

            // Template Variables
            String contact = getOwnerContact(programmeMembership.getManagingDeanery(),
                LocalOfficeContactType.ONBOARDING_SUPPORT,
                LocalOfficeContactType.TSS_SUPPORT, "");

            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("pm", programmeMembership);
            templateVariables.put("trainee", personalDetails);
            templateVariables.put("localOfficeContact", contact);
            templateVariables.put("contactHref", getHrefTypeForContact(contact));

            // Generate PDF with template
            String templatePath =
                PM_CONFIRMATION_TEMPLATE_PATH + File.separatorChar + templateVersion + ".html";
            TemplateSpec templateSpec = new TemplateSpec(
                templatePath, Set.of(), TemplateMode.HTML, null);

            return pdfService.generatePdf(templateSpec, templateVariables);
          } else {
            throw new IllegalArgumentException("Programme membership " + programmeMembershipId
                + " not starting in " + PM_CONFIRM_WEEKS + " weeks.");
          }
        }
      }
      throw new IllegalArgumentException(
          "No matched Programme membership " + programmeMembershipId + ".");
    }
    throw new IllegalArgumentException("Trainee Profile " + traineeTisId + " not found.");
  }

  /**
   * Get the programme membership that is a candidate for new-starter or pilot 2024 assessment.
   *
   * @param programmeMemberships  The list of programme memberships.
   * @param programmeMembershipId The programme membership ID.
   * @return The programme membership, or null if it is not a candidate because it does not exist,
   *     it is non-medical, or is of the wrong type.
   */
  private ProgrammeMembership getCandidateProgrammeMembership(
      List<ProgrammeMembership> programmeMemberships, String programmeMembershipId) {
    //get the programme membership that must be assessed
    Optional<ProgrammeMembership> optionalProgrammeMembership
        = programmeMemberships.stream()
        .filter(p -> p.getTisId().equals(programmeMembershipId)).findAny();

    //return null if it does not exist, or if it is not a medical one
    if (optionalProgrammeMembership.isEmpty()) {
      return null;
    }

    ProgrammeMembership programmeMembership = optionalProgrammeMembership.get();
    //it cannot be a pilot / new-starter candidate if it lacks a relevant programme membership type
    if (!hasProgrammeMembershipTypeOfInterest(programmeMembership)) {
      return null;
    }
    return programmeMembership;
  }

  /**
   * Is the programme membership of a type relevant to new-starters or the 2024 pilot.
   *
   * @param programmeMembership The programme membership to assess.
   * @return True if it is relevant, otherwise False.
   */
  private boolean hasProgrammeMembershipTypeOfInterest(ProgrammeMembership programmeMembership) {
    return programmeMembership.getProgrammeMembershipType() != null
        && NON_RELEVANT_PROGRAMME_MEMBERSHIP_TYPES.stream()
        .noneMatch(programmeMembership.getProgrammeMembershipType()::equalsIgnoreCase);
  }

  /**
   * Remove non-medical curricula from a list of programme memberships. Returned programme
   * memberships will each contain at least one medical curriculum.
   *
   * @param pms The list of programme memberships.
   * @return The filtered list of medical curricula containing programme memberships.
   */
  private List<ProgrammeMembership> getPmsMedicalCurricula(List<ProgrammeMembership> pms) {
    List<ProgrammeMembership> filteredPms = new ArrayList<>();
    for (ProgrammeMembership programmeMembership : pms) {
      List<Curriculum> filteredCms = programmeMembership.getCurricula().stream()
          .filter(c -> {
            String subtype = c.getCurriculumSubType();
            if (subtype == null) {
              return false;
            } else {
              return MEDICAL_CURRICULA.stream().anyMatch(subtype::equalsIgnoreCase);
            }
          })
          .toList();
      if (!filteredCms.isEmpty()) {
        programmeMembership.setCurricula(filteredCms);
        filteredPms.add(programmeMembership);
      }
    }
    return filteredPms;
  }

  /**
   * Remove non-TSS curricula from a list of programme memberships. Returned programme memberships
   * will each contain at least one TSS curriculum.
   *
   * @param pms The list of programme memberships.
   * @return The filtered list of programme memberships containing TSS-applicable curricula.
   */
  private List<ProgrammeMembership> getPmsTssCurricula(List<ProgrammeMembership> pms) {
    List<ProgrammeMembership> filteredPms = new ArrayList<>();
    for (ProgrammeMembership programmeMembership : pms) {
      List<Curriculum> filteredCms = programmeMembership.getCurricula().stream()
          .filter(c -> {
            String subtype = c.getCurriculumSubType();
            if (subtype == null) {
              return false;
            } else {
              return TSS_CURRICULA.stream().anyMatch(subtype::equalsIgnoreCase);
            }
          })
          .filter(c -> {
            String specialty = c.getCurriculumSpecialty();
            if (specialty == null) {
              return false; //should not really happen
            } else {
              return NOT_TSS_SPECIALTIES.stream().noneMatch(specialty::equalsIgnoreCase);
            }
          })
          .toList();
      if (!filteredCms.isEmpty()) {
        programmeMembership.setCurricula(filteredCms);
        filteredPms.add(programmeMembership);
      }
    }
    return filteredPms;
  }

  /**
   * Get programme memberships that comprise intra-deanery transfers or rotas for another programme
   * membership.
   *
   * @param anchorPm     The programme membership against which to assess the others.
   * @param candidatePms The list of candidate programme memberships.
   * @return The programme memberships that comprise intra-deanery transfers or rotas.
   */
  private List<ProgrammeMembership> getIntraOrRotaPms(ProgrammeMembership anchorPm,
      List<ProgrammeMembership> candidatePms) {
    List<ProgrammeMembership> newStarterPmsWithSameDeaneryStartedBeforeAnchor =
        candidatePms.stream()
            .filter(pm -> {
              if (pm.getProgrammeMembershipType() == null) {
                return false;
              } else {
                return (NON_RELEVANT_PROGRAMME_MEMBERSHIP_TYPES.stream()
                    .noneMatch(pm.getProgrammeMembershipType()::equalsIgnoreCase));
              }
            })
            .filter(pm -> {
              if (pm.getManagingDeanery() == null || anchorPm.getManagingDeanery() == null) {
                return false;
              } else {
                return pm.getManagingDeanery().equalsIgnoreCase(anchorPm.getManagingDeanery());
              }
            })
            .filter(pm ->
                    pm.getStartDate().isBefore(anchorPm.getStartDate())
            //dates cannot be null because any offenders removed in getRecentPrecedingPms()
            ).toList();

    List<String> anchorPmCurriculumSpecialties = anchorPm.getCurricula().stream()
        .map(Curriculum::getCurriculumSpecialtyCode)
        .filter(Objects::nonNull).toList();

    return new ArrayList<>(newStarterPmsWithSameDeaneryStartedBeforeAnchor.stream()
        .filter(pm -> {
          List<String> sharedCurriculumSpecialties = new ArrayList<>(pm.getCurricula().stream()
              .map(Curriculum::getCurriculumSpecialtyCode)
              .filter(Objects::nonNull).toList());
          sharedCurriculumSpecialties.retainAll(anchorPmCurriculumSpecialties);

          return !sharedCurriculumSpecialties.isEmpty();
        }).toList());
  }

  /**
   * Get the list of programme memberships that started before another programme membership and
   * finished within PROGRAMME_BREAK_DAYS of it, sorted in descending start-date order.
   *
   * @param anchorPm     The programme membership against which to assess the others.
   * @param candidatePms The possible programme memberships.
   * @return The filtered list.
   */
  private List<ProgrammeMembership> getRecentPrecedingPms(ProgrammeMembership anchorPm,
      List<ProgrammeMembership> candidatePms) {
    return
        candidatePms.stream()
            .filter(pm -> {
              if (pm.getStartDate() == null || anchorPm.getStartDate() == null) {
                return false;
              } else {
                return pm.getStartDate().isBefore(anchorPm.getStartDate());
              }
            })
            .filter(pm -> {
              if (pm.getEndDate() == null) {
                return false;
              } else {
                return pm.getEndDate()
                    .isAfter(anchorPm.getStartDate().minusDays(PROGRAMME_BREAK_DAYS));
              }
            }).toList();
  }

  /**
   * Retrieve the full list of contacts for a local office from Trainee Reference Service.
   *
   * @param localOfficeName The local office name.
   * @return The list of contacts, or an empty list if there is an error.
   */
  protected List<Map<String, String>> getOwnerContactList(String localOfficeName) {
    if (localOfficeName != null) {
      try {
        List<Map<String, String>> ownerContactList
            = restTemplate.getForObject(referenceUrl + API_GET_OWNER_CONTACT,
            List.class, Map.of(OWNER_FIELD, localOfficeName));
        return ownerContactList == null ? new ArrayList<>() : ownerContactList;
      } catch (RestClientException rce) {
        log.warn("Exception occurred when requesting reference local-office-contact-by-lo-name "
            + "endpoint: " + rce);
      }
    }
    return new ArrayList<>();
  }

  /**
   * Get specified owner contact from a list of contacts.
   *
   * @param ownerContactList    The owner contact list to search.
   * @param contactType         The contact type to return.
   * @param fallbackContactType if the contactType is not available, return this contactType
   *                            instead.
   * @param defaultMessage      The default message if the contact was not found.
   * @return The specific contact of the owner, or the default message if not found.
   */
  protected String getOwnerContact(List<Map<String, String>> ownerContactList,
      LocalOfficeContactType contactType, LocalOfficeContactType fallbackContactType,
      String defaultMessage) {

    Optional<Map<String, String>> ownerContact = ownerContactList.stream()
        .filter(c ->
            c.get(CONTACT_TYPE_FIELD).equalsIgnoreCase(contactType.getContactTypeName()))
        .findFirst();
    if (ownerContact.isEmpty() && fallbackContactType != null) {
      ownerContact = ownerContactList.stream()
          .filter(c ->
              c.get(CONTACT_TYPE_FIELD)
                  .equalsIgnoreCase(fallbackContactType.getContactTypeName()))
          .findFirst();
    }
    return ownerContact.map(oc -> oc.get(CONTACT_FIELD))
        .orElse(defaultMessage);
  }

  /**
   * Retrieve the contact of the specified type for the given local office. If the contact type is
   * not found then a fallback contact type will be sought, and a default message returned if
   * neither are present.
   *
   * @param localOfficeName     The local office to use.
   * @param contactType         The contact type to return.
   * @param fallbackContactType if the contactType is not available, return this contactType
   *                            instead.
   * @param defaultMessage      The default message if the contact was not found.
   * @return The specific contact of the local office, or the default message if not found.
   */
  public String getOwnerContact(String localOfficeName, LocalOfficeContactType contactType,
      LocalOfficeContactType fallbackContactType, String defaultMessage) {
    return getOwnerContact(
        getOwnerContactList(localOfficeName), contactType, fallbackContactType, defaultMessage);
  }

  /**
   * Return a href type for a contact. It is assumed to be either a URL or an email address. There
   * is minimal checking that it is a validly formatted email address.
   *
   * @param contact The contact string, expected to be either an email address or a URL.
   * @return "email" if it looks like an email address, "url" if it looks like a URL,
   *     and "NOT_HREF" otherwise.
   */
  protected String getHrefTypeForContact(String contact) {
    try {
      new URL(contact);
      return ABSOLUTE_URL.getHrefTypeName();
    } catch (MalformedURLException e) {
      if (contact.contains("@") && !contact.contains(" ")) {
        return PROTOCOL_EMAIL.getHrefTypeName();
      } else {
        return NON_HREF.getHrefTypeName();
      }
    }
  }
}
