package com.educa.backend.user;

import org.mapstruct.Mapper;

import com.educa.backend.user.dto.AdminUserDto;
import com.educa.backend.user.dto.UserDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    AdminUserDto toAdminDto(User user);

    default String roleName(Role role) {
        return role == null ? null : role.getName().name();
    }
}
