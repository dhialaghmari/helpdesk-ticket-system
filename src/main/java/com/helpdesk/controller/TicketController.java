package com.helpdesk.controller;

import com.helpdesk.dto.AssignRequest;
import com.helpdesk.dto.CommentRequest;
import com.helpdesk.dto.CommentResponse;
import com.helpdesk.dto.StatusUpdateRequest;
import com.helpdesk.dto.TicketRequest;
import com.helpdesk.dto.TicketResponse;
import com.helpdesk.entity.Category;
import com.helpdesk.entity.Priority;
import com.helpdesk.entity.TicketStatus;
import com.helpdesk.service.CommentService;
import com.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;

    public TicketController(TicketService ticketService, CommentService commentService) {
        this.ticketService = ticketService;
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody TicketRequest request, Authentication authentication) {
        return ticketService.create(request, authentication.getName());
    }

    @GetMapping
    public Page<TicketResponse> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Category category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return ticketService.search(authentication.getName(), status, priority, category, pageable);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public Map<TicketStatus, Long> statistics() {
        return ticketService.statistics();
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id, Authentication authentication) {
        return ticketService.get(id, authentication.getName());
    }

    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable Long id,
                                 @Valid @RequestBody TicketRequest request,
                                 Authentication authentication) {
        return ticketService.update(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public TicketResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return ticketService.changeStatus(id, request.status());
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public TicketResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return ticketService.assign(id, request.assigneeId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ticketService.delete(id);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id, Authentication authentication) {
        return commentService.list(id, authentication.getName());
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable Long id,
                                      @Valid @RequestBody CommentRequest request,
                                      Authentication authentication) {
        return commentService.add(id, request, authentication.getName());
    }
}
