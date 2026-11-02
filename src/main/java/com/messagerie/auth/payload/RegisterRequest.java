package com.messagerie.auth.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 256) String motDePasse) {
}
