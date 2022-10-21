/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.model.Placement;

@Slf4j
@Service
public class JwtService {

  private final Long JWT_EXPIRATION_MS = 31556926000L; //1 year in milliseconds
  private final String tokenIssuer;
  private final String tokenAudience;
  private final String tokenSigningKey;

  JwtService(@Value("${dsp.token.issuer}") String tokenIssuer,
             @Value("${dsp.token.audience}") String tokenAudience,
             @Value("${dsp.token.signing-key}") String tokenSigningKey) {
    this.tokenIssuer = tokenIssuer;
    this.tokenAudience = tokenAudience;
    this.tokenSigningKey = tokenSigningKey;
  }

  public String generatePlacementToken(Placement placement) {
    //TODO real values
    Map<String, Object> claims = new HashMap<>();
    claims.put("givenName", "Joe");
    claims.put("familyName", "Bloggs");
    claims.put("birthDate", "1980-08-21");

    return doGenerateToken(claims);
  }

  private String doGenerateToken(Map<String, Object> claims) {

    return Jwts.builder()
        .setClaims(claims)
        .setAudience(this.tokenAudience)
        .setIssuer(this.tokenIssuer)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
        .setNotBefore(new Date(System.currentTimeMillis()))
        .signWith(SignatureAlgorithm.HS256, this.tokenSigningKey)
        .compact();
  }

}
