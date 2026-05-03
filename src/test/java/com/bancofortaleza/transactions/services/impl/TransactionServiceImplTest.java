package com.bancofortaleza.transactions.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.configuration.SupportHeadersProvider;
import com.bff.services.client.SupportApiClient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    private static final String DEVICE_IP = "192.168.1.10";
    private static final String SESSION = "7f2c1a54-4cf0-4d8c-9b18-2a6a57e7f7f3";
    private static final Integer USER_ID = 42;

    @Mock
    private SupportApiClient supportApiClient;

    @Mock
    private SupportHeadersProvider supportHeadersProvider;

    @InjectMocks
    private TransactionServiceImpl service;

    @Test
    void createTransactionShouldMapRequestHeadersAndResponse() {
        // Arrange
        com.bff.services.server.models.TransactionCreateRequest request =
                new com.bff.services.server.models.TransactionCreateRequest()
                        .idAccount(5)
                        .amount(new BigDecimal("250.75"))
                        .description("Salary payment")
                        .concept(com.bff.services.server.models.ConceptTransaction.CREDIT)
                        .status(com.bff.services.server.models.Status.ACTIVE);

        HttpHeaders downstreamHeaders = new HttpHeaders();
        downstreamHeaders.add("x-trace-id", "abc");
        downstreamHeaders.add(HttpHeaders.CONNECTION, "keep-alive");

        when(supportHeadersProvider.getAuthenticatedUserId()).thenReturn(USER_ID);
        when(supportApiClient.createTransaction(any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(clientTransaction(), downstreamHeaders, HttpStatus.CREATED));

        // Act
        ResponseEntity<com.bff.services.server.models.TransactionResponse> response =
                service.createTransaction(DEVICE_IP, SESSION, request);

        // Assert
        ArgumentCaptor<com.bff.services.client.models.TransactionCreateRequest> captor =
                ArgumentCaptor.forClass(com.bff.services.client.models.TransactionCreateRequest.class);
        verify(supportApiClient).createTransaction(eq(DEVICE_IP), eq(SESSION), eq(USER_ID), captor.capture());

        com.bff.services.client.models.TransactionCreateRequest clientRequest = captor.getValue();
        assertThat(clientRequest.getIdAccount()).isEqualTo(5);
        assertThat(clientRequest.getAmount()).isEqualByComparingTo("250.75");
        assertThat(clientRequest.getDescription()).isEqualTo("Salary payment");
        assertThat(clientRequest.getConcept()).isEqualTo(com.bff.services.client.models.ConceptTransaction.CREDIT);
        assertThat(clientRequest.getStatus()).isEqualTo(com.bff.services.client.models.Status.ACTIVE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst("x-trace-id")).isEqualTo("abc");
        assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.CONNECTION);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(10);
        assertThat(response.getBody().getConcept()).isEqualTo(com.bff.services.server.models.ConceptTransaction.DEBIT);
        assertThat(response.getBody().getStatus()).isEqualTo(com.bff.services.server.models.Status.ACTIVE);
    }

    @Test
    void createTransactionShouldAllowNullRequestAndNullResponseBody() {
        // Arrange
        when(supportHeadersProvider.getAuthenticatedUserId()).thenReturn(USER_ID);
        when(supportApiClient.createTransaction(DEVICE_IP, SESSION, USER_ID, null))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).build());

        // Act
        ResponseEntity<com.bff.services.server.models.TransactionResponse> response =
                service.createTransaction(DEVICE_IP, SESSION, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getTransactionByIdShouldDelegateAuthenticatedUserAndMapResponse() {
        // Arrange
        when(supportHeadersProvider.getAuthenticatedUserId()).thenReturn(USER_ID);
        when(supportApiClient.getTransactionById(DEVICE_IP, SESSION, USER_ID, 10))
                .thenReturn(ResponseEntity.ok(clientTransaction()));

        // Act
        ResponseEntity<com.bff.services.server.models.TransactionResponse> response =
                service.getTransactionById(DEVICE_IP, SESSION, 10);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(10);
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("99.99");
    }

    @Test
    void getTransactionByIdShouldMapNullConceptAndStatus() {
        // Arrange
        com.bff.services.client.models.TransactionResponse downstreamResponse =
                new com.bff.services.client.models.TransactionResponse()
                        .id(10)
                        .amount(new BigDecimal("99.99"))
                        .description("No enums");

        when(supportHeadersProvider.getAuthenticatedUserId()).thenReturn(USER_ID);
        when(supportApiClient.getTransactionById(DEVICE_IP, SESSION, USER_ID, 10))
                .thenReturn(ResponseEntity.ok(downstreamResponse));

        // Act
        ResponseEntity<com.bff.services.server.models.TransactionResponse> response =
                service.getTransactionById(DEVICE_IP, SESSION, 10);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getConcept()).isNull();
        assertThat(response.getBody().getStatus()).isNull();
    }

    @Test
    void listTransactionsShouldMapFiltersAndBody() {
        // Arrange
        when(supportHeadersProvider.getAuthenticatedUserId()).thenReturn(USER_ID);
        when(supportApiClient.listTransactions(
                DEVICE_IP,
                SESSION,
                USER_ID,
                1,
                20,
                "salary",
                5,
                com.bff.services.client.models.ConceptTransaction.DEBIT,
                com.bff.services.client.models.Status.ACTIVE
        )).thenReturn(ResponseEntity.ok(List.of(clientTransaction())));

        // Act
        ResponseEntity<List<com.bff.services.server.models.TransactionResponse>> response =
                service.listTransactions(
                        DEVICE_IP,
                        SESSION,
                        1,
                        20,
                        "salary",
                        5,
                        com.bff.services.server.models.ConceptTransaction.DEBIT,
                        com.bff.services.server.models.Status.ACTIVE
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getId()).isEqualTo(10);
    }

    @Test
    void listTransactionsShouldReturnNullBodyWhenDownstreamBodyIsNull() {
        // Arrange
        when(supportHeadersProvider.getAuthenticatedUserId()).thenReturn(USER_ID);
        when(supportApiClient.listTransactions(DEVICE_IP, SESSION, USER_ID, null, null, null, null, null, null))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).body(null));

        // Act
        ResponseEntity<List<com.bff.services.server.models.TransactionResponse>> response =
                service.listTransactions(DEVICE_IP, SESSION, null, null, null, null, null, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void updateTransactionStatusShouldMapRequestAndResponse() {
        // Arrange
        com.bff.services.server.models.TransactionStatusUpdateRequest request =
                new com.bff.services.server.models.TransactionStatusUpdateRequest()
                        .status(com.bff.services.server.models.Status.INACTIVE);

        when(supportHeadersProvider.getAuthenticatedUserId()).thenReturn(USER_ID);
        when(supportApiClient.updateTransactionStatus(any(), any(), any(), any(), any()))
                .thenReturn(ResponseEntity.ok(clientTransaction()));

        // Act
        ResponseEntity<com.bff.services.server.models.TransactionResponse> response =
                service.updateTransactionStatus(DEVICE_IP, SESSION, 10, request);

        // Assert
        ArgumentCaptor<com.bff.services.client.models.TransactionStatusUpdateRequest> captor =
                ArgumentCaptor.forClass(com.bff.services.client.models.TransactionStatusUpdateRequest.class);
        verify(supportApiClient).updateTransactionStatus(eq(DEVICE_IP), eq(SESSION), eq(USER_ID), eq(10), captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(com.bff.services.client.models.Status.INACTIVE);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-05-01T16:40:00Z"));
    }

    @Test
    void updateTransactionStatusShouldAllowNullRequest() {
        // Arrange
        when(supportHeadersProvider.getAuthenticatedUserId()).thenReturn(USER_ID);
        when(supportApiClient.updateTransactionStatus(DEVICE_IP, SESSION, USER_ID, 10, null))
                .thenReturn(ResponseEntity.ok(clientTransaction()));

        // Act
        ResponseEntity<com.bff.services.server.models.TransactionResponse> response =
                service.updateTransactionStatus(DEVICE_IP, SESSION, 10, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private com.bff.services.client.models.TransactionResponse clientTransaction() {
        return new com.bff.services.client.models.TransactionResponse()
                .id(10)
                .idAccount(5)
                .amount(new BigDecimal("99.99"))
                .description("Debit payment")
                .concept(com.bff.services.client.models.ConceptTransaction.DEBIT)
                .status(com.bff.services.client.models.Status.ACTIVE)
                .createdAt(OffsetDateTime.parse("2026-05-01T16:35:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-05-01T16:40:00Z"));
    }
}
