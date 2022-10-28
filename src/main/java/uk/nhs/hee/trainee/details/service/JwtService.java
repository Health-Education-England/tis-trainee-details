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

import static io.jsonwebtoken.SignatureAlgorithm.HS256;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.TextCodec;
import io.jsonwebtoken.impl.crypto.DefaultJwtSignatureValidator;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.trainee.details.model.Placement;
import uk.nhs.hee.trainee.details.model.ProgrammeMembership;

@Slf4j
@Service
public class JwtService {

  private static final Long JWT_EXPIRATION_MS = 31556926000L; //1 year in milliseconds
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

  /**
   * Generate a JWT token for a placement object.
   *
   * @param placement the placement to include in the JWT
   * @return the JWT token string
   */
  public String generatePlacementToken(Placement placement) {
    //TODO real values
    Map<String, Object> claims = new HashMap<>();
    claims.put("givenName", "Joe");
    claims.put("familyName", "Bloggs");
    claims.put("birthDate", "1980-08-21");

    return generateToken(claims);
  }

  /**
   * Generate a JWT token for a programme membership object.
   *
   * @param programmeMembership the programme membership to include in the JWT
   * @return the JWT token string
   */
  public String generateProgrammeMembershipToken(ProgrammeMembership programmeMembership) {
    //TODO real values
    Map<String, Object> claims = new HashMap<>();
    claims.put("givenName", "Joe");
    claims.put("familyName", "Bloggs");
    claims.put("birthDate", "1980-08-21");

    return generateToken(claims);
  }

  /**
   * Generate a JWT token containing the provided claims, using the standard header and signature.
   *
   * @param claims the claims to include in the JWT
   * @return the JWT token string
   */
  private String generateToken(Map<String, Object> claims) {

    return Jwts.builder()
        .setClaims(claims)
        .setAudience(this.tokenAudience)
        .setIssuer(this.tokenIssuer)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
        .setNotBefore(new Date(System.currentTimeMillis()))
        .signWith(HS256, this.tokenSigningKey)
        .compact();
  }

  /**
   * Retrieve the payload from a JWT token.
   *
   * @param jwtToken the JWT token to process
   * @param verifySignature whether to verify the signature using our signing key
   * @return the payload JSON string
   * @throws SignatureException if the token can not be verified
   */
  public String getTokenPayload(String jwtToken, boolean verifySignature)
      throws SignatureException {
    if (verifySignature && !canVerifyToken(jwtToken)) {
      throw new SignatureException("Could not verify JWT token integrity!");
    }

    String[] chunks = jwtToken.split("\\.");
    Base64.Decoder decoder = Base64.getUrlDecoder();

    return new String(decoder.decode(chunks[1]));
  }

  /**
   * Verify the integrity of a signed JWT token using our signing key.
   *
   * @param jwtToken the JWT token to verify
   * @return true if the token can be verified, otherwise false
   */
  public boolean canVerifyToken(String jwtToken) {
    String[] chunks = jwtToken.split("\\.");

    String tokenWithoutSignature = chunks[0] + "." + chunks[1];
    String signature = chunks[2];
    SignatureAlgorithm sa = HS256; //hardcoded here, but could be retrieved from header.alg

    byte[] keyBytes = TextCodec.BASE64.decode(this.tokenSigningKey);
    SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, sa.getJcaName());

    DefaultJwtSignatureValidator validator = new DefaultJwtSignatureValidator(sa, secretKeySpec);
    return validator.isValid(tokenWithoutSignature, signature);
  }
}
