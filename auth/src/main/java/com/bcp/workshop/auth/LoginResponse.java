package com.bcp.workshop.auth;

public record LoginResponse(String token, int expiresIn, LoginUserResponse user, String debug) {}
