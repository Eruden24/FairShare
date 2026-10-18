package io.github.eruden24.fairshare.controller;

import com.jayway.jsonpath.JsonPath;
import io.github.eruden24.fairshare.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shares the database with the other @SpringBootTest classes, so every test uses its own emails.
 */
@SpringBootTest(properties = "fairshare.jwt.secret=test-secret-that-is-at-least-32-characters")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExpenseFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void membersAddAndViewExpensesSplitEqually() throws Exception {
        User anna = registerAndLogin("expense-anna@example.com", "Anna");
        User bob = registerAndLogin("expense-bob@example.com", "Bob");
        User carol = registerAndLogin("expense-carol@example.com", "Carol");
        User eve = registerAndLogin("expense-eve@example.com", "Eve");
        String groupId = createGroup(anna, "EUR");
        addMember(anna, groupId, bob);
        addMember(anna, groupId, carol);

        // Bob (a plain member) adds a 10.00 dinner that Anna paid, split between all three
        String body = mockMvc.perform(json(post("/api/groups/" + groupId + "/expenses"), """
                        {"description": "Dinner", "amount": 10.00, "paidById": "%s",
                         "participantIds": ["%s", "%s", "%s"], "date": "2026-10-01"}
                        """.formatted(anna.id, anna.id, bob.id, carol.id)).header(HttpHeaders.AUTHORIZATION, bob.token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(10.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.paidById").value(anna.id))
                .andExpect(jsonPath("$.createdById").value(bob.id))
                .andExpect(jsonPath("$.date").value("2026-10-01"))
                .andExpect(jsonPath("$.splits", hasSize(3)))
                .andReturn().getResponse().getContentAsString();

        List<Number> shares = JsonPath.read(body, "$.splits[*].amount");
        assertThat(shares).extracting(Number::toString).containsExactlyInAnyOrder("3.34", "3.33", "3.33");
        assertThat(shares.stream().map(n -> new BigDecimal(n.toString())).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("10.00");

        String expenseId = JsonPath.read(body, "$.id");

        // Any member can read it, outsiders can't see it at all
        mockMvc.perform(get("/api/groups/" + groupId + "/expenses/" + expenseId).header(HttpHeaders.AUTHORIZATION, carol.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Dinner"));

        mockMvc.perform(get("/api/groups/" + groupId + "/expenses/" + expenseId).header(HttpHeaders.AUTHORIZATION, eve.token))
                .andExpect(status().isNotFound());

        mockMvc.perform(json(post("/api/groups/" + groupId + "/expenses"), """
                        {"description": "Sneaky", "amount": 5, "paidById": "%s", "participantIds": ["%s"]}
                        """.formatted(eve.id, eve.id)).header(HttpHeaders.AUTHORIZATION, eve.token))
                .andExpect(status().isNotFound());

        // The expense doesn't exist under another group
        String otherGroupId = createGroup(anna, "EUR");
        mockMvc.perform(get("/api/groups/" + otherGroupId + "/expenses/" + expenseId).header(HttpHeaders.AUTHORIZATION, anna.token))
                .andExpect(status().isNotFound());

        // Deleting the group deletes its expenses too
        mockMvc.perform(delete("/api/groups/" + groupId).header(HttpHeaders.AUTHORIZATION, anna.token))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidExpensesReturn400() throws Exception {
        User anna = registerAndLogin("expense-invalid-anna@example.com", "Anna");
        User eve = registerAndLogin("expense-invalid-eve@example.com", "Eve");
        String groupId = createGroup(anna, "EUR");
        String expenses = "/api/groups/" + groupId + "/expenses";

        // Bean validation: missing and malformed fields
        mockMvc.perform(json(post(expenses), """
                        {"description": "", "amount": -5, "participantIds": []}
                        """).header(HttpHeaders.AUTHORIZATION, anna.token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.paidById").exists())
                .andExpect(jsonPath("$.errors.participantIds").exists());

        // Too many decimals for EUR
        mockMvc.perform(json(post(expenses), """
                        {"description": "Coffee", "amount": 12.505, "paidById": "%s", "participantIds": ["%s"]}
                        """.formatted(anna.id, anna.id)).header(HttpHeaders.AUTHORIZATION, anna.token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("must have at most 2 decimals in EUR"));

        // Payer and participants must be members
        mockMvc.perform(json(post(expenses), """
                        {"description": "Coffee", "amount": 3, "paidById": "%s", "participantIds": ["%s"]}
                        """.formatted(eve.id, anna.id)).header(HttpHeaders.AUTHORIZATION, anna.token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.paidById").value("is not a member of this group"));

        mockMvc.perform(json(post(expenses), """
                        {"description": "Coffee", "amount": 3, "paidById": "%s", "participantIds": ["%s", "%s"]}
                        """.formatted(anna.id, anna.id, eve.id)).header(HttpHeaders.AUTHORIZATION, anna.token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.participantIds").exists());
    }

    @Test
    void currenciesWithoutDecimalsSplitInWholeUnits() throws Exception {
        User anna = registerAndLogin("expense-yen-anna@example.com", "Anna");
        User bob = registerAndLogin("expense-yen-bob@example.com", "Bob");
        String groupId = createGroup(anna, "JPY");
        addMember(anna, groupId, bob);

        String body = mockMvc.perform(json(post("/api/groups/" + groupId + "/expenses"), """
                        {"description": "Ramen", "amount": 1001, "paidById": "%s", "participantIds": ["%s", "%s"]}
                        """.formatted(anna.id, anna.id, bob.id)).header(HttpHeaders.AUTHORIZATION, anna.token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        List<Number> shares = JsonPath.read(body, "$.splits[*].amount");
        assertThat(shares).extracting(Number::toString).containsExactlyInAnyOrder("501", "500");

        mockMvc.perform(json(post("/api/groups/" + groupId + "/expenses"), """
                        {"description": "Tip", "amount": 10.5, "paidById": "%s", "participantIds": ["%s"]}
                        """.formatted(anna.id, anna.id)).header(HttpHeaders.AUTHORIZATION, anna.token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("must have at most 0 decimals in JPY"));
    }

    private record User(String id, String token) {}

    private String createGroup(User owner, String currency) throws Exception {
        return JsonPath.read(mockMvc.perform(json(post("/api/groups"), """
                                {"name": "Trip", "currency": "%s"}
                                """.formatted(currency)).header(HttpHeaders.AUTHORIZATION, owner.token))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(),
                "$.id");
    }

    private void addMember(User owner, String groupId, User member) throws Exception {
        String email = JsonPath.read(mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, member.token))
                .andReturn().getResponse().getContentAsString(), "$.email");

        mockMvc.perform(json(post("/api/groups/" + groupId + "/members"), """
                        {"email": "%s"}
                        """.formatted(email)).header(HttpHeaders.AUTHORIZATION, owner.token))
                .andExpect(status().isCreated());
    }

    private User registerAndLogin(String email, String displayName) throws Exception {
        String id = JsonPath.read(mockMvc.perform(json(post("/api/users"), """
                                {"email": "%s", "password": "password123", "displayName": "%s"}
                                """.formatted(email, displayName)))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        String body = mockMvc.perform(json(post("/api/auth/login"), """
                        {"email": "%s", "password": "password123"}
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return new User(id, "Bearer " + JsonPath.read(body, "$.accessToken"));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
