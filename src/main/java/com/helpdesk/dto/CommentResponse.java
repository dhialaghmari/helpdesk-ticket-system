package com.helpdesk.dto;

import com.helpdesk.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String author,
        String content,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), c.getAuthor().getUsername(), c.getContent(), c.getCreatedAt());
    }
}
