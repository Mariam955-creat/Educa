package com.educa.backend.user;

import java.util.HashSet;
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
import com.educa.backend.user.dto.UpdateMeRequest;
import com.educa.backend.user.dto.UserDto;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
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
        return userMapper.toDto(userRepository.save(user));
    }

    private User loadById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    // ---------- administration ----------

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

    private static RoleName parseRoleName(String name) {
        try {
            return RoleName.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rôle inconnu : " + name);
        }
    }
}
