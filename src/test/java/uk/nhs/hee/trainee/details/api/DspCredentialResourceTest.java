/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.trainee.details.TestJwtUtil;
import uk.nhs.hee.trainee.details.config.DspConfigurationProperties;
import uk.nhs.hee.trainee.details.mapper.PlacementMapper;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;
import uk.nhs.hee.trainee.details.model.dsp.IssueTokenResponse;
import uk.nhs.hee.trainee.details.model.dsp.ParResponse;
import uk.nhs.hee.trainee.details.service.JwtService;
import uk.nhs.hee.trainee.details.service.PlacementService;
import uk.nhs.hee.trainee.details.service.ProgrammeMembershipService;

@ContextConfiguration(classes = {PlacementMapper.class, ProgrammeMembership.class})
@WebMvcTest(DspCredentialResource.class)
class DspCredentialResourceTest {

  private static final String CLIENT_ID = "11111111-2222-3333-4444-555555555555";
  private static final String CLIENT_SECRET = "636c69656e745f736563726574";
  private static final String REDIRECT_URI = "https://redirect.uri";
  private static final String PAR_RESPONSE_REQUEST_URI = "https://par-response/request-uri";

  private static final String VALID_STATE = "someString";
  private static final String INVALID_STATE = "invalid state";
  private static final String VALID_CODE = "valid code";
  private static final String INVALID_CODE = "invalid code";
  private static final String VALID_TOKEN = "valid token";
  private static final String PAYLOAD = "payload";

  @Autowired
  private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @MockBean
  private JwtService jwtService;
  @MockBean
  private PlacementService placementService;
  @MockBean
  private ProgrammeMembershipService programmeMembershipService;
  private RestTemplate restTemplate;

  private DspConfigurationProperties dspConfigurationProperties;

  @BeforeEach
  void setUp() {
    RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
    restTemplate = mock(RestTemplate.class);

    when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
    when(restTemplateBuilder.build()).thenReturn(restTemplate);

    dspConfigurationProperties = new DspConfigurationProperties();
    dspConfigurationProperties.setClientId(CLIENT_ID);
    dspConfigurationProperties.setClientSecret(CLIENT_SECRET);
    dspConfigurationProperties.setRedirectUri(REDIRECT_URI);
    dspConfigurationProperties.setParEndpoint("https://test/issuing/par");
    dspConfigurationProperties.setAuthorizeEndpoint("https://test/issuing/authorize");
    dspConfigurationProperties.setTokenAudience("");
    dspConfigurationProperties.setTokenIssuer("");
    dspConfigurationProperties.setTokenSigningKey("");
    dspConfigurationProperties.setTokenIssueEndpoint("https://test/issuing/token");

    DspCredentialResource resource
        = new DspCredentialResource(placementService, programmeMembershipService,
        objectMapper, restTemplateBuilder, jwtService, dspConfigurationProperties);
    mockMvc = MockMvcBuilders.standaloneSetup(resource)
        .build();
  }

