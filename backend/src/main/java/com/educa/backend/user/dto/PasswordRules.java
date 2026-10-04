package com.educa.backend.user.dto;

/** Règle commune aux mots de passe (inscription, changement) : au moins une lettre et un chiffre. */
public final class PasswordRules {

    public static final String REGEX = "^(?=.*\\p{L})(?=.*\\d).+$";
    public static final String MESSAGE = "le mot de passe doit contenir au moins une lettre et un chiffre";

    private PasswordRules() {
    }
}
