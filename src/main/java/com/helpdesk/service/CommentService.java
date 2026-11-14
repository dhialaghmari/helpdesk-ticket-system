package com.helpdesk.service;

import com.helpdesk.dto.CommentRequest;
import com.helpdesk.dto.CommentResponse;
import com.helpdesk.entity.Comment;
import com.helpdesk.entity.Ticket;
import com.helpdesk.entity.TicketStatus;
import com.helpdesk.entity.User;
import com.helpdesk.exception.ConflictException;
import com.helpdesk.exception.ResourceNotFoundException;
import com.helpdesk.repository.CommentRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;

    public CommentService(CommentRepository commentRepository,
                          UserRepository userRepository,
                          TicketService ticketService) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
    }

    public List<CommentResponse> list(Long ticketId, String username) {
        ticketService.loadAccessible(ticketId, username);
        return commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse add(Long ticketId, CommentRequest request, String username) {
        Ticket ticket = ticketService.loadAccessible(ticketId, username);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ConflictException("Cannot comment on a closed ticket");
        }
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(request.content().trim());
        return CommentResponse.from(commentRepository.save(comment));
    }
}
