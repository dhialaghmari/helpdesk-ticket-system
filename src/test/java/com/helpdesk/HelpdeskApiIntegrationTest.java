package com.helpdesk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HelpdeskApiIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "TestAdmin123!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    // ---------- helpers ----------

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String user, String password, Object body) throws Exception {
        request.with(httpBasic(user, password));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
        }
        return mvc.perform(request);
    }

    private JsonNode json(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** Registers a user with a random name and returns the created account (id, username...). */
    private JsonNode registerUser(String prefix) throws Exception {
        String username = prefix + UUID.randomUUID().toString().substring(0, 8);
        return json(mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", username + "@example.com",
                                "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER")));
    }

    private long createTicket(String user, String title) throws Exception {
        return json(call(post("/api/tickets"), user, PASSWORD,
                Map.of("title", title, "description", "Details for " + title, "priority", "HIGH", "category", "NETWORK"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN")))
                .get("id").asLong();
    }

    private void promote(long userId, String role) throws Exception {
        call(put("/api/users/" + userId + "/role"), ADMIN, ADMIN_PASSWORD, Map.of("role", role))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role));
    }

    // ---------- tests ----------

    @Test
    void registrationRejectsDuplicatesAndWeakInput() throws Exception {
        JsonNode user = registerUser("dup");
        String username = user.get("username").asText();

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "username", username, "email", "other@example.com", "password", PASSWORD))))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "username", "ok_name", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    void unauthenticatedAndWrongCredentialsAreRejected() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
        call(get("/api/tickets"), ADMIN, "wrong-password", null).andExpect(status().isUnauthorized());
    }

    @Test
    void userCreatesTicketAndOthersCannotSeeIt() throws Exception {
        String owner = registerUser("owner").get("username").asText();
        String stranger = registerUser("stranger").get("username").asText();
        long ticketId = createTicket(owner, "Printer offline");

        call(get("/api/tickets/" + ticketId), owner, PASSWORD, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Printer offline"))
                .andExpect(jsonPath("$.createdBy").value(owner));

        call(get("/api/tickets/" + ticketId), stranger, PASSWORD, null).andExpect(status().isForbidden());
        call(get("/api/tickets"), stranger, PASSWORD, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        call(get("/api/tickets"), owner, PASSWORD, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void ticketValidationAndDefaults() throws Exception {
        String user = registerUser("val").get("username").asText();

        call(post("/api/tickets"), user, PASSWORD, Map.of("title", " ", "description", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        call(post("/api/tickets"), user, PASSWORD, Map.of("title", "VPN", "description", "Cannot connect"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.category").value("OTHER"));

        call(get("/api/tickets/999999"), user, PASSWORD, null).andExpect(status().isNotFound());
        call(get("/api/tickets?status=NOPE"), user, PASSWORD, null).andExpect(status().isBadRequest());
    }

    @Test
    void onlyStaffCanChangeStatusAndAssign() throws Exception {
        String user = registerUser("req").get("username").asText();
        JsonNode agent = registerUser("agent");
        JsonNode plainUser = registerUser("plain");
        promote(agent.get("id").asLong(), "AGENT");
        String agentName = agent.get("username").asText();
        long ticketId = createTicket(user, "Laptop will not boot");

        call(patch("/api/tickets/" + ticketId + "/status"), user, PASSWORD, Map.of("status", "RESOLVED"))
                .andExpect(status().isForbidden());

        // assigning to a plain USER is invalid
        call(patch("/api/tickets/" + ticketId + "/assignee"), agentName, PASSWORD,
                Map.of("assigneeId", plainUser.get("id").asLong()))
                .andExpect(status().isBadRequest());

        // assigning to a staff member moves OPEN -> IN_PROGRESS
        call(patch("/api/tickets/" + ticketId + "/assignee"), agentName, PASSWORD,
                Map.of("assigneeId", agent.get("id").asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value(agentName))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        call(patch("/api/tickets/" + ticketId + "/status"), agentName, PASSWORD, Map.of("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // the agent sees tickets of other users
        call(get("/api/tickets/" + ticketId), agentName, PASSWORD, null).andExpect(status().isOk());
    }

    @Test
    void closedTicketsCannotBeEditedOrCommented() throws Exception {
        String user = registerUser("closed").get("username").asText();
        long ticketId = createTicket(user, "Old issue");

        call(patch("/api/tickets/" + ticketId + "/status"), ADMIN, ADMIN_PASSWORD, Map.of("status", "CLOSED"))
                .andExpect(status().isOk());

        call(put("/api/tickets/" + ticketId), user, PASSWORD, Map.of("title", "New", "description", "New"))
                .andExpect(status().isConflict());
        call(post("/api/tickets/" + ticketId + "/comments"), user, PASSWORD, Map.of("content", "hello?"))
                .andExpect(status().isConflict());
    }

    @Test
    void ownerCanEditOpenTicket() throws Exception {
        String user = registerUser("edit").get("username").asText();
        long ticketId = createTicket(user, "Wi-Fi drops");

        call(put("/api/tickets/" + ticketId), user, PASSWORD,
                Map.of("title", "Wi-Fi drops every hour", "description", "More details", "priority", "CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Wi-Fi drops every hour"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"));
    }

    @Test
    void commentsAreVisibleToOwnerAndStaffOnly() throws Exception {
        String owner = registerUser("cown").get("username").asText();
        String stranger = registerUser("cstr").get("username").asText();
        long ticketId = createTicket(owner, "Mailbox full");

        call(post("/api/tickets/" + ticketId + "/comments"), owner, PASSWORD, Map.of("content", "Please help"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value(owner));
        call(post("/api/tickets/" + ticketId + "/comments"), ADMIN, ADMIN_PASSWORD, Map.of("content", "On it"))
                .andExpect(status().isCreated());

        call(get("/api/tickets/" + ticketId + "/comments"), owner, PASSWORD, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Please help"));

        call(get("/api/tickets/" + ticketId + "/comments"), stranger, PASSWORD, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminCanDeleteTicketsAndManageUsers() throws Exception {
        String user = registerUser("del").get("username").asText();
        long ticketId = createTicket(user, "Delete me");

        call(delete("/api/tickets/" + ticketId), user, PASSWORD, null).andExpect(status().isForbidden());
        call(get("/api/users"), user, PASSWORD, null).andExpect(status().isForbidden());
        call(get("/api/users"), ADMIN, ADMIN_PASSWORD, null).andExpect(status().isOk());

        call(delete("/api/tickets/" + ticketId), ADMIN, ADMIN_PASSWORD, null).andExpect(status().isNoContent());
        call(get("/api/tickets/" + ticketId), ADMIN, ADMIN_PASSWORD, null).andExpect(status().isNotFound());
    }

    @Test
    void adminCannotChangeOwnRoleOrDeleteUserWithTickets() throws Exception {
        long adminId = json(call(get("/api/users/me"), ADMIN, ADMIN_PASSWORD, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))).get("id").asLong();

        call(put("/api/users/" + adminId + "/role"), ADMIN, ADMIN_PASSWORD, Map.of("role", "USER"))
                .andExpect(status().isBadRequest());

        JsonNode user = registerUser("busy");
        createTicket(user.get("username").asText(), "Has a ticket");
        call(delete("/api/users/" + user.get("id").asLong()), ADMIN, ADMIN_PASSWORD, null)
                .andExpect(status().isConflict());

        JsonNode idle = registerUser("idle");
        call(delete("/api/users/" + idle.get("id").asLong()), ADMIN, ADMIN_PASSWORD, null)
                .andExpect(status().isNoContent());
    }

    @Test
    void statisticsAreRestrictedToStaffAndFilterWorks() throws Exception {
        String user = registerUser("stat").get("username").asText();
        long ticketId = createTicket(user, "Stats ticket");

        call(get("/api/tickets/stats"), user, PASSWORD, null).andExpect(status().isForbidden());
        call(get("/api/tickets/stats"), ADMIN, ADMIN_PASSWORD, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.OPEN").isNumber())
                .andExpect(jsonPath("$.CLOSED").isNumber());

        call(get("/api/tickets?status=OPEN&priority=HIGH&category=NETWORK&sort=createdAt,desc"),
                ADMIN, ADMIN_PASSWORD, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + ticketId + ")]").isNotEmpty());
    }

    @Test
    void invalidStatusTransitionsAreRejected() throws Exception {
        String user = registerUser("flow").get("username").asText();
        long ticketId = createTicket(user, "Transition check");

        // OPEN -> RESOLVED is not allowed (must go through IN_PROGRESS)
        call(patch("/api/tickets/" + ticketId + "/status"), ADMIN, ADMIN_PASSWORD, Map.of("status", "RESOLVED"))
                .andExpect(status().isConflict());
        call(patch("/api/tickets/" + ticketId + "/status"), ADMIN, ADMIN_PASSWORD, Map.of("status", "IN_PROGRESS"))
                .andExpect(status().isOk());
        call(patch("/api/tickets/" + ticketId + "/status"), ADMIN, ADMIN_PASSWORD, Map.of("status", "CLOSED"))
                .andExpect(status().isOk());
        // CLOSED is final
        call(patch("/api/tickets/" + ticketId + "/status"), ADMIN, ADMIN_PASSWORD, Map.of("status", "OPEN"))
                .andExpect(status().isConflict());
    }

    @Test
    void weakAdminPasswordsAreRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> com.helpdesk.config.DataInitializer.validateAdminPassword("Admin@12345"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> com.helpdesk.config.DataInitializer.validateAdminPassword(""));
        com.helpdesk.config.DataInitializer.validateAdminPassword("ChangeThisToAStrongPassword!");
    }
}
