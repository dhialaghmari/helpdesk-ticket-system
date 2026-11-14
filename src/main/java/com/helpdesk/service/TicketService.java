package com.helpdesk.service;

import com.helpdesk.dto.TicketRequest;
import com.helpdesk.dto.TicketResponse;
import com.helpdesk.entity.Category;
import com.helpdesk.entity.Priority;
import com.helpdesk.entity.Ticket;
import com.helpdesk.entity.TicketStatus;
import com.helpdesk.entity.User;
import com.helpdesk.exception.BadRequestException;
import com.helpdesk.exception.ConflictException;
import com.helpdesk.exception.ResourceNotFoundException;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TicketResponse create(TicketRequest request, String username) {
        Ticket ticket = new Ticket();
        ticket.setCreatedBy(findUser(username));
        ticket.setStatus(TicketStatus.OPEN);
        apply(ticket, request);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    /** Staff see every ticket; regular users only see the ones they created. */
    public Page<TicketResponse> search(String username, TicketStatus status, Priority priority,
                                       Category category, Pageable pageable) {
        User user = findUser(username);

        Specification<Ticket> spec = (root, query, cb) -> cb.conjunction();
        if (!user.isStaff()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("createdBy").get("id"), user.getId()));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        return ticketRepository.findAll(spec, pageable).map(TicketResponse::from);
    }

    public TicketResponse get(Long id, String username) {
        return TicketResponse.from(loadAccessible(id, username));
    }

    @Transactional
    public TicketResponse update(Long id, TicketRequest request, String username) {
        Ticket ticket = loadAccessible(id, username);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ConflictException("Closed tickets cannot be modified");
        }
        apply(ticket, request);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse changeStatus(Long id, TicketStatus status) {
        Ticket ticket = findTicket(id);
        if (ticket.getStatus() != status && !ticket.getStatus().canTransitionTo(status)) {
            throw new ConflictException("Invalid status change: " + ticket.getStatus() + " -> " + status);
        }
        ticket.setStatus(status);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    /** Assigns the ticket to a staff member; an OPEN ticket moves to IN_PROGRESS. */
    @Transactional
    public TicketResponse assign(Long id, Long assigneeId) {
        Ticket ticket = findTicket(id);
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + assigneeId));
        if (!assignee.isStaff()) {
            throw new BadRequestException("Tickets can only be assigned to an AGENT or an ADMIN");
        }
        ticket.setAssignedTo(assignee);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public void delete(Long id) {
        ticketRepository.delete(findTicket(id));
    }

    public Map<TicketStatus, Long> statistics() {
        Map<TicketStatus, Long> result = new EnumMap<>(TicketStatus.class);
        for (TicketStatus s : TicketStatus.values()) {
            result.put(s, 0L);
        }
        for (Object[] row : ticketRepository.countByStatus()) {
            result.put((TicketStatus) row[0], (Long) row[1]);
        }
        return result;
    }

    /** Returns the ticket if it exists and the user may see it (owner or staff). */
    public Ticket loadAccessible(Long id, String username) {
        Ticket ticket = findTicket(id);
        User user = findUser(username);
        boolean owner = ticket.getCreatedBy().getId().equals(user.getId());
        if (!owner && !user.isStaff()) {
            throw new AccessDeniedException("Access to this ticket is denied");
        }
        return ticket;
    }

    private void apply(Ticket ticket, TicketRequest request) {
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
        ticket.setCategory(request.category() != null ? request.category() : Category.OTHER);
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
