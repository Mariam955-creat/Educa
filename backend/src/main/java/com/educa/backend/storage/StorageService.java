package com.educa.backend.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction du stockage de fichiers. Implémentation dev : {@link FileSystemStorageService}.
 * Cible ultérieure : implémentation S3-compatible.
 */
public interface StorageService {

    /** Stocke le fichier sous {@code folder/} et renvoie sa clé (chemin relatif). */
    String store(MultipartFile file, String folder);

    /** Stocke un contenu binaire brut sous {@code folder/} avec l'extension donnée (ex. {@code "pdf"}). */
    String store(byte[] content, String folder, String extension);

    Resource loadAsResource(String key);

    void delete(String key);
}
