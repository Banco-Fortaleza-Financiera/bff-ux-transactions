package com.bancofortaleza.transactions.services.impl;

import com.bancofortaleza.transactions.configuration.SupportHeadersProvider;
import com.bancofortaleza.transactions.services.TransactionService;
import com.bff.services.client.SupportApiClient;
import com.bff.services.client.models.ConceptTransaction;
import com.bff.services.client.models.Status;
import com.bff.services.client.models.TransactionCreateRequest;
import com.bff.services.client.models.TransactionResponse;
import com.bff.services.client.models.TransactionStatusUpdateRequest;
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
                toClientRequest(transactionCreateRequest)
        );

        return ResponseEntity.status(response.getStatusCode())
                .headers(sanitizeHeaders(response.getHeaders()))
                .body(toServerResponse(response.getBody()));
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
                .body(toServerResponse(response.getBody()));
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
                toClientConcept(concept),
                toClientStatus(status)
        );

        List<com.bff.services.server.models.TransactionResponse> body = response.getBody() == null
                ? null
                : response.getBody().stream().map(this::toServerResponse).toList();

        return ResponseEntity.status(response.getStatusCode())
                .headers(sanitizeHeaders(response.getHeaders()))
                .body(body);
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
                toClientRequest(transactionStatusUpdateRequest)
        );

        return ResponseEntity.status(response.getStatusCode())
                .headers(sanitizeHeaders(response.getHeaders()))
                .body(toServerResponse(response.getBody()));
    }

    private TransactionCreateRequest toClientRequest(com.bff.services.server.models.TransactionCreateRequest request) {
        if (request == null) {
            return null;
        }

        return new TransactionCreateRequest()
                .idAccount(request.getIdAccount())
                .amount(request.getAmount())
                .description(request.getDescription())
                .concept(toClientConcept(request.getConcept()))
                .status(toClientStatus(request.getStatus()));
    }

    private TransactionStatusUpdateRequest toClientRequest(com.bff.services.server.models.TransactionStatusUpdateRequest request) {
        if (request == null) {
            return null;
        }

        return new TransactionStatusUpdateRequest()
                .status(toClientStatus(request.getStatus()));
    }

    private com.bff.services.server.models.TransactionResponse toServerResponse(TransactionResponse response) {
        if (response == null) {
            return null;
        }

        return new com.bff.services.server.models.TransactionResponse()
                .id(response.getId())
                .idAccount(response.getIdAccount())
                .amount(response.getAmount())
                .description(response.getDescription())
                .concept(toServerConcept(response.getConcept()))
                .status(toServerStatus(response.getStatus()))
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt());
    }

    private ConceptTransaction toClientConcept(com.bff.services.server.models.ConceptTransaction concept) {
        return concept == null ? null : ConceptTransaction.fromValue(concept.getValue());
    }

    private com.bff.services.server.models.ConceptTransaction toServerConcept(ConceptTransaction concept) {
        return concept == null ? null : com.bff.services.server.models.ConceptTransaction.fromValue(concept.getValue());
    }

    private Status toClientStatus(com.bff.services.server.models.Status status) {
        return status == null ? null : Status.fromValue(status.getValue());
    }

    private com.bff.services.server.models.Status toServerStatus(Status status) {
        return status == null ? null : com.bff.services.server.models.Status.fromValue(status.getValue());
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
