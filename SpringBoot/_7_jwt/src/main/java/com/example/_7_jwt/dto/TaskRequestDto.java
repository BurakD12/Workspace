package com.example._7_jwt.dto;

import java.time.LocalDate;

import com.example._7_jwt.model.Priority;
import com.example._7_jwt.model.Status;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class TaskRequestDto {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Status status;

    private Priority priority;

    private LocalDate dueDate;

    @NotNull(message = "Project id is required")
    private Long projectId;

    private Long assigneeId;
}
