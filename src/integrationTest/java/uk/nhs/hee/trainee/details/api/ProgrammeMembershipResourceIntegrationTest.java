/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester;
import com.openhtmltopdf.pdfboxout.visualtester.PdfVisualTester.PdfCompareResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.trainee.details.DockerImageNames;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.dto.ProgrammeMembershipDto;
import uk.nhs.hee.trainee.details.model.PersonalDetails;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.TraineeProfile;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
class ProgrammeMembershipResourceIntegrationTest {

  // Not ideal having a hardcoded path, but we want to be able to upload the results.
  private static final String TEST_OUTPUT_PATH = "build/reports/pdf-regression";

  private static final String TRAINEE_ID = UUID.randomUUID().toString();
  private static final String PM_ID = UUID.randomUUID().toString();

  private static final LocalDate START_DATE = LocalDate.EPOCH.minusDays(300);
  private static final LocalDate END_DATE = LocalDate.EPOCH.plusDays(300);

  @Container
  @ServiceConnection
  private static final MongoDBContainer mongoContainer = new MongoDBContainer(
      DockerImageNames.MONGO);

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MongoTemplate mongoTemplate;

  @MockBean
  private SqsTemplate sqsTemplate;

  @MockBean
  private RestTemplate restTemplate;

  @Autowired
  private MockMvc mockMvc;

  private MockedStatic<Clock> mockedClock;

  @BeforeEach
  void setUp(@Value("${application.timezone}") ZoneId zoneId) {
    Clock clock = Clock.fixed(Instant.EPOCH, zoneId);
    mockedClock = mockStatic(Clock.class,
        withSettings().defaultAnswer(InvocationOnMock::callRealMethod));
    mockedClock.when(() -> Clock.system(any())).thenAnswer(inv -> clock);
  }

  @AfterEach
  void tearDown() {
    mockedClock.close();
    mongoTemplate.findAllAndRemove(new Query(), TraineeProfile.class);
  }


  @Test
  void getShouldReturnBadRequestWhenTokenNotFound() throws Exception {
    mockMvc.perform(post("/api/programme-membership/{programmeMembershipId}/sign-coj", 0)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldReturnBadRequestWhenTokenNotMap() throws Exception {
    String token = TestJwtUtil.generateToken("[]");

    mockMvc.perform(post("/api/programme-membership/{programmeMembershipId}/sign-coj", 0)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getShouldReturnBadRequestWhenTisIdNotInToken() throws Exception {
    String token = TestJwtUtil.generateToken("{}");

    mockMvc.perform(post("/api/programme-membership/{programmeMembershipId}/sign-coj", 0)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequestWhenIdIsNull() throws Exception {
    mockMvc.perform(patch("/api/programme-membership/{traineeTisId}", 40)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(new ProgrammeMembershipDto())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequestWhenDownloadPdfTokenNotFound() throws Exception {
    mockMvc.perform(get("/api/programme-membership/{programmeMembershipId}/confirmation", 0)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequestWhenDownloadPdfTokenNotMap() throws Exception {
    String token = TestJwtUtil.generateToken("[]");

    mockMvc.perform(get("/api/programme-membership/{programmeMembershipId}/confirmation", 0)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnGenericPdfWhenProgrammeMembershipEmpty() throws Exception {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setTisId(PM_ID);
    pm.setStartDate(START_DATE);
    profile.setProgrammeMemberships(List.of(pm));

    mongoTemplate.save(profile);

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    MvcResult result = mockMvc.perform(
            get("/api/programme-membership/{programmeMembershipId}/confirmation", PM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andReturn();

    MockHttpServletResponse response = result.getResponse();
    byte[] responseBytes = response.getContentAsByteArray();

    int problems = compareGeneratedPdf("programme-confirmation-empty", responseBytes);
    assertThat("Unexpected PDF comparison problem count.", problems, is(0));
  }

  @Test
  void shouldReturnCustomizedPdfWhenProgrammeMembershipNotEmpty() throws Exception {
    TraineeProfile profile = new TraineeProfile();
    profile.setTraineeTisId(TRAINEE_ID);

    PersonalDetails personalDetails = new PersonalDetails();
    personalDetails.setForenames("Anthony Mara");
    personalDetails.setSurname("Gilliam");
    personalDetails.setGmcNumber("0000000");
    profile.setPersonalDetails(personalDetails);

    ProgrammeMembership pm = new ProgrammeMembership();
    pm.setTisId(PM_ID);
    pm.setStartDate(START_DATE);
    pm.setEndDate(END_DATE);
    pm.setProgrammeName("General Practice");
    pm.setManagingDeanery("North West");
    profile.setProgrammeMemberships(List.of(pm));

    mongoTemplate.save(profile);

    when(restTemplate.getForObject(anyString(), any(), anyMap())).thenReturn(List.of(
        Map.of(
            "contactTypeName", "Onboarding Support",
            "contact", "local.office@example.com"
        )
    ));

    String token = TestJwtUtil.generateTokenForTisId(TRAINEE_ID);
    MvcResult result = mockMvc.perform(
            get("/api/programme-membership/{programmeMembershipId}/confirmation", PM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk())
        .andReturn();

    MockHttpServletResponse response = result.getResponse();
    byte[] responseBytes = response.getContentAsByteArray();

    int problems = compareGeneratedPdf("programme-confirmation", responseBytes);
    assertThat("Unexpected PDF comparison problem count.", problems, is(0));
  }

  /**
   * Compare the bytes of a generated PDF against an existing example.
   *
   * @param resource          The filename of the existing PDF to check against, without extension.
   * @param generatedPdfBytes The generated bytes.
   * @return The number of discrepancies between the generation and expected results.
   * @throws IOException If output files could not be created, or reading the bytes fails.
   */
  private int compareGeneratedPdf(String resource, byte[] generatedPdfBytes) throws IOException {
    Files.createDirectories(Paths.get(TEST_OUTPUT_PATH));

    byte[] expectedPdfBytes;

    try (InputStream expectedIs = getClass().getResourceAsStream("/pdf/" + resource + ".pdf")) {
      assert expectedIs != null;
      expectedPdfBytes = IOUtils.toByteArray(expectedIs);
    }

    // Get a list of results.
    List<PdfCompareResult> problems = PdfVisualTester.comparePdfDocuments(expectedPdfBytes,
        generatedPdfBytes, resource, false);

    if (!problems.isEmpty()) {
      System.err.println("Found problems with test case (" + resource + "):");
      System.err.println(problems.stream()
          .map(p -> p.logMessage)
          .collect(Collectors.joining("\n    ", "[\n    ", "\n]")));

      System.err.println("For test case (" + resource + ") writing failure artefacts to '"
          + TEST_OUTPUT_PATH + "'");
      File generatedPdf = new File(TEST_OUTPUT_PATH, resource + "---actual.pdf");
      Files.write(generatedPdf.toPath(), generatedPdfBytes);
    }

    for (PdfCompareResult result : problems) {
      if (result.testImages != null) {
        File output = new File(TEST_OUTPUT_PATH,
            resource + "---" + result.pageNumber + "---diff.png");
        ImageIO.write(result.testImages.createDiff(), "png", output);

        output = new File(TEST_OUTPUT_PATH, resource + "---" + result.pageNumber + "---actual.png");
        ImageIO.write(result.testImages.getActual(), "png", output);

        output = new File(TEST_OUTPUT_PATH,
            resource + "---" + result.pageNumber + "---expected.png");
        ImageIO.write(result.testImages.getExpected(), "png", output);
      }
    }

    return problems.size();
  }
}
