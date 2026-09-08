package com.educa.backend.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** Génération de slugs URL à partir d'un titre. */
public final class Slugs {

    private Slugs() {
    }

    public static String slugify(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return normalized.isBlank() ? "cours" : normalized;
    }

    /** Ajoute un suffixe {@code -2}, {@code -3}, … tant que {@code taken} est vrai. */
    public static String unique(String base, Predicate<String> taken) {
        if (!taken.test(base)) {
            return base;
        }
        int suffix = 2;
        while (taken.test(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }
}
