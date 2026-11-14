package com.helpdesk.entity;

public enum Role {
    /** Full access: user management and ticket deletion. */
    ADMIN,
    /** Support staff: handles all tickets (status, assignment, comments). */
    AGENT,
    /** End user: creates and follows their own tickets. */
    USER
}
