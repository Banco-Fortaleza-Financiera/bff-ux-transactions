package com.bancofortaleza.transactions.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Constructor;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AppLoggerTest {

    @AfterEach
    void tearDown() {
        AppLogger.clearContext();
    }

    @Test
    void contextMethodsShouldStoreReadRemoveAndClearValues() {
        // Arrange / Act
        AppLogger.putContext(AppLogger.REQUEST_ID, "request-1");
        AppLogger.putContext(AppLogger.METHOD, "");
        AppLogger.putContext(AppLogger.DEVICE_IP, null);
        AppLogger.putContext(Map.of(AppLogger.PATH, "/transactions", AppLogger.DEVICE_IP, "192.168.1.10"));

        // Assert
        assertThat(AppLogger.getContext(AppLogger.REQUEST_ID)).isEqualTo("request-1");
        assertThat(AppLogger.getContext(AppLogger.METHOD)).isNull();
        assertThat(AppLogger.getContext(AppLogger.PATH)).isEqualTo("/transactions");

        AppLogger.removeContext(AppLogger.REQUEST_ID);
        assertThat(AppLogger.getContext(AppLogger.REQUEST_ID)).isNull();

        AppLogger.clearContext();
        assertThat(AppLogger.getContext(AppLogger.PATH)).isNull();
    }

    @Test
    void loggingMethodsShouldBeCallableWithoutThrowing() {
        // Act / Assert
        assertThatCode(() -> {
            RuntimeException exception = new RuntimeException("boom");
            AppLogger.trace(AppLoggerTest.class, "trace {}", 1);
            AppLogger.debug(AppLoggerTest.class, "debug {}", 1);
            AppLogger.info(AppLoggerTest.class, "info {}", 1);
            AppLogger.warn(AppLoggerTest.class, "warn {}", 1);
            AppLogger.warn(AppLoggerTest.class, "warn throwable", exception);
            AppLogger.error(AppLoggerTest.class, "error {}", 1);
            AppLogger.error(AppLoggerTest.class, "error throwable", exception);
            AppLogger.requestStarted(AppLoggerTest.class, "GET", "/transactions");
            AppLogger.requestCompleted(AppLoggerTest.class, "GET", "/transactions", 200, 10);
            AppLogger.requestFailed(AppLoggerTest.class, "GET", "/transactions", 10, exception);
        }).doesNotThrowAnyException();
    }

    @Test
    void constructorShouldRemainPrivateUtilityConstructor() throws Exception {
        // Arrange
        Constructor<AppLogger> constructor = AppLogger.class.getDeclaredConstructor();

        // Act
        constructor.setAccessible(true);

        // Assert
        assertThatCode(constructor::newInstance).doesNotThrowAnyException();
    }
}
