// com/polytech/paqbackend/dto/PasswordResetResponseDto.java
package com.polytech.paqbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponseDto {
    private boolean success;
    private String message;
    private String token; // Pour debug (à ne pas retourner en prod)
}