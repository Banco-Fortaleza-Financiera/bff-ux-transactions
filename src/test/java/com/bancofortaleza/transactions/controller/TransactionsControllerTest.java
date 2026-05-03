package com.bancofortaleza.transactions.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.services.TransactionService;
import com.bff.services.server.models.AccountStatementReportResponse;
import com.bff.services.server.models.ConceptTransaction;
import com.bff.services.server.models.Status;
import com.bff.services.server.models.TransactionCreateRequest;
import com.bff.services.server.models.TransactionResponse;
import com.bff.services.server.models.TransactionStatusUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TransactionsControllerTest {

    private static final String DEVICE_IP = "192.168.1.10";
    private static final String SESSION = "7f2c1a54-4cf0-4d8c-9b18-2a6a57e7f7f3";

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionsController controller;

    @Test
    void createTransactionShouldDelegateToService() {
        // Arrange
        TransactionCreateRequest request = new TransactionCreateRequest();
        ResponseEntity<TransactionResponse> expected = ResponseEntity.ok(new TransactionResponse().id(1));
        when(transactionService.createTransaction(DEVICE_IP, SESSION, request)).thenReturn(expected);

        // Act
        ResponseEntity<TransactionResponse> response = controller.createTransaction(DEVICE_IP, SESSION, request);

        // Assert
        assertThat(response).isSameAs(expected);
        verify(transactionService).createTransaction(DEVICE_IP, SESSION, request);
    }

    @Test
    void getTransactionByIdShouldDelegateToService() {
        // Arrange
        ResponseEntity<TransactionResponse> expected = ResponseEntity.ok(new TransactionResponse().id(1));
        when(transactionService.getTransactionById(DEVICE_IP, SESSION, 1)).thenReturn(expected);

        // Act
        ResponseEntity<TransactionResponse> response = controller.getTransactionById(DEVICE_IP, SESSION, 1);

        // Assert
        assertThat(response).isSameAs(expected);
        verify(transactionService).getTransactionById(DEVICE_IP, SESSION, 1);
    }

    @Test
    void listTransactionsShouldDelegateToService() {
        // Arrange
        ResponseEntity<List<TransactionResponse>> expected = ResponseEntity.ok(List.of(new TransactionResponse().id(1)));
        when(transactionService.listTransactions(
                DEVICE_IP,
                SESSION,
                1,
                20,
                "salary",
                5,
                ConceptTransaction.CREDIT,
                Status.ACTIVE
        )).thenReturn(expected);

        // Act
        ResponseEntity<List<TransactionResponse>> response = controller.listTransactions(
                DEVICE_IP,
                SESSION,
                1,
                20,
                "salary",
                5,
                ConceptTransaction.CREDIT,
                Status.ACTIVE
        );

        // Assert
        assertThat(response).isSameAs(expected);
        verify(transactionService).listTransactions(
                DEVICE_IP,
                SESSION,
                1,
                20,
                "salary",
                5,
                ConceptTransaction.CREDIT,
                Status.ACTIVE
        );
    }

    @Test
    void generateAccountStatementReportShouldDelegateToService() {
        // Arrange
        LocalDate startDate = LocalDate.parse("2026-05-01");
        LocalDate endDate = LocalDate.parse("2026-05-31");
        ResponseEntity<AccountStatementReportResponse> expected =
                ResponseEntity.ok(new AccountStatementReportResponse().idUser(10));
        when(transactionService.generateAccountStatementReport(DEVICE_IP, SESSION, 10, startDate, endDate))
                .thenReturn(expected);

        // Act
        ResponseEntity<AccountStatementReportResponse> response =
                controller.generateAccountStatementReport(DEVICE_IP, SESSION, 10, startDate, endDate);

        // Assert
        assertThat(response).isSameAs(expected);
        verify(transactionService).generateAccountStatementReport(DEVICE_IP, SESSION, 10, startDate, endDate);
    }

    @Test
    void updateTransactionStatusShouldDelegateToService() {
        // Arrange
        TransactionStatusUpdateRequest request = new TransactionStatusUpdateRequest().status(Status.INACTIVE);
        ResponseEntity<TransactionResponse> expected = ResponseEntity.ok(new TransactionResponse().id(1));
        when(transactionService.updateTransactionStatus(DEVICE_IP, SESSION, 1, request)).thenReturn(expected);

        // Act
        ResponseEntity<TransactionResponse> response = controller.updateTransactionStatus(DEVICE_IP, SESSION, 1, request);

        // Assert
        assertThat(response).isSameAs(expected);
        verify(transactionService).updateTransactionStatus(DEVICE_IP, SESSION, 1, request);
    }
}
