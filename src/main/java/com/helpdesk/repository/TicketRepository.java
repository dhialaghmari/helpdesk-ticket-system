package com.helpdesk.repository;

import com.helpdesk.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    /** Loads creator and assignee in the same query to avoid N+1 selects on lists. */
    @Override
    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    boolean existsByCreatedById(Long userId);

    boolean existsByAssignedToId(Long userId);

    @Query("select t.status, count(t) from Ticket t group by t.status")
    List<Object[]> countByStatus();
}
