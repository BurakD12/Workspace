package com.example._8_method_security.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.example._8_method_security.repository.CommentRepository;
import com.example._8_method_security.repository.ProjectRepository;
import com.example._8_method_security.repository.TaskRepository;

import lombok.RequiredArgsConstructor;

@Component("taskFlowPermissions")
@RequiredArgsConstructor
public class TaskFlowPermissions {
    
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;

    public boolean isProjectOwner(Long projectId, Authentication authentication) {
        String username = usernameOf(authentication);
        if (projectId == null || username == null) {
            return false;
        }  
        return projectRepository.existsByIdAndOwnerUsername(projectId, username);
    }

    public boolean isProjectOwnerOfTask(Long taskId, Authentication authentication) {
        String username = usernameOf(authentication);
        if (taskId == null || username == null) {
            return false;
        }
        return taskRepository.existsByIdAndProjectOwnerUsername(taskId, username);
    }

    public boolean isTaskAssignee(Long taskId, Authentication authentication) {
        String username = usernameOf(authentication);
        if (taskId == null || username == null) {
            return false;
        }
        return taskRepository.existsByIdAndAssigneeUsername(taskId, username);
    }

    public static String usernameOf(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken
        ) {
           return null; 
        }
        return authentication.getName();
    }

    public boolean canWorkOnTask(Long taskId, Authentication authentication) {
        return isProjectOwnerOfTask(taskId, authentication) || isTaskAssignee(taskId, authentication);
    }

    public boolean canPlaceTaskInProject(Long taskId, Long projectId, Authentication authentication) {
        if (taskId == null || projectId == null || usernameOf(authentication) == null) {
            return false;
        }

        if (taskRepository.existsByIdAndProjectId(taskId, projectId)) {
            return true;
        }
        return isProjectOwnerOfTask(taskId, authentication) && isProjectOwner(projectId, authentication);
    }

    public boolean isCommentAuthor(Long commnentId, Authentication authentication) {
        String username = usernameOf(authentication);
        if (commnentId == null || username == null) {
            return false;
        }
        return commentRepository.existsByIdAndAuthorUsername(commnentId, username);
    }
}

