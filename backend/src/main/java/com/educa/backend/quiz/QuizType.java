package com.educa.backend.quiz;

public enum QuizType {
    /** Contrôle rattaché à un chapitre — tentatives illimitées. */
    CONTROL,
    /** Examen final rattaché au cours — tentatives limitées, déverrouillé à 100 % de progression. */
    FINAL_EXAM
}
