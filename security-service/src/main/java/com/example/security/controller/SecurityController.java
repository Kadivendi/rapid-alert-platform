package com.rapidalert.security.controller;

import com.rapidalert.security.dto.request.SecurityRequest;
import com.rapidalert.security.dto.response.TokenResponse;
import com.rapidalert.security.entity.Client;
import com.rapidalert.security.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class SecurityController {

    private final ClientService clientService;

    @PostMapping("/register")
    @Operation(summary = "register a new Client with given credentials")
    public ResponseEntity<Boolean> register(
            @RequestBody @Valid SecurityRequest request
    ) {
        return ResponseEntity.ok(clientService.register(request));
    }

    @PostMapping("/authenticate")
    @Operation(summary = "authenticate a Client with existing credentials")
    public ResponseEntity<TokenResponse> authenticate(
            @RequestBody @Valid SecurityRequest request
    ) {
        return ResponseEntity.ok(clientService.authenticate(request));
    }

    @GetMapping("/validate")
    @Operation(summary = "validate given JWT and return Client ID")
    public ResponseEntity<Long> isTokenValid(
            @AuthenticationPrincipal Client client
    ) {
        return ResponseEntity.ok(client.getId());
    }
}
