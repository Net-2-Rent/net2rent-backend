package com.net2rent.net2rent_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.net2rent.net2rent_backend.dto.LoginRequest;
import com.net2rent.net2rent_backend.dto.request.CreateGuestIncidentRequest;
import com.net2rent.net2rent_backend.dto.request.GuestAccessRequest;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;

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
class GuestIncidentIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String guestAccessAndGetToken(String ref, String pin) throws Exception {
        String body = objectMapper.writeValueAsString(new GuestAccessRequest(ref, pin));
        MvcResult result = mockMvc.perform(post("/api/guest/access")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asString();
    }

    private String staffLoginAndGetToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asString();
    }

    private String json(CreateGuestIncidentRequest req) throws Exception {
        return objectMapper.writeValueAsString(req);
    }

    private CreateGuestIncidentRequest validRequest() {
        return new CreateGuestIncidentRequest(
                "Ana", "López", null, IncidentCategory.ELECTRICITY,
                "No hay luz en el salón desde ayer");
    }

    @Test
    void guestWithValidToken_returns201WithCode() throws Exception {
        String token = guestAccessAndGetToken("APT-1001", "1234");

        mockMvc.perform(post("/api/guest/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/guest/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffTokenCannotUseGuestEndpoint_returns403() throws Exception {
        String token = staffLoginAndGetToken("admin@net2rent.com", "Test1234");

        mockMvc.perform(post("/api/guest/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingRequiredFields_returns409() throws Exception {
        String token = guestAccessAndGetToken("APT-1001", "1234");
        CreateGuestIncidentRequest empty = new CreateGuestIncidentRequest("", "", null, null, "");

        mockMvc.perform(post("/api/guest/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(empty)))
                .andExpect(status().isConflict());
    }

    @Test
    void withoutCategory_returns201() throws Exception {
        String token = guestAccessAndGetToken("APT-1001", "1234");
        CreateGuestIncidentRequest noCategory = new CreateGuestIncidentRequest(
                "Ana", "López", null, null, "No hay luz en el salón desde ayer");

        mockMvc.perform(post("/api/guest/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(noCategory)))
                .andExpect(status().isCreated());
    }
}