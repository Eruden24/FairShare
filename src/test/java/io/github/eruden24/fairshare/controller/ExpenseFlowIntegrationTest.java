package io.github.eruden24.fairshare.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseFlowIntegrationTest extends ApiFlowTestSupport {

    @Test
    void membersAddAndViewExpensesSplitEqually() throws Exception {
        User anna = registerAndLogin("expense-anna@example.com", "Anna");
        User bob = registerAndLogin("expense-bob@example.com", "Bob");
        User carol = registerAndLogin("expense-carol@example.com", "Carol");
        User eve = registerAndLogin("expense-eve@example.com", "Eve");
        String groupId = createGroup(anna, "EUR");
        addMember(anna, groupId, bob);
        addMember(anna, groupId, carol);

        // Bob (a plain member) adds a 10.00 dinner that Anna paid, split between all three. No splitType = EQUAL.
        String body = mockMvc.perform(json(post("/api/groups/" + groupId + "/expenses"), """
                        {"description": "Dinner", "amount": 10.00, "paidById": "%s", "date": "2026-10-01",
                         "participants": [{"userId": "%s"}, {"userId": "%s"}, {"userId": "%s"}]}
                        """.formatted(anna.id(), anna.id(), bob.id(), carol.id())).header(HttpHeaders.AUTHORIZATION, bob.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(10.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.splitType").value("EQUAL"))
                .andExpect(jsonPath("$.paidById").value(anna.id()))
                .andExpect(jsonPath("$.createdById").value(bob.id()))
                .andExpect(jsonPath("$.date").value("2026-10-01"))
                .andExpect(jsonPath("$.splits", hasSize(3)))
                .andReturn().getResponse().getContentAsString();

        List<Number> shares = JsonPath.read(body, "$.splits[*].amount");
        assertThat(shares).extracting(Number::toString).containsExactlyInAnyOrder("3.34", "3.33", "3.33");
        assertThat(shares.stream().map(n -> new BigDecimal(n.toString())).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("10.00");

        String expenseId = JsonPath.read(body, "$.id");

        // Any member can read it, outsiders can't see it at all
        mockMvc.perform(get("/api/groups/" + groupId + "/expenses/" + expenseId).header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Dinner"));

        mockMvc.perform(get("/api/groups/" + groupId + "/expenses/" + expenseId).header(HttpHeaders.AUTHORIZATION, eve.token()))
                .andExpect(status().isNotFound());

        mockMvc.perform(json(post("/api/groups/" + groupId + "/expenses"), """
                        {"description": "Sneaky", "amount": 5, "paidById": "%s", "participants": [{"userId": "%s"}]}
                        """.formatted(eve.id(), eve.id())).header(HttpHeaders.AUTHORIZATION, eve.token()))
                .andExpect(status().isNotFound());

        // The expense doesn't exist under another group
        String otherGroupId = createGroup(anna, "EUR");
        mockMvc.perform(get("/api/groups/" + otherGroupId + "/expenses/" + expenseId).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isNotFound());

        // Deleting the group deletes its expenses too
        mockMvc.perform(delete("/api/groups/" + groupId).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void splitsByExactAmountsPercentagesAndShares() throws Exception {
        User anna = registerAndLogin("split-anna@example.com", "Anna");
        User bob = registerAndLogin("split-bob@example.com", "Bob");
        String groupId = createGroup(anna, "EUR");
        addMember(anna, groupId, bob);
        String expenses = "/api/groups/" + groupId + "/expenses";

        mockMvc.perform(json(post(expenses), """
                        {"description": "Groceries", "amount": 10.00, "paidById": "%s", "splitType": "EXACT",
                         "participants": [{"userId": "%s", "value": 7.50}, {"userId": "%s", "value": 2.50}]}
                        """.formatted(anna.id(), anna.id(), bob.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.splitType").value("EXACT"))
                .andExpect(jsonPath("$.splits[?(@.userId == '%s')].amount".formatted(anna.id())).value(7.50))
                .andExpect(jsonPath("$.splits[*].value").value(everyItem(nullValue())));

        mockMvc.perform(json(post(expenses), """
                        {"description": "Rent", "amount": 1000, "paidById": "%s", "splitType": "PERCENTAGE",
                         "participants": [{"userId": "%s", "value": 60}, {"userId": "%s", "value": 40}]}
                        """.formatted(anna.id(), anna.id(), bob.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.splits[?(@.userId == '%s')].amount".formatted(bob.id())).value(400.00))
                .andExpect(jsonPath("$.splits[?(@.userId == '%s')].value".formatted(bob.id())).value(40));

        mockMvc.perform(json(post(expenses), """
                        {"description": "Cabin", "amount": 300, "paidById": "%s", "splitType": "SHARES",
                         "participants": [{"userId": "%s", "value": 2}, {"userId": "%s", "value": 1}]}
                        """.formatted(bob.id(), anna.id(), bob.id())).header(HttpHeaders.AUTHORIZATION, bob.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.splits[?(@.userId == '%s')].amount".formatted(anna.id())).value(200.00))
                .andExpect(jsonPath("$.splits[?(@.userId == '%s')].value".formatted(anna.id())).value(2));

        // Values that don't add up are rejected on "participants"
        mockMvc.perform(json(post(expenses), """
                        {"description": "Rent", "amount": 1000, "paidById": "%s", "splitType": "PERCENTAGE",
                         "participants": [{"userId": "%s", "value": 60}, {"userId": "%s", "value": 30}]}
                        """.formatted(anna.id(), anna.id(), bob.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.participants").value("percentages add up to 90 but must add up to 100"));

        mockMvc.perform(json(post(expenses), """
                        {"description": "Oops", "amount": 10, "paidById": "%s", "splitType": "HALVES",
                         "participants": [{"userId": "%s"}]}
                        """.formatted(anna.id(), anna.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsPagesFiltersEditsAndDeletes() throws Exception {
        User anna = registerAndLogin("list-anna@example.com", "Anna");
        User bob = registerAndLogin("list-bob@example.com", "Bob");
        User carol = registerAndLogin("list-carol@example.com", "Carol");
        String groupId = createGroup(anna, "EUR");
        addMember(anna, groupId, bob);
        addMember(anna, groupId, carol);
        String expenses = "/api/groups/" + groupId + "/expenses";

        createExpense(bob, groupId, "Breakfast", "12.00", bob, "2026-09-01", anna, bob);
        String lunchId = createExpense(bob, groupId, "Lunch", "20.00", bob, "2026-09-02", anna, bob);
        createExpense(anna, groupId, "Dinner", "30.00", anna, "2026-09-03", anna, bob);

        // Newest first, 2 per page
        mockMvc.perform(get(expenses).param("size", "2").header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].description").value(contains("Dinner", "Lunch")))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get(expenses).param("size", "2").param("page", "1").header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(jsonPath("$.content[*].description").value(contains("Breakfast")));

        // Filters: date range (inclusive) and payer
        mockMvc.perform(get(expenses).param("from", "2026-09-02").param("paidById", bob.id())
                        .header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(jsonPath("$.content[*].description").value(contains("Lunch")));

        mockMvc.perform(get(expenses).param("from", "2026-09-03").param("to", "2026-09-01")
                        .header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.to").exists());

        mockMvc.perform(get(expenses).param("size", "1000").header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isBadRequest());

        // Carol didn't add Lunch and isn't the owner: she can't edit or delete it
        String newLunch = """
                {"description": "Long lunch", "amount": 25.00, "paidById": "%s",
                 "participants": [{"userId": "%s"}, {"userId": "%s"}, {"userId": "%s"}]}
                """.formatted(bob.id(), anna.id(), bob.id(), carol.id());
        mockMvc.perform(json(put(expenses + "/" + lunchId), newLunch).header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(expenses + "/" + lunchId).header(HttpHeaders.AUTHORIZATION, carol.token()))
                .andExpect(status().isForbidden());

        // Bob added it, so he can replace it: new amount, new participants, re-split
        mockMvc.perform(json(put(expenses + "/" + lunchId), newLunch).header(HttpHeaders.AUTHORIZATION, bob.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Long lunch"))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.splits", hasSize(3)));

        // Anna is the owner, so she can delete anyone's expense
        mockMvc.perform(delete(expenses + "/" + lunchId).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(expenses + "/" + lunchId).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(expenses).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void invalidExpensesReturn400() throws Exception {
        User anna = registerAndLogin("expense-invalid-anna@example.com", "Anna");
        User eve = registerAndLogin("expense-invalid-eve@example.com", "Eve");
        String groupId = createGroup(anna, "EUR");
        String expenses = "/api/groups/" + groupId + "/expenses";

        // Bean validation: missing and malformed fields
        mockMvc.perform(json(post(expenses), """
                        {"description": "", "amount": -5, "participants": []}
                        """).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.paidById").exists())
                .andExpect(jsonPath("$.errors.participants").exists());

        // Too many decimals for EUR
        mockMvc.perform(json(post(expenses), """
                        {"description": "Coffee", "amount": 12.505, "paidById": "%s", "participants": [{"userId": "%s"}]}
                        """.formatted(anna.id(), anna.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("must have at most 2 decimals in EUR"));

        // Payer and participants must be members
        mockMvc.perform(json(post(expenses), """
                        {"description": "Coffee", "amount": 3, "paidById": "%s", "participants": [{"userId": "%s"}]}
                        """.formatted(eve.id(), anna.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.paidById").value("is not a member of this group"));

        mockMvc.perform(json(post(expenses), """
                        {"description": "Coffee", "amount": 3, "paidById": "%s",
                         "participants": [{"userId": "%s"}, {"userId": "%s"}]}
                        """.formatted(anna.id(), anna.id(), eve.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.participants").exists());
    }

    @Test
    void currenciesWithoutDecimalsSplitInWholeUnits() throws Exception {
        User anna = registerAndLogin("expense-yen-anna@example.com", "Anna");
        User bob = registerAndLogin("expense-yen-bob@example.com", "Bob");
        String groupId = createGroup(anna, "JPY");
        addMember(anna, groupId, bob);

        String body = mockMvc.perform(json(post("/api/groups/" + groupId + "/expenses"), """
                        {"description": "Ramen", "amount": 1001, "paidById": "%s",
                         "participants": [{"userId": "%s"}, {"userId": "%s"}]}
                        """.formatted(anna.id(), anna.id(), bob.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        List<Number> shares = JsonPath.read(body, "$.splits[*].amount");
        assertThat(shares).extracting(Number::toString).containsExactlyInAnyOrder("501", "500");

        mockMvc.perform(json(post("/api/groups/" + groupId + "/expenses"), """
                        {"description": "Tip", "amount": 10.5, "paidById": "%s", "participants": [{"userId": "%s"}]}
                        """.formatted(anna.id(), anna.id())).header(HttpHeaders.AUTHORIZATION, anna.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("must have at most 0 decimals in JPY"));
    }
}
