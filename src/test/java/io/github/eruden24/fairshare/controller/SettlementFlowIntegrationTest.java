package io.github.eruden24.fairshare.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettlementFlowIntegrationTest extends ApiFlowTestSupport {

    @Test
    void settlingUpBringsEveryoneToZero() throws Exception {
        User anna = registerAndLogin("settle-anna@example.com", "Anna");
        User bob = registerAndLogin("settle-bob@example.com", "Bob");
        User carol = registerAndLogin("settle-carol@example.com", "Carol");
        User eve = registerAndLogin("settle-eve@example.com", "Eve");
        String groupId = createGroup(anna, "EUR");
        addMember(anna, groupId, bob);
        addMember(anna, groupId, carol);
        String settlements = "/api/groups/" + groupId + "/settlements";

        // Anna pays 30.00, Bob pays 60.00, both for all three: Bob +30, Anna 0, Carol -30
        createExpense(anna, groupId, "Dinner", "30.00", anna, "2026-10-01", anna, bob, carol);
        createExpense(bob, groupId, "Taxi", "60.00", bob, "2026-10-02", anna, bob, carol);

        // One transfer is enough
        mockMvc.perform(get(settlements + "/suggested").header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fromDisplayName").value("Carol"))
                .andExpect(jsonPath("$[0].toDisplayName").value("Bob"))
                .andExpect(jsonPath("$[0].amount").value(30.00));

        // Phase 7: Carol owes money, so she can't leave and the group can't be deleted
        mockMvc.perform(delete("/api/groups/" + groupId + "/members/me").header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("settle up first")));
        mockMvc.perform(delete("/api/groups/" + groupId).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isConflict());

        // Anna (keeping the books) records that Carol paid Bob 30.00
        String settlementId = JsonPath.read(mockMvc.perform(json(post(settlements), """
                                {"fromUserId": "%s", "toUserId": "%s", "amount": 30.00, "date": "2026-10-05"}
                                """.formatted(carol.id(), bob.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.fromDisplayName").value("Carol"))
                        .andExpect(jsonPath("$.toDisplayName").value("Bob"))
                        .andExpect(jsonPath("$.amount").value(30.00))
                        .andExpect(jsonPath("$.currency").value("EUR"))
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        mockMvc.perform(get("/api/groups/" + groupId + "/balances").header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(jsonPath("$.balances[*].balance").value(everyItem(is(0.00))))
                .andExpect(jsonPath("$.balances[?(@.displayName == 'Carol')].sent").value(30.00))
                .andExpect(jsonPath("$.balances[?(@.displayName == 'Bob')].received").value(30.00));
        mockMvc.perform(get(settlements + "/suggested").header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get(settlements).header(HttpHeaders.AUTHORIZATION, bob.token()))
                .andExpect(jsonPath("$[*].id").value(contains(settlementId)));

        // Outsiders see nothing; only the recorder or the owner can delete
        mockMvc.perform(get(settlements).header(HttpHeaders.AUTHORIZATION, eve.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(settlements + "/" + settlementId).header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isForbidden());

        // Everyone is settled: Carol can leave now, and the group can be deleted
        mockMvc.perform(delete("/api/groups/" + groupId + "/members/me").header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/groups/" + groupId).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletingASettlementReopensTheDebt() throws Exception {
        User anna = registerAndLogin("settle-undo-anna@example.com", "Anna");
        User bob = registerAndLogin("settle-undo-bob@example.com", "Bob");
        String groupId = createGroup(anna, "EUR");
        addMember(anna, groupId, bob);
        createExpense(anna, groupId, "Dinner", "20.00", anna, "2026-10-01", anna, bob);

        String settlementId = JsonPath.read(mockMvc.perform(json(post("/api/groups/" + groupId + "/settlements"), """
                                {"fromUserId": "%s", "toUserId": "%s", "amount": 10}
                                """.formatted(bob.id(), anna.id())).header(HttpHeaders.AUTHORIZATION, bob.token()))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        mockMvc.perform(delete("/api/groups/" + groupId + "/settlements/" + settlementId)
                        .header(HttpHeaders.AUTHORIZATION, bob.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/groups/" + groupId + "/balances").header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(jsonPath("$.balances[*].balance").value(contains(10.00, -10.00)));
        // The owner can't remove Bob while he owes
        mockMvc.perform(delete("/api/groups/" + groupId + "/members/" + bob.id()).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidSettlementsReturn400() throws Exception {
        User anna = registerAndLogin("settle-invalid-anna@example.com", "Anna");
        User eve = registerAndLogin("settle-invalid-eve@example.com", "Eve");
        String settlements = "/api/groups/" + createGroup(anna, "EUR") + "/settlements";

        mockMvc.perform(json(post(settlements), """
                        {"fromUserId": "%s", "toUserId": "%s", "amount": 5}
                        """.formatted(anna.id(), anna.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.toUserId").value("must be different from fromUserId"));

        mockMvc.perform(json(post(settlements), """
                        {"fromUserId": "%s", "toUserId": "%s", "amount": 5}
                        """.formatted(eve.id(), anna.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fromUserId").value("is not a member of this group"));

        mockMvc.perform(json(post(settlements), """
                        {"fromUserId": "%s", "toUserId": "%s", "amount": 0}
                        """.formatted(anna.id(), eve.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }
}
