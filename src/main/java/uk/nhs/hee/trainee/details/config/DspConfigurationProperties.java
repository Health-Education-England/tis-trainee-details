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

package uk.nhs.hee.trainee.details.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "dsp")
@Configuration
public class DspConfigurationProperties {
  private  String clientId;
  private  String clientSecret;
  private  String redirectUri;
  private  String parEndpoint;
  private  String authorizeEndpoint;
  private  String tokenIssuer;
  private  String tokenAudience;
  private  String tokenSigningKey;
  private  String tokenIssueEndpoint;

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public String getParEndpoint() {
    return parEndpoint;
  }

  public String getAuthorizeEndpoint() {
    return authorizeEndpoint;
  }

  public String getTokenIssuer() {
    return tokenIssuer;
  }

  public String getTokenAudience() {
    return tokenAudience;
  }

  public String getTokenSigningKey() {
    return tokenSigningKey;
  }

  public String getTokenIssueEndpoint() {
    return tokenIssueEndpoint;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public void setParEndpoint(String parEndpoint) {
    this.parEndpoint = parEndpoint;
  }

  public void setAuthorizeEndpoint(String authorizeEndpoint) {
    this.authorizeEndpoint = authorizeEndpoint;
  }

  public void setTokenIssuer(String tokenIssuer) {
    this.tokenIssuer = tokenIssuer;
  }

  public void setTokenAudience(String tokenAudience) {
    this.tokenAudience = tokenAudience;
  }

  public void setTokenSigningKey(String tokenSigningKey) {
    this.tokenSigningKey = tokenSigningKey;
  }

  public void setTokenIssueEndpoint(String tokenIssueEndpoint) {
    this.tokenIssueEndpoint = tokenIssueEndpoint;
  }
}
