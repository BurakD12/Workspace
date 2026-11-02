package com.example._8_method_security.service;

import java.util.List;

import com.example._8_method_security.dto.CommentRequestDto;
import com.example._8_method_security.dto.CommentResponseDto;
import com.example._8_method_security.dto.CommentUpdateRequestDto;

public interface CommentService {
    CommentResponseDto createComment(CommentRequestDto request, String authorUsername);
    List<CommentResponseDto> getCommentsByTask(Long taskId);
    CommentResponseDto getCommentById(Long id);
    CommentResponseDto updateComment(Long id, CommentUpdateRequestDto request);
    void deleteComment(Long id);
}