package com.net2rent.net2rent_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.net2rent.net2rent_backend.dto.LoginRequest;
import com.net2rent.net2rent_backend.dto.request.CreatePhoneIncidentRequest;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;

import java.time.LocalDateTime;

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
class IncidentIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asString();
    }

    private String json(CreatePhoneIncidentRequest req) throws Exception {
        return objectMapper.writeValueAsString(req);
    }

    private CreatePhoneIncidentRequest validRequest(Long lodgingId, Long assigneeId) {
        return new CreatePhoneIncidentRequest(
                lodgingId,
                LocalDateTime.now().minusHours(1),
                "Ana", "López",
                null,
                IncidentCategory.ELECTRICITY,
                IncidentPriority.NORMAL,
                assigneeId,
                "No hay luz en el salón desde ayer");
    }

    @Test
    void adminRegistersWithoutOperator_returns201New() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest(1L, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void operatorCannotRegister_returns403() throws Exception {
        String token = loginAndGetToken("operario@net2rent.com", "Test1234");
        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest(1L, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingRequiredFields_returns409() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        CreatePhoneIncidentRequest empty = new CreatePhoneIncidentRequest(
                null, null, "", "", null, null, null, null,"");
        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(empty)))
                .andExpect(status().isConflict());
    }

    @Test
    void lodgingOfAnotherAccount_returns404() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest(2L, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void futureOpenedAt_returns409() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        CreatePhoneIncidentRequest future = new CreatePhoneIncidentRequest(
                1L, LocalDateTime.now().plusDays(1), "Ana", "López", null,
                IncidentCategory.ELECTRICITY, IncidentPriority.NORMAL, null,
                "No hay luz en el salón desde ayer");
        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(future)))
                .andExpect(status().isConflict());
    }

    @Test
    void adminRegistersWithOperator_returns201Assigned() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");

        MvcResult ops = mockMvc.perform(get("/api/users/operators")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode operators = objectMapper.readTree(ops.getResponse().getContentAsString());
        Long operatorId = Long.valueOf(operators.get(0).get("id").asString());

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest(1L, operatorId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }
}