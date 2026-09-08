package com.educa.backend.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.educa.backend.common.error.ResourceNotFoundException;
import com.educa.backend.user.dto.UpdateMeRequest;
import com.educa.backend.user.dto.UserDto;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        return userMapper.toDto(loadById(id));
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
}
