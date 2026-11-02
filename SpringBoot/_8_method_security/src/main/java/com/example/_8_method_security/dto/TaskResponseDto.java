package com.example._8_method_security.dto;

import java.time.LocalDate;

import com.example._8_method_security.model.Priority;
import com.example._8_method_security.model.Status;

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
public class TaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private Status status;
    private Priority priority;
    private LocalDate dueDate;
    private ProjectSummaryDto project;
    private UserSummaryDto assignee;
}
