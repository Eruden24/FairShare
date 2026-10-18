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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "fairshare.jwt.secret=test-secret-that-is-at-least-32-characters")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GroupFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mockMvc.perform(get("/api/groups")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginFailsWithSameMessageForUnknownEmailAndWrongPassword() throws Exception {
        register("carol@example.com", "Carol");

        mockMvc.perform(json(post("/api/auth/login"), """
                        {"email": "carol@example.com", "password": "wrong-password"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));

        mockMvc.perform(json(post("/api/auth/login"), """
                        {"email": "nobody@example.com", "password": "password123"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void badInputReturns400() throws Exception {
        String token = registerAndLogin("dave@example.com", "Dave");

        mockMvc.perform(json(post("/api/groups"), """
                        {"name": "", "currency": "euro"}
                        """).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.currency").exists());

        mockMvc.perform(get("/api/groups/not-a-uuid").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(json(post("/api/groups"), "{ not json").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullGroupLifecycle() throws Exception {
        String alice = registerAndLogin("alice@example.com", "Alice");
        String bob = registerAndLogin("Bob@Example.com", "Bob");
        String eve = registerAndLogin("eve@example.com", "Eve");

        String bobId = JsonPath.read(
                mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bob))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email").value("bob@example.com"))
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        // Alice creates a group and becomes its owner
        String groupId = JsonPath.read(
                mockMvc.perform(json(post("/api/groups"), """
                                {"name": "Trip to Rome", "currency": "EUR"}
                                """).header(HttpHeaders.AUTHORIZATION, alice))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.memberCount").value(1))
                        .andExpect(jsonPath("$.myRole").value("OWNER"))
                        .andReturn().getResponse().getContentAsString(),
                "$.id");

        // Outsiders can't see it
        mockMvc.perform(get("/api/groups/" + groupId).header(HttpHeaders.AUTHORIZATION, eve))
                .andExpect(status().isNotFound());

        // Alice adds Bob by email (case-insensitive)
        mockMvc.perform(json(post("/api/groups/" + groupId + "/members"), """
                        {"email": "BOB@example.com"}
                        """).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        // Bob sees the group as a member but can't manage it
        mockMvc.perform(get("/api/groups").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].memberCount").value(2))
                .andExpect(jsonPath("$[0].myRole").value("MEMBER"));

        mockMvc.perform(json(patch("/api/groups/" + groupId), """
                        {"name": "Bob's trip"}
                        """).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isForbidden());

        // The owner can't leave before handing over ownership
        mockMvc.perform(delete("/api/groups/" + groupId + "/members/me").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isConflict());

        mockMvc.perform(json(put("/api/groups/" + groupId + "/owner"), """
                        {"userId": "%s"}
                        """.formatted(bobId)).header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/groups/" + groupId + "/members/me").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(status().isNoContent());

        // Bob is now the only member and the owner
        mockMvc.perform(get("/api/groups/" + groupId + "/members").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role").value("OWNER"));

        mockMvc.perform(json(patch("/api/groups/" + groupId), """
                        {"name": "Rome 2026"}
                        """).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rome 2026"));

        mockMvc.perform(delete("/api/groups/" + groupId).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/groups/" + groupId).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCanUpdateProfileAndChangePassword() throws Exception {
        String token = registerAndLogin("frank@example.com", "Frank");

        mockMvc.perform(json(patch("/api/users/me"), """
                        {"displayName": "Franky", "defaultCurrency": "USD"}
                        """).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Franky"))
                .andExpect(jsonPath("$.defaultCurrency").value("USD"));

        mockMvc.perform(json(put("/api/users/me/password"), """
                        {"currentPassword": "wrong-password", "newPassword": "newPassword1"}
                        """).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());

        mockMvc.perform(json(put("/api/users/me/password"), """
                        {"currentPassword": "password123", "newPassword": "newPassword1"}
                        """).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        mockMvc.perform(json(post("/api/auth/login"), """
                        {"email": "frank@example.com", "password": "newPassword1"}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocsArePublicAndDescribeBearerAuth() throws Exception {
        String docs = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andReturn().getResponse().getContentAsString();

        assertThat(docs).doesNotContain("currentUserId");
    }

    private void register(String email, String displayName) throws Exception {
        mockMvc.perform(json(post("/api/users"), """
                        {"email": "%s", "password": "password123", "displayName": "%s"}
                        """.formatted(email, displayName)))
                .andExpect(status().isCreated());
    }

    private String registerAndLogin(String email, String displayName) throws Exception {
        register(email, displayName);

        String body = mockMvc.perform(json(post("/api/auth/login"), """
                        {"email": "%s", "password": "password123"}
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
