package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthService {
    @Inject
    UserRepository userRepository;

    public UserRecord authenticate(String username, String password) {
        return userRepository.findByCredentials(username, password);
    }
}
