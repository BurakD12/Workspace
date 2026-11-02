package com.example._9_docker.dto;

import java.util.Set;

import com.example._9_docker.model.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Set<Role> roles;
}