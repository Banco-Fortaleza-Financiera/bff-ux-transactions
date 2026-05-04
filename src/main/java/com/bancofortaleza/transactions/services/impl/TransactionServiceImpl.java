package com.bancofortaleza.transactions.services.impl;

import com.bancofortaleza.transactions.configuration.SupportHeadersProvider;
import com.bancofortaleza.transactions.services.TransactionService;
import com.bancofortaleza.transactions.services.mapper.TransactionMapper;
import com.bff.services.client.SupportApiClient;
import com.bff.services.client.models.AccountStatementReportResponse;
import com.bff.services.client.models.TransactionResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            HttpHeaders.CONNECTION.toLowerCase(Locale.ROOT),
            HttpHeaders.CONTENT_LENGTH.toLowerCase(Locale.ROOT),
            HttpHeaders.TRANSFER_ENCODING.toLowerCase(Locale.ROOT),
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "upgrade"
    );

    private final SupportApiClient supportApiClient;
    private final SupportHeadersProvider supportHeadersProvider;
    private final TransactionMapper transactionMapper;

    @Override
    public ResponseEntity<com.bff.services.server.models.TransactionResponse> createTransaction(
            String xDeviceIp,
            String xSession,
            com.bff.services.server.models.TransactionCreateRequest transactionCreateRequest
    ) {
        ResponseEntity<TransactionResponse> response = supportApiClient.createTransaction(
                xDeviceIp,
                xSession,
                supportHeadersProvider.getAuthenticatedUserId(),
                transactionMapper.toClientRequest(transactionCreateRequest)
        );

        return ResponseEntity.status(response.getStatusCode())
                .headers(sanitizeHeaders(response.getHeaders()))
                .body(transactionMapper.toServerResponse(response.getBody()));
    }

    @Override
    public ResponseEntity<com.bff.services.server.models.TransactionResponse> getTransactionById(String xDeviceIp, String xSession, Integer id) {
        ResponseEntity<TransactionResponse> response = supportApiClient.getTransactionById(
                xDeviceIp,
                xSession,
                supportHeadersProvider.getAuthenticatedUserId(),
                id
        );

        return ResponseEntity.status(response.getStatusCode())
                .headers(sanitizeHeaders(response.getHeaders()))
                .body(transactionMapper.toServerResponse(response.getBody()));
    }

    @Override
    public ResponseEntity<List<com.bff.services.server.models.TransactionResponse>> listTransactions(
            String xDeviceIp,
            String xSession,
            Integer xPage,
            Integer xPageSize,
            String search,
            Integer idAccount,
            com.bff.services.server.models.ConceptTransaction concept,
            com.bff.services.server.models.Status status
    ) {
        ResponseEntity<List<TransactionResponse>> response = supportApiClient.listTransactions(
                xDeviceIp,
                xSession,
                supportHeadersProvider.getAuthenticatedUserId(),
                xPage,
                xPageSize,
                search,
                idAccount,
                transactionMapper.toClientConcept(concept),
                transactionMapper.toClientStatus(status)
        );

        return ResponseEntity.status(response.getStatusCode())
                .headers(sanitizeHeaders(response.getHeaders()))
                .body(transactionMapper.toServerTransactionResponses(response.getBody()));
    }

    @Override
    public ResponseEntity<com.bff.services.server.models.AccountStatementReportResponse> generateAccountStatementReport(
            String xDeviceIp,
            String xSession,
            Integer idUser,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ResponseEntity<AccountStatementReportResponse> response = supportApiClient.generateAccountStatementReport(
                xDeviceIp,
                xSession,
                supportHeadersProvider.getAuthenticatedUserId(),
                idUser,
                startDate,
                endDate
        );

        return ResponseEntity.status(response.getStatusCode())
                .headers(sanitizeHeaders(response.getHeaders()))
                .body(transactionMapper.toServerResponse(response.getBody()));
    }

    @Override
    public ResponseEntity<com.bff.services.server.models.TransactionResponse> updateTransactionStatus(
            String xDeviceIp,
            String xSession,
            Integer id,
            com.bff.services.server.models.TransactionStatusUpdateRequest transactionStatusUpdateRequest
    ) {
        ResponseEntity<TransactionResponse> response = supportApiClient.updateTransactionStatus(
                xDeviceIp,
                xSession,
                supportHeadersProvider.getAuthenticatedUserId(),
                id,
                transactionMapper.toClientRequest(transactionStatusUpdateRequest)
        );

        return ResponseEntity.status(response.getStatusCode())
                .headers(sanitizeHeaders(response.getHeaders()))
                .body(transactionMapper.toServerResponse(response.getBody()));
    }

    private HttpHeaders sanitizeHeaders(HttpHeaders source) {
        HttpHeaders headers = new HttpHeaders();
        source.forEach((name, values) -> {
            if (!isHopByHopHeader(name)) {
                values.forEach(value -> headers.add(name, value));
            }
        });
        return headers;
    }

    private boolean isHopByHopHeader(String name) {
        return HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }
}
