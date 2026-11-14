package com.helpdesk.repository;

import com.helpdesk.entity.Role;
import com.helpdesk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    List<User> findByRoleInOrderByUsernameAsc(List<Role> roles);
}
