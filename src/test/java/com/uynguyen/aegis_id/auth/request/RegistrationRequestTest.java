package com.uynguyen.aegis_id.auth.request;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("RegistrationRequest Tests")
class RegistrationRequestTest {

    @Autowired
    private Validator validator;

    private RegistrationRequest createValidRequest() {
        return RegistrationRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("test@example.com")
            .phoneNumber("+33123456789")
            .password("Password1!")
            .confirmPassword("Password1!")
            .build();
    }

    /**
     * Helper method to check if a violation exists for a specific property.
     * This approach is i18n-safe as it doesn't rely on exact error messages.
     */
    private boolean hasViolationForProperty(
        Set<ConstraintViolation<RegistrationRequest>> violations,
        String propertyPath
    ) {
        return violations
            .stream()
            .anyMatch(v -> propertyPath.equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("Should create valid registration request")
    void testValidRegistrationRequest() {
        // Given
        RegistrationRequest request = createValidRequest();

        // When
        Set<ConstraintViolation<RegistrationRequest>> violations =
            validator.validate(request);

        // Then
        assertTrue(
            violations.isEmpty(),
            "Valid request should have no violations"
        );
    }

    @Nested
    @DisplayName("FirstName Validation Tests")
    class FirstNameTests {

        @Test
        @DisplayName("Should reject blank first name")
        void testFirstNameCannotBeBlank() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setFirstName("");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertFalse(violations.isEmpty());
            assertTrue(
                hasViolationForProperty(violations, "firstName"),
                "Should have violation for firstName property"
            );
        }

        @Test
        @DisplayName("Should reject null first name")
        void testFirstNameCannotBeNull() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setFirstName(null);

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "firstName"),
                "Should have violation for firstName property"
            );
        }

        @Test
        @DisplayName("Should reject first name exceeding max length")
        void testFirstNameMaxLength() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setFirstName("A".repeat(51));

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "firstName"),
                "Should have violation for firstName property"
            );
        }

        @Test
        @DisplayName("Should accept first name at max length")
        void testFirstNameAtMaxLength() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setFirstName("A".repeat(50));

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should reject first name with invalid characters")
        void testFirstNameInvalidPattern() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setFirstName("John123");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "firstName"),
                "Should have violation for firstName property"
            );
        }

        @ParameterizedTest
        @DisplayName(
            "Should accept first name with allowed punctuation/spacing"
        )
        @ValueSource(strings = { "O'Brien", "Jean-Pierre", "Mary Anne" })
        void testFirstNameWithAllowedCharacters(String firstName) {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setFirstName(firstName);

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("LastName Validation Tests")
    class LastNameTests {

        @Test
        @DisplayName("Should reject blank last name")
        void testLastNameCannotBeBlank() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setLastName("");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertFalse(violations.isEmpty());
            assertTrue(
                hasViolationForProperty(violations, "lastName"),
                "Should have violation for lastName property"
            );
        }

        @Test
        @DisplayName("Should accept null last name")
        void testLastNameCanBeNull() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setLastName(null);

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should reject last name exceeding max length")
        void testLastNameMaxLength() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setLastName("B".repeat(51));

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "lastName"),
                "Should have violation for lastName property"
            );
        }

        @Test
        @DisplayName("Should accept last name at max length")
        void testLastNameAtMaxLength() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setLastName("B".repeat(50));

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should reject last name with invalid characters")
        void testLastNameInvalidPattern() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setLastName("Doe123");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "lastName"),
                "Should have violation for lastName property"
            );
        }

        @Test
        @DisplayName("Should accept last name with hyphen")
        void testLastNameWithHyphen() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setLastName("Smith-Jones");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailTests {

        @ParameterizedTest
        @DisplayName("Should reject blank/null/invalid email")
        @NullAndEmptySource
        @ValueSource(strings = { "invalid-email" })
        void testEmailInvalidValues(String email) {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setEmail(email);

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "email"),
                "Should have violation for email property"
            );
        }

        @Test
        @DisplayName("Should reject email without proper domain")
        void testEmailWithoutDomain() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setEmail("testexample.com"); // Missing @

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept valid email")
        void testValidEmail() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setEmail("valid.email@example.com");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("PhoneNumber Validation Tests")
    class PhoneNumberTests {

        @Test
        @DisplayName("Should reject blank phone number")
        void testPhoneNumberCannotBeBlank() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPhoneNumber("");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertFalse(violations.isEmpty());
            assertTrue(
                hasViolationForProperty(violations, "phoneNumber"),
                "Should have violation for phoneNumber property"
            );
        }

        @Test
        @DisplayName("Should reject null phone number")
        void testPhoneNumberCannotBeNull() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPhoneNumber(null);

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "phoneNumber"),
                "Should have violation for phoneNumber property"
            );
        }

        @Test
        @DisplayName("Should reject invalid phone number format")
        void testPhoneNumberInvalidFormat() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPhoneNumber("abc123");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "phoneNumber"),
                "Should have violation for phoneNumber property"
            );
        }

        @Test
        @DisplayName("Should reject phone number starting with 0")
        void testPhoneNumberStartingWithZero() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPhoneNumber("0123456789");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertFalse(violations.isEmpty());
        }

        @ParameterizedTest
        @DisplayName("Should accept valid phone number formats")
        @ValueSource(
            strings = { "+33123456789", "33123456789", "+14155552671" }
        )
        void testValidPhoneNumberFormats(String phoneNumber) {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPhoneNumber(phoneNumber);

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordTests {

        @Test
        @DisplayName("Should reject blank password")
        void testPasswordCannotBeBlank() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPassword("");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertFalse(violations.isEmpty());
            assertTrue(
                hasViolationForProperty(violations, "password"),
                "Should have violation for password property"
            );
        }

        @Test
        @DisplayName("Should reject password longer than 72 characters")
        void testPasswordMaxLength() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPassword("Passw0rd!".repeat(10)); // 90 characters

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertFalse(violations.isEmpty());
            assertTrue(
                hasViolationForProperty(violations, "password"),
                "Should have violation for password property"
            );
        }

        @ParameterizedTest
        @DisplayName("Should reject null/short/weak password variants")
        @NullSource
        @ValueSource(
            strings = {
                "Pass1!", "password1!", "PASSWORD1!", "Password!", "Password1",
            }
        )
        void testInvalidPasswordVariants(String password) {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPassword(password);

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "password"),
                "Should have violation for password property"
            );
        }

        @Test
        @DisplayName("Should accept valid strong password")
        void testValidStrongPassword() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPassword("StrongP@ssw0rd");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName(
            "Should accept password at min length with all requirements"
        )
        void testPasswordAtMinLength() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPassword("Pass1!ab");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("ConfirmPassword Validation Tests")
    class ConfirmPasswordTests {

        @Test
        @DisplayName("Should reject blank confirm password")
        void testConfirmPasswordCannotBeBlank() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setConfirmPassword("");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertFalse(violations.isEmpty());
            assertTrue(
                hasViolationForProperty(violations, "confirmPassword"),
                "Should have violation for confirmPassword property"
            );
        }

        @Test
        @DisplayName("Should reject null confirm password")
        void testConfirmPasswordCannotBeNull() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setConfirmPassword(null);

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "confirmPassword"),
                "Should have violation for confirmPassword property"
            );
        }

        @Test
        @DisplayName("Should reject confirm password shorter than 8 characters")
        void testConfirmPasswordMinLength() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setConfirmPassword("Pass1!");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "confirmPassword"),
                "Should have violation for confirmPassword property"
            );
        }

        @Test
        @DisplayName("Should reject confirm password longer than 72 characters")
        void testConfirmPasswordMaxLength() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setConfirmPassword("P".repeat(73));

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertEquals(1, violations.size());
            assertTrue(
                hasViolationForProperty(violations, "confirmPassword"),
                "Should have violation for confirmPassword property"
            );
        }

        @Test
        @DisplayName("Should accept valid confirm password")
        void testValidConfirmPassword() {
            // Given
            RegistrationRequest request = createValidRequest();
            request.setPassword("Password1!");
            request.setConfirmPassword("Password1!");

            // When
            Set<ConstraintViolation<RegistrationRequest>> violations =
                validator.validate(request);

            // Then
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Lombok Features Tests")
    class LombokFeaturesTests {

        @Test
        @DisplayName("Should work with builder pattern")
        void testBuilderPattern() {
            // Given & When
            RegistrationRequest request = RegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .phoneNumber("+33123456789")
                .password("Password1!")
                .confirmPassword("Password1!")
                .build();

            // Then
            assertNotNull(request);
            assertEquals("John", request.getFirstName());
            assertEquals("Doe", request.getLastName());
            assertEquals("test@example.com", request.getEmail());
            assertEquals("+33123456789", request.getPhoneNumber());
            assertEquals("Password1!", request.getPassword());
            assertEquals("Password1!", request.getConfirmPassword());
        }

        @Test
        @DisplayName("Should work with no-args constructor")
        void testNoArgsConstructor() {
            // Given & When
            RegistrationRequest request = new RegistrationRequest();

            // Then
            assertNotNull(request);
            assertNull(request.getFirstName());
            assertNull(request.getLastName());
            assertNull(request.getEmail());
            assertNull(request.getPhoneNumber());
            assertNull(request.getPassword());
            assertNull(request.getConfirmPassword());
        }

        @Test
        @DisplayName("Should work with all-args constructor")
        void testAllArgsConstructor() {
            // Given & When
            RegistrationRequest request = new RegistrationRequest(
                "John",
                "Doe",
                "test@example.com",
                "+33123456789",
                "Password1!",
                "Password1!"
            );

            // Then
            assertNotNull(request);
            assertEquals("John", request.getFirstName());
            assertEquals("Doe", request.getLastName());
            assertEquals("test@example.com", request.getEmail());
            assertEquals("+33123456789", request.getPhoneNumber());
            assertEquals("Password1!", request.getPassword());
            assertEquals("Password1!", request.getConfirmPassword());
        }

        @Test
        @DisplayName("Should work with getters and setters")
        void testGettersAndSetters() {
            // Given
            RegistrationRequest request = new RegistrationRequest();

            // When
            request.setFirstName("Jane");
            request.setLastName("Smith");
            request.setEmail("jane@example.com");
            request.setPhoneNumber("+14155552671");
            request.setPassword("NewPassword1!");
            request.setConfirmPassword("NewPassword1!");

            // Then
            assertEquals("Jane", request.getFirstName());
            assertEquals("Smith", request.getLastName());
            assertEquals("jane@example.com", request.getEmail());
            assertEquals("+14155552671", request.getPhoneNumber());
            assertEquals("NewPassword1!", request.getPassword());
            assertEquals("NewPassword1!", request.getConfirmPassword());
        }
    }
}
