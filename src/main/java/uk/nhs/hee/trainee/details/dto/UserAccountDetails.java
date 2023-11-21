package uk.nhs.hee.trainee.details.dto;

/**
 * Account details for an individual user.
 *
 * @param email      The profile email.
 * @param familyName The profile family name.
 */
public record UserAccountDetails(String email, String familyName) {

}
