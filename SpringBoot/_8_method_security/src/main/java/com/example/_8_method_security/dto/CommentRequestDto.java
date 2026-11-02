package com.example._8_method_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CommentRequestDto {
    @NotNull(message = "Task id is required")
    private Long taskId;

    @NotBlank(message = "Text is required")
    @Size(max = 2000, message = "Text must be at most 2000 characters")
    private String text;
}
