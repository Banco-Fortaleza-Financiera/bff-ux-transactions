package com.bancofortaleza.transactions.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.domain.exceptions.ApiException;
import com.bff.services.auth.AuthApiClient;
import com.bff.services.auth.models.TokenValidationRequest;
import com.bff.services.auth.models.TokenValidationResponse;
import feign.FeignException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TokenValidationServiceImplTest {

    private static final String DEVICE_IP = "192.168.1.10";
    private static final String SESSION = "7f2c1a54-4cf0-4d8c-9b18-2a6a57e7f7f3";

    @Mock
    private AuthApiClient authApiClient;

    @InjectMocks
    private TokenValidationServiceImpl service;

    @Test
    void validateShouldReturnUserIdWhenBearerTokenIsValid() {
        // Arrange
        when(authApiClient.validateToken(eq(DEVICE_IP), eq(SESSION), org.mockito.ArgumentMatchers.any()))
                .thenReturn(ResponseEntity.ok(new TokenValidationResponse().valid(true).idUser(15)));

        // Act
        Integer userId = service.validate("Bearer access-token", DEVICE_IP, SESSION);

        // Assert
        ArgumentCaptor<TokenValidationRequest> captor = ArgumentCaptor.forClass(TokenValidationRequest.class);
        verify(authApiClient).validateToken(eq(DEVICE_IP), eq(SESSION), captor.capture());

        assertThat(userId).isEqualTo(15);
        assertThat(captor.getValue().getTokenType()).isEqualTo("Bearer");
        assertThat(captor.getValue().getAccessToken()).isEqualTo("access-token");
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorizationHeaders")
    void validateShouldThrowUnauthorizedWhenAuthorizationHeaderIsInvalid(String authorizationHeader) {
        // Act / Assert
        assertThatThrownBy(() -> service.validate(authorizationHeader, DEVICE_IP, SESSION))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getCode()).isEqualTo("UNAUTHORIZED");
                    assertThat(exception.getMessage()).isEqualTo("No tiene autorizacion");
                });
    }

    @ParameterizedTest
    @MethodSource("invalidTokenResponses")
    void validateShouldThrowUnauthorizedWhenAuthServiceResponseIsInvalid(TokenValidationResponse response) {
        // Arrange
        when(authApiClient.validateToken(eq(DEVICE_IP), eq(SESSION), org.mockito.ArgumentMatchers.any()))
                .thenReturn(ResponseEntity.ok(response));

        // Act / Assert
        assertThatThrownBy(() -> service.validate("Bearer access-token", DEVICE_IP, SESSION))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getCode()).isEqualTo("UNAUTHORIZED");
                });
    }

    @Test
    void validateShouldThrowUnauthorizedWhenAuthServiceReturnsUnauthorized() {
        // Arrange
        when(authApiClient.validateToken(eq(DEVICE_IP), eq(SESSION), org.mockito.ArgumentMatchers.any()))
                .thenThrow(mock(FeignException.Unauthorized.class));

        // Act / Assert
        assertThatThrownBy(() -> service.validate("Bearer access-token", DEVICE_IP, SESSION))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getCode()).isEqualTo("UNAUTHORIZED");
                });
    }

    @Test
    void validateShouldThrowServiceUnavailableWhenAuthServiceFails() {
        // Arrange
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(503);
        when(authApiClient.validateToken(eq(DEVICE_IP), eq(SESSION), org.mockito.ArgumentMatchers.any()))
                .thenThrow(exception);

        // Act / Assert
        assertThatThrownBy(() -> service.validate("Bearer access-token", DEVICE_IP, SESSION))
                .isInstanceOfSatisfying(ApiException.class, apiException -> {
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("TOKEN_VALIDATION_UNAVAILABLE");
                    assertThat(apiException.getMessage())
                            .isEqualTo("La validacion del token no esta disponible por el momento");
                });
    }

    private static Stream<String> invalidAuthorizationHeaders() {
        return Stream.of(null, "", "   ", "Basic token", "Bearer", "Bearer ");
    }

    private static Stream<TokenValidationResponse> invalidTokenResponses() {
        return Stream.of(
                null,
                new TokenValidationResponse().valid(false).idUser(15),
                new TokenValidationResponse().valid(true),
                new TokenValidationResponse().idUser(15)
        );
    }
}
