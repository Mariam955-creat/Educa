package com.educa.backend.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.config.EducaProperties;

/** Stockage sur le système de fichiers local (profil dev). */
@Service
public class FileSystemStorageService implements StorageService {

    private final Path root;

    public FileSystemStorageService(EducaProperties properties) {
        this.root = Paths.get(properties.storage().localPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le dossier de stockage " + root, e);
        }
    }

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fichier vide");
        }
        String ext = extensionOf(file.getOriginalFilename());
        String key = folder + "/" + UUID.randomUUID() + ext;
        Path target = resolveWithinRoot(key);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de l'écriture du fichier", e);
        }
        return key;
    }

    @Override
    public String store(byte[] content, String folder, String extension) {
        String ext = extension == null || extension.isBlank() ? "" : "." + extension.toLowerCase();
        String key = folder + "/" + UUID.randomUUID() + ext;
        Path target = resolveWithinRoot(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de l'écriture du fichier", e);
        }
        return key;
    }

    @Override
    public Resource loadAsResource(String key) {
        Path target = resolveWithinRoot(key);
        if (!Files.exists(target) || !Files.isReadable(target)) {
            throw new ResourceNotFoundException("Fichier introuvable");
        }
        try {
            return new UrlResource(target.toUri());
        } catch (IOException e) {
            throw new UncheckedIOException("Fichier illisible", e);
        }
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        try {
            Files.deleteIfExists(resolveWithinRoot(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Échec de la suppression du fichier", e);
        }
    }

    private Path resolveWithinRoot(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Clé de fichier invalide");
        }
        return resolved;
    }

    private static String extensionOf(String filename) {
        String ext = StringUtils.getFilenameExtension(filename);
        return ext == null || ext.isBlank() ? "" : "." + ext.toLowerCase();
    }
}
