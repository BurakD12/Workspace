package com.example._10_integration_testing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupplierRequestDto {
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Email(message = "Email must be a valid address")
    @Size(max = 150, message = "Email must be at most 150 characters")
    private String email;

    @Size(max = 40, message = "Phone must be at most 40 characters")
    private String phone;
}
