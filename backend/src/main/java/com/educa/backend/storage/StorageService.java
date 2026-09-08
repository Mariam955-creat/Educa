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

    Resource loadAsResource(String key);

    void delete(String key);
}
