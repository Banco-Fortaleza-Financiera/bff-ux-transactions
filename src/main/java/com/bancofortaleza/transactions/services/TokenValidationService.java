package com.bancofortaleza.transactions.services;

public interface TokenValidationService {

    Integer validate(String authorizationHeader, String xDeviceIp, String xSession);
}
