package com.grenlus.signage.controller;

import com.grenlus.signage.dtos.LoginRequestDto;
import com.grenlus.signage.dtos.LoginResponseDto;
import com.grenlus.signage.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {

        // Si las credenciales no sirven, esto lanza y el manejador global
        // responde 401. Nunca se aclara si fallo el email o la contrasenia:
        // decirlo permitiria averiguar que emails existen.
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserDetails usuario = (UserDetails) auth.getPrincipal();
        String rol = usuario.getAuthorities().iterator().next().getAuthority()
                .replaceFirst("^ROLE_", "");

        return ResponseEntity.ok(new LoginResponseDto(
                jwtService.generar(usuario), usuario.getUsername(), rol, jwtService.duracionMs()));
    }
}
