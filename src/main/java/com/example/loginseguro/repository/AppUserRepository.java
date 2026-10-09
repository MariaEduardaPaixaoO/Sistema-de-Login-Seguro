package com.example.loginseguro.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.loginseguro.model.AppUser;

public interface AppUserRepository extends MongoRepository<AppUser, String> {

    /** O e-mail recebido deve estar normalizado (ver UserService#normalizeEmail). */
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
