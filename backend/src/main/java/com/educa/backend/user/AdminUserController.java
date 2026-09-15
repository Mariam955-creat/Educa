package com.educa.backend.user;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.common.web.PageResponse;
import com.educa.backend.user.dto.AdminUserDto;
import com.educa.backend.user.dto.UpdateRolesRequest;
import com.educa.backend.user.dto.UpdateStatusRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<AdminUserDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageResponse.of(userService.adminList(q, PageRequest.of(Math.max(page, 0), safeSize)));
    }

    @PatchMapping("/{id}/roles")
    public AdminUserDto updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateRolesRequest request) {
        return userService.updateRoles(id, request.roles());
    }

    @PatchMapping("/{id}/status")
    public AdminUserDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return userService.updateStatus(id, request.enabled());
    }
}
