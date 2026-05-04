package com.bancofortaleza.transactions.configuration;

import com.bancofortaleza.transactions.domain.exceptions.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportHeadersProvider {

    private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    private static final String UNAUTHORIZED_MESSAGE = "No tiene autorizacion";

    private final HttpServletRequest request;

    public Integer getAuthenticatedUserId() {
        Object value = request.getAttribute(TokenValidationInterceptor.AUTHENTICATED_USER_ID_ATTRIBUTE);

        if (value instanceof Integer authenticatedUserId) {
            return authenticatedUserId;
        }

        throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE);
    }
}
