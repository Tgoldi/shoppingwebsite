package com.example.shoppingwebsite.repository;

import com.example.shoppingwebsite.model.TokenRevocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TokenRevocationRepository extends JpaRepository<TokenRevocation, String> {

    /** Atomically moves the user to the next token generation. Returns 0 when the user has no row yet. */
    @Transactional
    @Modifying
    @Query("UPDATE TokenRevocation t SET t.generation = t.generation + 1 WHERE t.username = :username")
    int incrementGeneration(@Param("username") String username);
}
