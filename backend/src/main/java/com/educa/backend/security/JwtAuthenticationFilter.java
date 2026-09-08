package com.educa.backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Extrait et valide le JWT {@code Authorization: Bearer ...} et place l'identité
 * dans le {@code SecurityContext}. Aucun accès base : l'identité vient des claims.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            Claims claims = jwtService.parse(header.substring(PREFIX.length()));
            if (claims != null) {
                Long userId = Long.valueOf(claims.getSubject());
                String email = claims.get("email", String.class);
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);

                var authorities = roles == null ? List.<SimpleGrantedAuthority>of()
                        : roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();

                var authentication = new UsernamePasswordAuthenticationToken(
                        new AuthPrincipal(userId, email), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
