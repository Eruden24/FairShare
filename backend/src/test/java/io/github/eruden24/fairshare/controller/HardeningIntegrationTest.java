package io.github.eruden24.fairshare.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase 9: errors look the same everywhere, logins are rate-limited, stale edits are refused. */
class HardeningIntegrationTest extends ApiFlowTestSupport {

    @Test
    void missingOrBadTokenReturnsProblemDetails() throws Exception {
        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.instance").value("/api/groups"));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidQueryParametersUseTheSameErrorsShape() throws Exception {
        User anna = registerAndLogin("hardening-params-anna@example.com", "Anna");
        String groupId = createGroup(anna, "EUR");

        mockMvc.perform(get("/api/groups/" + groupId + "/expenses").param("size", "1000")
                        .header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.size").exists());
    }

    @Test
    void tooManyFailedLoginsAreRefusedForAWhile() throws Exception {
        registerAndLogin("hardening-login@example.com", "Login");
        String wrong = """
                {"email": "hardening-login@example.com", "password": "wrong-password"}
                """;

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(json(post("/api/auth/login"), wrong)).andExpect(status().isUnauthorized());
        }

        // Even the right password is refused now
        mockMvc.perform(json(post("/api/auth/login"), """
                        {"email": "hardening-login@example.com", "password": "password123"}
                        """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("\\d+")))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void editingWithAnOldVersionIsRefused() throws Exception {
        User anna = registerAndLogin("hardening-version-anna@example.com", "Anna");
        String groupId = createGroup(anna, "EUR");
        String expenseId = createExpense(anna, groupId, "Dinner", "10.00", anna, "2026-10-01", anna);
        String url = "/api/groups/" + groupId + "/expenses/" + expenseId;

        int version = JsonPath.read(mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andReturn().getResponse().getContentAsString(), "$.version");
        String edit = """
                {"description": "Dinner", "amount": %s, "paidById": "%s",
                 "participants": [{"userId": "%s"}], "version": %d}
                """;

        // First edit with the current version works and returns a newer version
        mockMvc.perform(json(put(url), edit.formatted("12.00", anna.id(), anna.id(), version))
                        .header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(greaterThan(version)));

        // A second edit still holding the old version would overwrite the first one: refused
        mockMvc.perform(json(put(url), edit.formatted("15.00", anna.id(), anna.id(), version))
                        .header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isConflict());

        mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(jsonPath("$.amount").value(12.00));
    }

    @Test
    void editThatOnlyChangesSharesStillChangesTheVersion() throws Exception {
        User anna = registerAndLogin("hardening-shares-anna@example.com", "Anna");
        User bob = registerAndLogin("hardening-shares-bob@example.com", "Bob");
        String groupId = createGroup(anna, "EUR");
        addMember(anna, groupId, bob);
        String url = "/api/groups/" + groupId + "/expenses";
        String body = """
                {"description": "Cabin", "amount": 90, "paidById": "%s", "splitType": "SHARES", "date": "2026-10-01",
                 "participants": [{"userId": "%s", "value": %d}, {"userId": "%s", "value": %d}]}
                """;

        String created = mockMvc.perform(json(post(url), body.formatted(anna.id(), anna.id(), 2, bob.id(), 1))
                        .header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String expenseId = JsonPath.read(created, "$.id");
        int version = JsonPath.read(created, "$.version");

        // Same description, amount, payer, date and type: only the shares swap
        mockMvc.perform(json(put(url + "/" + expenseId), body.formatted(anna.id(), anna.id(), 1, bob.id(), 2))
                        .header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(greaterThan(version)));
    }
}
