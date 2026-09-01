package com.net2rent.net2rent_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.net2rent.net2rent_backend.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IsolationAndAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asString();
    }

    @Test
    void unauthenticatedRequestIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/lodgings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userSeesLodgingOfOwnAccount() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        mockMvc.perform(get("/api/lodgings/1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotSeeLodgingOfAnotherAccount_returns404() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        mockMvc.perform(get("/api/lodgings/2")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}