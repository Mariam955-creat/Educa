package com.educa.backend.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Détecte le type MIME réel d'un fichier à partir de son contenu (magic bytes),
 * indépendamment du {@code Content-Type} déclaré par le client (trivialement falsifiable).
 */
@Component
public class FileTypeDetector {

    private final Tika tika = new Tika();

    /** @return le type MIME détecté (jamais {@code null} — {@code application/octet-stream} si indéterminé). */
    public String detect(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return tika.detect(in, file.getOriginalFilename());
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier impossible", e);
        }
    }
}
