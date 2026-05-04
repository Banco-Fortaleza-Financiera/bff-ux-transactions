package com.bancofortaleza.transactions.services.mapper;

import com.bff.services.client.models.AccountStatementAccount;
import com.bff.services.client.models.AccountStatementReportResponse;
import com.bff.services.client.models.AccountStatementTransaction;
import com.bff.services.client.models.ConceptTransaction;
import com.bff.services.client.models.Status;
import com.bff.services.client.models.TransactionCreateRequest;
import com.bff.services.client.models.TransactionResponse;
import com.bff.services.client.models.TransactionStatusUpdateRequest;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionCreateRequest toClientRequest(
            com.bff.services.server.models.TransactionCreateRequest request
    );

    TransactionStatusUpdateRequest toClientRequest(
            com.bff.services.server.models.TransactionStatusUpdateRequest request
    );

    com.bff.services.server.models.TransactionResponse toServerResponse(TransactionResponse response);

    List<com.bff.services.server.models.TransactionResponse> toServerTransactionResponses(
            List<TransactionResponse> responses
    );

    com.bff.services.server.models.AccountStatementReportResponse toServerResponse(
            AccountStatementReportResponse response
    );

    com.bff.services.server.models.AccountStatementAccount toServerAccount(AccountStatementAccount account);

    com.bff.services.server.models.AccountStatementTransaction toServerTransaction(
            AccountStatementTransaction transaction
    );

    com.bff.services.client.models.ConceptTransaction toClientConcept(
            com.bff.services.server.models.ConceptTransaction concept
    );

    com.bff.services.server.models.ConceptTransaction toServerConcept(ConceptTransaction concept);

    com.bff.services.client.models.Status toClientStatus(
            com.bff.services.server.models.Status status
    );

    com.bff.services.server.models.Status toServerStatus(Status status);
}
