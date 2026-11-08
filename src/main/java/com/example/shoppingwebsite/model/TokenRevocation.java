package com.example.shoppingwebsite.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Per-user token generation, stored in the database so logout survives restarts and is shared by every instance.
 * Every token carries the generation it was issued under; logout increments it, which revokes all older tokens.
 * A user without a row is at generation 0.
 */
@Entity
@Table(name = "token_revocations")
public class TokenRevocation {

    @Id
    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "generation", nullable = false)
    private long generation;

    protected TokenRevocation() {
    }

    public TokenRevocation(String username, long generation) {
        this.username = username;
        this.generation = generation;
    }

    public String getUsername() {
        return username;
    }

    public long getGeneration() {
        return generation;
    }
}
