package com.educa.backend.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.educa.backend.user.UserRepository;

/**
 * Charge un utilisateur par email pour l'authentification par mot de passe
 * (utilisé par l'{@code AuthenticationManager} lors du login).
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.educa.backend.user.User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .build();
    }
}
