package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.core.security.TokenService;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    public record LoginRequestDTO(@NotBlank String email, @NotBlank String senha) {}
    public record LoginResponseDTO(String token, String papel, String nome) {}

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.senha());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        
        var usuario = (UsuarioJpaEntity) auth.getPrincipal();
        var token = tokenService.generateToken(usuario);
        
        return ResponseEntity.ok(new LoginResponseDTO(token, usuario.getPapel(), usuario.getNome()));
    }
}
