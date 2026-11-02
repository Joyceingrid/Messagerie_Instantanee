package com.messagerie.demo;

import com.messagerie.user.UserRepository;
import jakarta.servlet.Filter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthApiTests {
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired UserRepository users;
    @Autowired ObjectMapper json;

    @Test
    void registerLoginAndReadOwnAccount() throws Exception {
        MockMvc api = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        String username = "test-" + UUID.randomUUID();
        String body = json.writeValueAsString(java.util.Map.of("username", username, "motDePasse", "Test-secret-123"));
        try {
            api.perform(post("/api/comptes").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist());
            api.perform(post("/api/comptes").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict());
            api.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(body.replace("Test-secret-123", "incorrect")))
                    .andExpect(status().isUnauthorized());
            String response = api.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String token = json.readTree(response).get("token").asString();
            api.perform(get("/api/comptes/me")).andExpect(status().isUnauthorized());
            api.perform(get("/api/comptes/me").header("Authorization", "Bearer " + token + "AAAA"))
                    .andExpect(status().isUnauthorized());
            api.perform(get("/api/comptes/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username));
        } finally {
            users.findByUsername(username).ifPresent(users::delete);
        }
    }
}
