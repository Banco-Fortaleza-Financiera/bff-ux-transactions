package com.bancofortaleza.transactions.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.domain.exceptions.ApiException;
import com.bancofortaleza.transactions.domain.model.ErrorResponse;
import feign.FeignException;
import feign.Request;
import feign.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleFeignExceptionShouldPreserveValidStatusBodyAndAllowedHeaders() {
        // Arrange
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("x-error-id", List.of("err-1"));
        headers.put(HttpHeaders.CONNECTION, List.of("keep-alive"));
        FeignException exception = feignException(409, headers, "conflict");

        // Act
        ResponseEntity<byte[]> response = handler.handleFeignException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("conflict".getBytes(StandardCharsets.UTF_8));
        assertThat(response.getHeaders().getFirst("x-error-id")).isEqualTo("err-1");
        assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.CONNECTION);
    }

    @Test
    void handleFeignExceptionShouldReturnBadGatewayWhenStatusIsInvalid() {
        // Arrange
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(0);

        // Act
        ResponseEntity<byte[]> response = handler.handleFeignException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void handleFeignExceptionShouldReturnBadGatewayWhenStatusIsTooHigh() {
        // Arrange
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(600);

        // Act
        ResponseEntity<byte[]> response = handler.handleFeignException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void handleFeignExceptionShouldIgnoreNullHeaderNamesAndValues() {
        // Arrange
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put(null, List.of("ignored"));
        headers.put("x-null-values", null);

        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(500);
        when(exception.responseHeaders()).thenReturn(headers);
        when(exception.content()).thenReturn("error".getBytes(StandardCharsets.UTF_8));

        // Act
        ResponseEntity<byte[]> response = handler.handleFeignException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders()).doesNotContainKey("x-null-values");
    }

    @Test
    void handleApiExceptionShouldReturnConfiguredErrorResponse() {
        // Act
        ResponseEntity<ErrorResponse> response = handler.handleApiException(
                new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "No tiene autorizacion"),
                request("/channel/v1/transactions")
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().path()).isEqualTo("/channel/v1/transactions");
        assertThat(response.getBody().details()).isEmpty();
    }

    @Test
    void handleValidationShouldReturnFieldErrorsWithPublicHeaderNames() throws NoSuchMethodException {
        // Arrange
        Method method = SampleController.class.getDeclaredMethod("create", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "xDeviceIp", "must not be blank"));
        bindingResult.addError(new FieldError("request", "amount", "must be positive"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception, request("/validation"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().details())
                .containsExactly(
                        new ErrorResponse.FieldError("x-device-ip", "must not be blank"),
                        new ErrorResponse.FieldError("amount", "must be positive")
                );
    }

    @Test
    void handleConstraintViolationShouldReturnSanitizedFieldErrors() {
        // Arrange
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("listTransactions.xSession");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(exception, request("/constraints"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details())
                .containsExactly(new ErrorResponse.FieldError("x-session", "must not be blank"));
    }

    @Test
    void handleBadRequestShouldReturnGenericBadRequest() {
        // Act
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                new HttpMessageNotReadableException("invalid json"),
                request("/bad-request")
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void handleTypeMismatchShouldReturnGenericBadRequest() {
        // Arrange
        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "id", null, new NumberFormatException());

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(exception, request("/bad-type"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid request");
    }

    @Test
    void handleMissingRequestParameterShouldReturnParameterDetail() {
        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMissingRequestParameter(
                new MissingServletRequestParameterException("idAccount", "Integer"),
                request("/missing-param")
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MISSING_REQUIRED_PARAMETER");
        assertThat(response.getBody().details())
                .containsExactly(new ErrorResponse.FieldError("idAccount", "Parameter is required"));
    }

    @Test
    void handleMissingRequestHeaderShouldReturnHeaderDetail() {
        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMissingRequestHeader(
                new MissingRequestHeaderException("x-session", null),
                request("/missing-header")
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MISSING_REQUIRED_HEADER");
        assertThat(response.getBody().details())
                .containsExactly(new ErrorResponse.FieldError("x-session", "Header is required"));
    }

    @Test
    void handleNotFoundShouldReturnNotFound() throws Exception {
        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new NoHandlerFoundException("GET", "/missing", new HttpHeaders()),
                request("/missing")
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void handleNoResourceFoundShouldReturnNotFound() {
        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/assets/missing.js"),
                request("/assets/missing.js")
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Resource not found");
    }

    @Test
    void handleMethodNotAllowedShouldReturnMethodNotAllowed() {
        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("POST", List.of("GET")),
                request("/method")
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void handleUnexpectedShouldReturnInternalServerError() {
        // Act
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("boom"),
                request("/unexpected")
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    private FeignException feignException(int status, Map<String, Collection<String>> headers, String body) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/downstream",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );

        Response response = Response.builder()
                .request(request)
                .status(status)
                .reason("downstream")
                .headers(headers)
                .body(body, StandardCharsets.UTF_8)
                .build();

        return FeignException.errorStatus("SupportApi#method", response);
    }

    private HttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }

    private static class SampleController {
        @SuppressWarnings("unused")
        void create(String body) {
        }
    }
}
