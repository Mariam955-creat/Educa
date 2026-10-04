package com.educa.backend.user;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.educa.backend.common.error.ApiException;
import com.educa.backend.common.error.ConflictException;
import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.security.CurrentUser;
import com.educa.backend.user.dto.AdminUserDto;
import com.educa.backend.user.dto.PublicProfileDto;
import com.educa.backend.user.dto.TrashedUserDto;
import com.educa.backend.user.dto.UpdateMeRequest;
import com.educa.backend.user.dto.UserDto;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final List<UserDeletionGuard> deletionGuards;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper,
                       RefreshTokenRepository refreshTokenRepository, List<UserDeletionGuard> deletionGuards) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.refreshTokenRepository = refreshTokenRepository;
        this.deletionGuards = deletionGuards;
    }

    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        return userMapper.toDto(loadById(id));
    }

    /** Nom d'affichage d'un utilisateur (pour les DTO d'autres modules). */
    @Transactional(readOnly = true)
    public String displayNameById(Long id) {
        return userRepository.findById(id).map(User::getFullName).orElse("—");
    }

    @Transactional
    public UserDto update(Long id, UpdateMeRequest request) {
        User user = loadById(id);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.preferredLanguage() != null) {
            user.setPreferredLanguage(request.preferredLanguage());
        }
        // Champs facultatifs : absent = inchangé, chaîne vide = effacé
        if (request.headline() != null) user.setHeadline(blankToNull(request.headline()));
        if (request.bio() != null) user.setBio(blankToNull(request.bio()));
        if (request.country() != null) user.setCountry(blankToNull(request.country()));
        if (request.phone() != null) user.setPhone(blankToNull(request.phone()));
        return userMapper.toDto(userRepository.save(user));
    }

    /** Profil public (formateur d'un cours) : nom, titre et biographie — jamais d'email ni de téléphone. */
    @Transactional(readOnly = true)
    public PublicProfileDto publicProfile(Long id) {
        return userRepository.findById(id)
                .map(u -> new PublicProfileDto(u.getFullName(), u.getHeadline(), u.getBio()))
                .orElse(new PublicProfileDto("—", null, null));
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private User loadById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    // ---------- administration ----------

    /** Compteurs d'utilisateurs pour le tableau de bord admin. */
    @Transactional(readOnly = true)
    public UserStats stats(java.time.Instant newSince) {
        return new UserStats(userRepository.countByDeletedAtIsNull(),
                userRepository.countByRoles_NameAndDeletedAtIsNull(RoleName.LEARNER),
                userRepository.countByRoles_NameAndDeletedAtIsNull(RoleName.INSTRUCTOR),
                userRepository.countByRoles_NameAndDeletedAtIsNull(RoleName.ADMIN),
                userRepository.countByEnabledFalseAndDeletedAtIsNull(),
                userRepository.countByCreatedAtAfterAndDeletedAtIsNull(newSince));
    }

    public record UserStats(long total, long learners, long instructors, long admins, long disabled, long recent) {
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> adminList(String q, Pageable pageable) {
        String normalizedQ = StringUtils.hasText(q) ? q.trim() : null;
        return userRepository.search(normalizedQ, pageable).map(userMapper::toAdminDto);
    }

    @Transactional
    public AdminUserDto updateRoles(Long id, Set<String> roleNames) {
        User user = loadById(id);
        Set<Role> resolved = new HashSet<>();
        for (String name : roleNames) {
            RoleName roleName = parseRoleName(name);
            resolved.add(roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Rôle " + roleName + " absent — vérifier les migrations")));
        }
        if (id.equals(CurrentUser.id()) && resolved.stream().noneMatch(r -> r.getName() == RoleName.ADMIN)) {
            throw new ConflictException("Vous ne pouvez pas retirer votre propre rôle administrateur");
        }
        user.setRoles(resolved);
        return userMapper.toAdminDto(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto updateStatus(Long id, boolean enabled) {
        if (!enabled && id.equals(CurrentUser.id())) {
            throw new ConflictException("Vous ne pouvez pas désactiver votre propre compte");
        }
        User user = loadById(id);
        user.setEnabled(enabled);
        return userMapper.toAdminDto(userRepository.save(user));
    }

    // ---------- corbeille ----------

    /**
     * Met un compte à la corbeille : il est désactivé (connexion et rafraîchissement de session refusés), ses
     * sessions sont révoquées et il disparaît des listes. Restaurable.
     */
    @Transactional
    public void softDelete(Long id) {
        if (id.equals(CurrentUser.id())) {
            throw new ConflictException("Vous ne pouvez pas supprimer votre propre compte");
        }
        User user = loadById(id);
        if (user.getDeletedAt() == null) {
            user.setDeletedAt(java.time.Instant.now());
            user.setEnabled(false);
            refreshTokenRepository.findByUser_IdAndRevokedFalse(id).forEach(token -> token.setRevoked(true));
        }
    }

    @Transactional
    public AdminUserDto restore(Long id) {
        User user = loadById(id);
        user.setDeletedAt(null);
        user.setEnabled(true);
        return userMapper.toAdminDto(user);
    }

    /**
     * Suppression définitive, depuis la corbeille uniquement. Efface en cascade inscriptions, achats,
     * certificats et avis du compte ; refusée (409) si une garde s'y oppose (formateur ayant des cours).
     */
    @Transactional
    public void deletePermanently(Long id) {
        User user = loadById(id);
        if (user.getDeletedAt() == null) {
            throw new ConflictException("Mettez d'abord le compte à la corbeille");
        }
        for (UserDeletionGuard guard : deletionGuards) {
            String reason = guard.blockingReason(id);
            if (reason != null) {
                throw new ConflictException(reason);
            }
        }
        refreshTokenRepository.deleteByUser_Id(id);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<TrashedUserDto> trash() {
        return userRepository.findByDeletedAtIsNotNullOrderByDeletedAtDesc().stream()
                .map(u -> new TrashedUserDto(u.getId(), u.getEmail(), u.getFullName(),
                        u.getRoles().stream().map(r -> r.getName().name()).collect(java.util.stream.Collectors.toSet()),
                        u.getDeletedAt()))
                .toList();
    }

    private static RoleName parseRoleName(String name) {
        try {
            return RoleName.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rôle inconnu : " + name);
        }
    }
}
