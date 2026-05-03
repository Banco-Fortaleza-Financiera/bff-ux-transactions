package com.bancofortaleza.transactions.services;

import com.bff.services.server.models.AccountStatementReportResponse;
import com.bff.services.server.models.ConceptTransaction;
import com.bff.services.server.models.Status;
import com.bff.services.server.models.TransactionCreateRequest;
import com.bff.services.server.models.TransactionResponse;
import com.bff.services.server.models.TransactionStatusUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;

public interface TransactionService {

    ResponseEntity<TransactionResponse> createTransaction(
            String xDeviceIp,
            String xSession,
            TransactionCreateRequest transactionCreateRequest
    );

    ResponseEntity<TransactionResponse> getTransactionById(String xDeviceIp, String xSession, Integer id);

    ResponseEntity<List<TransactionResponse>> listTransactions(
            String xDeviceIp,
            String xSession,
            Integer xPage,
            Integer xPageSize,
            String search,
            Integer idAccount,
            ConceptTransaction concept,
            Status status
    );

    ResponseEntity<AccountStatementReportResponse> generateAccountStatementReport(
            String xDeviceIp,
            String xSession,
            Integer idUser,
            LocalDate startDate,
            LocalDate endDate
    );

    ResponseEntity<TransactionResponse> updateTransactionStatus(
            String xDeviceIp,
            String xSession,
            Integer id,
            TransactionStatusUpdateRequest transactionStatusUpdateRequest
    );
}
