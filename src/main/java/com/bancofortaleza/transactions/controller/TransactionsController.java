package com.bancofortaleza.transactions.controller;

import com.bancofortaleza.transactions.services.TransactionService;
import com.bff.services.server.ChannelApi;
import com.bff.services.server.models.AccountStatementReportResponse;
import com.bff.services.server.models.ConceptTransaction;
import com.bff.services.server.models.Status;
import com.bff.services.server.models.TransactionCreateRequest;
import com.bff.services.server.models.TransactionResponse;
import com.bff.services.server.models.TransactionStatusUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TransactionsController implements ChannelApi {

    private final TransactionService transactionService;

    @Override
    public ResponseEntity<TransactionResponse> createTransaction(
            String xDeviceIp,
            String xSession,
            TransactionCreateRequest transactionCreateRequest
    ) {
        return transactionService.createTransaction(xDeviceIp, xSession, transactionCreateRequest);
    }

    @Override
    public ResponseEntity<TransactionResponse> getTransactionById(String xDeviceIp, String xSession, Integer id) {
        return transactionService.getTransactionById(xDeviceIp, xSession, id);
    }

    @Override
    public ResponseEntity<List<TransactionResponse>> listTransactions(
            String xDeviceIp,
            String xSession,
            Integer xPage,
            Integer xPageSize,
            String search,
            Integer idAccount,
            ConceptTransaction concept,
            Status status
    ) {
        return transactionService.listTransactions(xDeviceIp, xSession, xPage, xPageSize, search, idAccount, concept, status);
    }

    @Override
    public ResponseEntity<AccountStatementReportResponse> generateAccountStatementReport(
            String xDeviceIp,
            String xSession,
            Integer idUser,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return transactionService.generateAccountStatementReport(xDeviceIp, xSession, idUser, startDate, endDate);
    }

    @Override
    public ResponseEntity<TransactionResponse> updateTransactionStatus(
            String xDeviceIp,
            String xSession,
            Integer id,
            TransactionStatusUpdateRequest transactionStatusUpdateRequest
    ) {
        return transactionService.updateTransactionStatus(xDeviceIp, xSession, id, transactionStatusUpdateRequest);
    }
}
