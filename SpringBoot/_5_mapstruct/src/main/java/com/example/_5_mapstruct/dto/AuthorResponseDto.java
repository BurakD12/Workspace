package com.example._5_mapstruct.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuthorResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String bio;
}
