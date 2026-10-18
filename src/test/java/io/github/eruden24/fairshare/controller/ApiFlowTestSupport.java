package io.github.eruden24.fairshare.controller;

import com.jayway.jsonpath.JsonPath;
import io.github.eruden24.fairshare.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base for HTTP flow tests: a real app on a throwaway PostgreSQL, plus helpers that register users,
 * create groups and add expenses through the API. The database is shared by all @SpringBootTest classes
 * and not rolled back, so every test must use its own emails.
 */
@SpringBootTest(properties = "fairshare.jwt.secret=test-secret-that-is-at-least-32-characters")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
abstract class ApiFlowTestSupport {

    @Autowired
    MockMvc mockMvc;

    /** A logged-in user: their id and the ready-to-send "Bearer ..." header value. */
    record User(String id, String token) {}

    User registerAndLogin(String email, String displayName) throws Exception {
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

    String createGroup(User owner, String currency) throws Exception {
        return JsonPath.read(mockMvc.perform(json(post("/api/groups"), """
                                {"name": "Trip", "currency": "%s"}
                                """.formatted(currency)).header(HttpHeaders.AUTHORIZATION, owner.token()))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(),
                "$.id");
    }

    void addMember(User owner, String groupId, User member) throws Exception {
        String email = JsonPath.read(mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, member.token()))
                .andReturn().getResponse().getContentAsString(), "$.email");

        mockMvc.perform(json(post("/api/groups/" + groupId + "/members"), """
                        {"email": "%s"}
                        """.formatted(email)).header(HttpHeaders.AUTHORIZATION, owner.token()))
                .andExpect(status().isCreated());
    }

    /** An expense split equally between the participants. Returns its id. */
    String createExpense(User caller, String groupId, String description, String amount, User payer,
                         String date, User... participants) throws Exception {
        String participantsJson = Arrays.stream(participants)
                .map(p -> "{\"userId\": \"" + p.id() + "\"}")
                .collect(Collectors.joining(", "));

        return JsonPath.read(mockMvc.perform(json(post("/api/groups/" + groupId + "/expenses"), """
                                {"description": "%s", "amount": %s, "paidById": "%s", "date": "%s", "participants": [%s]}
                                """.formatted(description, amount, payer.id(), date, participantsJson))
                                .header(HttpHeaders.AUTHORIZATION, caller.token()))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(),
                "$.id");
    }

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
