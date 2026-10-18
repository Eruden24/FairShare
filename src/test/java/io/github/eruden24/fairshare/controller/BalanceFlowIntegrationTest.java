package io.github.eruden24.fairshare.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BalanceFlowIntegrationTest extends ApiFlowTestSupport {

    @Test
    void balancesFollowTheExpenses() throws Exception {
        User anna = registerAndLogin("balance-anna@example.com", "Anna");
        User bob = registerAndLogin("balance-bob@example.com", "Bob");
        User carol = registerAndLogin("balance-carol@example.com", "Carol");
        User eve = registerAndLogin("balance-eve@example.com", "Eve");
        String groupId = createGroup(anna, "EUR");
        addMember(anna, groupId, bob);
        addMember(anna, groupId, carol);
        String balances = "/api/groups/" + groupId + "/balances";

        // No expenses yet: everyone is at zero
        mockMvc.perform(get(balances).header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balances", hasSize(3)))
                .andExpect(jsonPath("$.balances[*].balance").value(contains(0.00, 0.00, 0.00)));

        // Anna pays a 30.00 dinner, Bob a 60.00 taxi, both for all three
        createExpense(anna, groupId, "Dinner", "30.00", anna, "2026-10-01", anna, bob, carol);
        String taxiId = createExpense(bob, groupId, "Taxi", "60.00", bob, "2026-10-02", anna, bob, carol);

        // Bob +30, Anna 0, Carol -30: creditors first
        String body = mockMvc.perform(get(balances).header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances[*].displayName").value(contains("Bob", "Anna", "Carol")))
                .andExpect(jsonPath("$.balances[*].paid").value(contains(60.00, 30.00, 0.00)))
                .andExpect(jsonPath("$.balances[*].owed").value(contains(30.00, 30.00, 30.00)))
                .andExpect(jsonPath("$.balances[*].balance").value(contains(30.00, 0.00, -30.00)))
                .andReturn().getResponse().getContentAsString();

        List<Number> all = JsonPath.read(body, "$.balances[*].balance");
        assertThat(all.stream().map(n -> new BigDecimal(n.toString())).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("0");

        // My balances, across my groups
        mockMvc.perform(get("/api/users/me/balances").header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].groupId").value(groupId))
                .andExpect(jsonPath("$[0].balance").value(-30.00));

        // Deleting the taxi changes the balances straight away: they are computed, never stored
        mockMvc.perform(delete("/api/groups/" + groupId + "/expenses/" + taxiId).header(HttpHeaders.AUTHORIZATION, bob.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(balances).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(jsonPath("$.balances[*].displayName").value(contains("Anna", "Bob", "Carol")))
                .andExpect(jsonPath("$.balances[*].balance").value(contains(20.00, -10.00, -10.00)));

        // Outsiders can't see the balances
        mockMvc.perform(get(balances).header(HttpHeaders.AUTHORIZATION, eve.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/users/me/balances").header(HttpHeaders.AUTHORIZATION, eve.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void someoneWhoLeftKeepsTheirBalanceInTheGroup() throws Exception {
        User anna = registerAndLogin("balance-left-anna@example.com", "Anna");
        User bob = registerAndLogin("balance-left-bob@example.com", "Bob");
        String groupId = createGroup(anna, "EUR");
        addMember(anna, groupId, bob);

        createExpense(anna, groupId, "Dinner", "20.00", anna, "2026-10-01", anna, bob);
        mockMvc.perform(delete("/api/groups/" + groupId + "/members/me").header(HttpHeaders.AUTHORIZATION, bob.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/groups/" + groupId + "/balances").header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(jsonPath("$.balances[*].displayName").value(contains("Anna", "Bob")))
                .andExpect(jsonPath("$.balances[*].member").value(contains(true, false)))
                .andExpect(jsonPath("$.balances[*].balance").value(contains(10.00, -10.00)));
    }
}
