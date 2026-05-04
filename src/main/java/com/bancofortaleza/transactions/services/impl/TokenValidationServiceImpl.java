package com.bancofortaleza.transactions.services.impl;

import com.bancofortaleza.transactions.domain.exceptions.ApiException;
import com.bancofortaleza.transactions.services.TokenValidationService;
import com.bancofortaleza.transactions.utils.AppLogger;
import com.bff.services.auth.AuthApiClient;
import com.bff.services.auth.models.TokenValidationRequest;
import com.bff.services.auth.models.TokenValidationResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenValidationServiceImpl implements TokenValidationService {

    private static final String BEARER_TOKEN_TYPE = "Bearer";
    private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    private static final String UNAUTHORIZED_MESSAGE = "No tiene autorizacion";
    private static final String TOKEN_VALIDATION_UNAVAILABLE_CODE = "TOKEN_VALIDATION_UNAVAILABLE";
    private static final String TOKEN_VALIDATION_UNAVAILABLE_MESSAGE = "La validacion del token no esta disponible por el momento";

    private final AuthApiClient authApiClient;

    @Override
    public Integer validate(String authorizationHeader, String xDeviceIp, String xSession) {
        AuthorizationToken authorizationToken = resolveAuthorizationToken(authorizationHeader);

        try {
            TokenValidationRequest request = new TokenValidationRequest()
                    .accessToken(authorizationToken.accessToken())
                    .tokenType(authorizationToken.tokenType());

            ResponseEntity<TokenValidationResponse> response = authApiClient.validateToken(xDeviceIp, xSession, request);
            TokenValidationResponse body = response.getBody();

            if (body == null || !Boolean.TRUE.equals(body.getValid()) || body.getIdUser() == null) {
                throw unauthorized();
            }

            return body.getIdUser();
        } catch (FeignException.Unauthorized exception) {
            AppLogger.warn(TokenValidationServiceImpl.class, "Token validation rejected by auth service");
            throw unauthorized();
        } catch (FeignException exception) {
            AppLogger.warn(
                    TokenValidationServiceImpl.class,
                    "Token validation service unavailable status={}",
                    exception.status()
            );
            throw tokenValidationUnavailable();
        }
    }

    private AuthorizationToken resolveAuthorizationToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw unauthorized();
        }

        String[] parts = authorizationHeader.trim().split("\\s+", 2);
        if (parts.length != 2 || !BEARER_TOKEN_TYPE.equalsIgnoreCase(parts[0]) || parts[1].isBlank()) {
            throw unauthorized();
        }

        return new AuthorizationToken(BEARER_TOKEN_TYPE, parts[1]);
    }

    private ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE);
    }

    private ApiException tokenValidationUnavailable() {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                TOKEN_VALIDATION_UNAVAILABLE_CODE,
                TOKEN_VALIDATION_UNAVAILABLE_MESSAGE
        );
    }

    private record AuthorizationToken(String tokenType, String accessToken) {
    }
}
