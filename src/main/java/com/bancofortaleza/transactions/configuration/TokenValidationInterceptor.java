package com.bancofortaleza.transactions.configuration;

import com.bancofortaleza.transactions.services.TokenValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TokenValidationInterceptor implements HandlerInterceptor {

    public static final String AUTHENTICATED_USER_ID_ATTRIBUTE = "authenticatedUserId";

    private static final String DEVICE_IP_HEADER = "x-device-ip";
    private static final String SESSION_HEADER = "x-session";

    private final ObjectProvider<TokenValidationService> tokenValidationServiceProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String xDeviceIp = request.getHeader(DEVICE_IP_HEADER);
        String xSession = request.getHeader(SESSION_HEADER);

        if (isBlank(xDeviceIp) || isBlank(xSession)) {
            return true;
        }

        Integer authenticatedUserId = tokenValidationServiceProvider.getObject().validate(
                request.getHeader(HttpHeaders.AUTHORIZATION),
                xDeviceIp,
                xSession
        );
        request.setAttribute(AUTHENTICATED_USER_ID_ATTRIBUTE, authenticatedUserId);
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
