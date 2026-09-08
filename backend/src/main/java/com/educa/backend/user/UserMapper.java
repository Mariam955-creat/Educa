package com.educa.backend.user;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.educa.backend.user.dto.UserDto;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(),
                user.getPreferredLanguage(), roles);
    }
}
