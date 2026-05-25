// com/polytech/paqbackend/dto/PasswordResetRequestDto.java
package com.polytech.paqbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequestDto {

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;
}