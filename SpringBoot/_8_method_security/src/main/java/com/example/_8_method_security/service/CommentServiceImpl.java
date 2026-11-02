package com.example._8_method_security.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example._8_method_security.dto.CommentRequestDto;
import com.example._8_method_security.dto.CommentResponseDto;
import com.example._8_method_security.dto.CommentUpdateRequestDto;
import com.example._8_method_security.dto.TaskSummaryDto;
import com.example._8_method_security.dto.UserSummaryDto;
import com.example._8_method_security.exception.ResourceNotFoundException;
import com.example._8_method_security.model.Comment;
import com.example._8_method_security.model.Task;
import com.example._8_method_security.model.User;
import com.example._8_method_security.repository.CommentRepository;
import com.example._8_method_security.repository.TaskRepository;
import com.example._8_method_security.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService{
    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @PreAuthorize("#authorUsername == authentication.name and (hasRole('ADMIN'))" + 
        "or @taskFlowPermissions.canWorkOnTask(#request.taskId, authentication)"
    )
    public CommentResponseDto createComment(CommentRequestDto request, String authorUsername) {
        Long taskId = request.getTaskId();
        Task task = taskRepository.findById(taskId)
                        .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
        User author = userRepository.findByUsername(authorUsername)
                        .orElseThrow(() -> new ResourceNotFoundException("User", authorUsername));
        
        Comment comment = Comment.builder()
                .text(request.getText())
                .author(author)
                .task(task)
                .build();
                
        Comment saved = commentRepository.save(comment);
        return toResponse(saved);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.canWorkOnTask(#taskId, authentication)")
    @PostFilter("hasRole('ADMIN') or filterObject.author.username == authentication.name " + 
        "or @taskFlowPermissions.isProjectOwnerOfTask(#taskId, authentication)")
    public List<CommentResponseDto> getCommentsByTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
           throw new ResourceNotFoundException("Task", taskId); 
        }
        
        return new ArrayList<>(commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(this::toResponse)
                .toList());
    }

    @Override
    @PostAuthorize("hasRole('ADMIN') or returnObject.author.username == authentication.name "
        + "or @taskFlowPermissions.isProjectOwnerOfTask(returnObject.task.id, authentication)")
    public CommentResponseDto getCommentById(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    @Transactional
    @PreAuthorize("@taskFlowPermissions.isCommentAuthor(#id, authentication)")
    public CommentResponseDto updateComment(Long id, CommentUpdateRequestDto request) {
        Comment comment = getEntity(id);
        comment.setText(request.getText());
        return toResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional 
    @PreAuthorize("hasRole('ADMIN') or @taskFlowPermissions.isCommentAuthor(#id, authentication)")
    public void deleteComment(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Comment", id);
        }
        commentRepository.deleteById(id);
    }

    private CommentResponseDto toResponse(Comment comment) {
        User author = comment.getAuthor();
        UserSummaryDto userSummaryDto = author == null ? null : new UserSummaryDto(author.getId(), author.getUsername());

        Task task = comment.getTask();
        TaskSummaryDto taskSummaryDto = task == null ? null : new TaskSummaryDto(task.getId(), task.getTitle());

        return new CommentResponseDto(comment.getId(), comment.getText(), comment.getCreatedAt(), taskSummaryDto, userSummaryDto);
    }

    private Comment getEntity(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }

}
