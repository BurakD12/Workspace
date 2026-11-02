package com.example._9_docker.dto;

import com.example._9_docker.model.RefreshToken;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
public class RefreshRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}