  @Test
  void shouldReturnErrorWhenTokenPayloadNotMap() throws Exception {
    String token = TestJwtUtil.generateToken("[]");

    mockMvc.perform(get("/api/credential/par/placement/{placementTisId}", 140)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());

    Mockito.verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest
  @ValueSource(strings = {"placement", "programmemembership"})
  void shouldReturnErrorWhenTraineeDataNotFound(String credentialType) throws Exception {
    when(placementService.getPlacementForTrainee("40", "140"))
        .thenReturn(Optional.empty());
    when(programmeMembershipService.getProgrammeMembershipForTrainee("40", "140"))
        .thenReturn(Optional.empty());
    String token = TestJwtUtil.generateTokenForTisId("40");

    mockMvc.perform(
        get("/api/credential/par/{credentialType}/{placementTisId}", credentialType, 140)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isUnprocessableEntity());

    Mockito.verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest
  @ValueSource(strings = {"placement", "programmemembership"})
  void shouldPostSuitableParRequestWhenTraineeDataFound(String credentialType) throws Exception {
    Placement placement = new Placement();
    placement.setTisId("140");
    // TODO: add fields to be used in token
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("140");
    // TODO: add fields to be used in token
    when(placementService.getPlacementForTrainee("40", "140"))
        .thenReturn(Optional.of(placement));
    when(programmeMembershipService.getProgrammeMembershipForTrainee("40", "140"))
        .thenReturn(Optional.of(programmeMembership));

    ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> httpEntityCaptor
        = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(any(URI.class), httpEntityCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(new ParResponse()));

    String token = TestJwtUtil.generateTokenForTisId("40");

    mockMvc.perform(
        get("/api/credential/par/{credentialType}/{placementTisId}", credentialType, 140)
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, token));

    HttpEntity<MultiValueMap<String, String>> httpEntity = httpEntityCaptor.getValue();
    Map<String, String> requestBody = httpEntity.getBody().toSingleValueMap();
    assertThat("Unexpected client id.", requestBody.get("client_id"),
        is(dspConfigurationProperties.getClientId()));
    assertThat("Unexpected client secret.", requestBody.get("client_secret"),
        is(dspConfigurationProperties.getClientSecret()));
    assertThat("Unexpected redirect uri.", requestBody.get("redirect_uri"),
        is(dspConfigurationProperties.getRedirectUri()));
    assertThat("Unexpected scope.", requestBody.get("scope"),
        is("issue.TestCredential"));

    if (credentialType.equals("placement")) {
      String placementJwt = jwtService.generatePlacementToken(placement);
      assertThat("Unexpected placement data.",
          requestBody.get("id_token_hint"), is(placementJwt));
    } else if (credentialType.equals("programmemembership")) {
      String programmeMembershipJwt
          = jwtService.generateProgrammeMembershipToken(programmeMembership);
      assertThat("Unexpected programme membership data.",
          requestBody.get("id_token_hint"), is(programmeMembershipJwt));
    }
  }

  @Test
  void shouldReturnErrorWhenGatewayErrors() throws Exception {
    Placement placement = new Placement();
    placement.setTisId("140");
    when(placementService.getPlacementForTrainee("40", "140"))
        .thenReturn(Optional.of(placement));

    ResponseEntity<ParResponse> parResponseEntity = ResponseEntity.badRequest().build();
    when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class),
        eq(ParResponse.class))).thenReturn(parResponseEntity);

    String token = TestJwtUtil.generateTokenForTisId("40");

    mockMvc.perform(get("/api/credential/par/placement/{placementTisId}", 140)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void shouldReturnErrorWhenGatewayReturnsEmptyBody() throws Exception {
    Placement placement = new Placement();
    placement.setTisId("140");
    when(placementService.getPlacementForTrainee("40", "140"))
        .thenReturn(Optional.of(placement));

    ResponseEntity<ParResponse> parResponseEntity = ResponseEntity.created(URI.create("")).build();
    when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class),
        eq(ParResponse.class))).thenReturn(parResponseEntity);

    String token = TestJwtUtil.generateTokenForTisId("40");

    mockMvc.perform(get("/api/credential/par/placement/{placementTisId}", 140)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isInternalServerError());
  }

  @ParameterizedTest
  @ValueSource(strings = {"placement", "programmemembership"})
  void shouldReturnAuthorizeRequestUriAsRedirectWhenTraineeDataFound(String credentialType)
      throws Exception {
    Placement placement = new Placement();
    placement.setTisId("140");
    when(placementService.getPlacementForTrainee("40", "140"))
        .thenReturn(Optional.of(placement));

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId("140");
    when(programmeMembershipService.getProgrammeMembershipForTrainee("40", "140"))
        .thenReturn(Optional.of(programmeMembership));

    ParResponse parResponse = new ParResponse();
    parResponse.setRequestUri(dspConfigurationProperties.getParEndpoint());
    ResponseEntity<ParResponse> parResponseEntity = ResponseEntity.created(URI.create(""))
        .body(parResponse);
    when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class),
        eq(ParResponse.class))).thenReturn(parResponseEntity);

    String token = TestJwtUtil.generateTokenForTisId("40");

    mockMvc.perform(
        get("/api/credential/par/{credentialType}/{placementTisId}", credentialType, 140)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION,
            String.format("https://test/issuing/authorize?client_id=%s&request_uri=%s",
                dspConfigurationProperties.getClientId(),
                dspConfigurationProperties.getParEndpoint())));
  }

  @Test
  void shouldReturnBadRequestForPayloadWithInvalidState() throws Exception {
    String authToken = TestJwtUtil.generateTokenForTisId("40");

    mockMvc.perform(
        get("/api/credential/payload/?code={VALID_CODE}&state={INVALID_STATE}",
            VALID_CODE, INVALID_STATE)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, authToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldThrowBadRequestWhenInvalidOrExpiredCodeUsed() throws Exception {
    String authToken = TestJwtUtil.generateTokenForTisId("40");

    ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> httpEntityCaptor
        = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(any(URI.class), httpEntityCaptor.capture(),
        eq(IssueTokenResponse.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

    mockMvc.perform(
        get("/api/credential/payload/?code={INVALID_CODE}&state={VALID_STATE}",
            INVALID_CODE, VALID_STATE)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, authToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldThrowInternalServerErrorWhenValidRequestButIdTokenIsNull() throws Exception {
    String authToken = TestJwtUtil.generateTokenForTisId("40");
    IssueTokenResponse credentialToken = new IssueTokenResponse();

    ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> httpEntityCaptor
        = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(any(URI.class), httpEntityCaptor.capture(),
        eq(IssueTokenResponse.class)))
        .thenReturn(ResponseEntity.ok(credentialToken));

    mockMvc.perform(
        get("/api/credential/payload/?code={VALID_CODE}&state={VALID_STATE}",
            VALID_CODE, VALID_STATE)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, authToken))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void shouldReturnPayloadWhenValidRequestWithIdToken() throws Exception {
    IssueTokenResponse credentialToken = new IssueTokenResponse();
    credentialToken.setIdToken(VALID_TOKEN);
    String authToken = TestJwtUtil.generateTokenForTisId("40");

    ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> httpEntityCaptor
        = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(any(URI.class), httpEntityCaptor.capture(),
        eq(IssueTokenResponse.class)))
        .thenReturn(ResponseEntity.ok(credentialToken));

    when(jwtService.getTokenPayload(VALID_TOKEN, false)).thenReturn(PAYLOAD);

    mockMvc.perform(
        get("/api/credential/payload/?code={VALID_CODE}&state={VALID_STATE}",
            VALID_CODE, VALID_STATE)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, authToken))
        .andExpect(status().isOk())
        .andExpect(content().string(PAYLOAD));
  }
}
