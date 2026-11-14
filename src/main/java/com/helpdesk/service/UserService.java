package com.helpdesk.service;

import com.helpdesk.dto.RegisterRequest;
import com.helpdesk.dto.UserResponse;
import com.helpdesk.entity.Role;
import com.helpdesk.entity.User;
import com.helpdesk.exception.BadRequestException;
import com.helpdesk.exception.ConflictException;
import com.helpdesk.exception.ResourceNotFoundException;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       TicketRepository ticketRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Public sign-up: new accounts always get the USER role. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getByUsername(String username) {
        return UserResponse.from(findByUsername(username));
    }

    public UserResponse getById(Long id) {
        return UserResponse.from(findById(id));
    }

    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    /** ADMIN and AGENT accounts, used to pick an assignee. */
    public List<UserResponse> listStaff() {
        return userRepository.findByRoleInOrderByUsernameAsc(List.of(Role.ADMIN, Role.AGENT)).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateRole(Long id, Role newRole, String currentUsername) {
        User target = findById(id);
        if (target.getUsername().equals(currentUsername)) {
            throw new BadRequestException("You cannot change your own role");
        }
        if (target.getRole() == Role.ADMIN && newRole != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ConflictException("The last administrator cannot be demoted");
        }
        target.setRole(newRole);
        return UserResponse.from(userRepository.save(target));
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        User target = findById(id);
        if (target.getUsername().equals(currentUsername)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        if (target.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ConflictException("The last administrator cannot be deleted");
        }
        if (ticketRepository.existsByCreatedById(id) || ticketRepository.existsByAssignedToId(id)) {
            throw new ConflictException("User is linked to existing tickets and cannot be deleted");
        }
        userRepository.delete(target);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
