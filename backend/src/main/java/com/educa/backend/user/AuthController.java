package com.educa.backend.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.educa.backend.security.CurrentUser;
import com.educa.backend.user.dto.LoginRequest;
import com.educa.backend.user.dto.RefreshRequest;
import com.educa.backend.user.dto.RegisterRequest;
import com.educa.backend.user.dto.TokenResponse;
import com.educa.backend.user.dto.UpdateMeRequest;
import com.educa.backend.user.dto.UserDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    public UserDto me() {
        return userService.getById(CurrentUser.id());
    }

    @PatchMapping("/me")
    public UserDto updateMe(@Valid @RequestBody UpdateMeRequest request) {
        return userService.update(CurrentUser.id(), request);
    }
}
