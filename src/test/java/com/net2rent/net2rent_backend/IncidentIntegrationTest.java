package com.net2rent.net2rent_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.net2rent.net2rent_backend.dto.LoginRequest;
import com.net2rent.net2rent_backend.dto.request.CreateCommentRequest;
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

    @Test
    void timeline_returnsEventsInOrder_forOwnedIncident() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");

        MvcResult ops = mockMvc.perform(get("/api/users/operators")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode operators = objectMapper.readTree(ops.getResponse().getContentAsString());
        Long operatorId = Long.valueOf(operators.get(0).get("id").asString());

        MvcResult created = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest(1L, operatorId))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = Long.parseLong(
                objectMapper.readTree(created.getResponse().getContentAsString())
                        .get("id").asString());

        mockMvc.perform(get("/api/incidents/" + id + "/timeline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("EVENT"))
                .andExpect(jsonPath("$[0].eventType").value("CREATED"))
                .andExpect(jsonPath("$[1].eventType").value("ASSIGNED"));
    }

    @Test
    void addComment_thenTimeline_showsCommentInterleavedWithEvents() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");

        MvcResult created = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest(1L, null))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = Long.parseLong(
                objectMapper.readTree(created.getResponse().getContentAsString())
                        .get("id").asString());

        String commentBody = objectMapper.writeValueAsString(
                new CreateCommentRequest("Contactado el propietario, envío operario mañana"));
        mockMvc.perform(post("/api/incidents/" + id + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("COMMENT"))
                .andExpect(jsonPath("$.text").value("Contactado el propietario, envío operario mañana"));

        mockMvc.perform(get("/api/incidents/" + id + "/timeline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("EVENT"))
                .andExpect(jsonPath("$[0].eventType").value("CREATED"))
                .andExpect(jsonPath("$[1].type").value("COMMENT"));
    }

    @Test
    void emptyComment_returns409() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        long id = Long.parseLong(objectMapper.readTree(
                        mockMvc.perform(post("/api/incidents")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json(validRequest(1L, null))))
                                .andReturn().getResponse().getContentAsString())
                .get("id").asString());

        mockMvc.perform(post("/api/incidents/" + id + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- resolve (CU-EXE-06/09/10/11) ----------

    private Long createAssignedIncidentId(String token) throws Exception {
        MvcResult ops = mockMvc.perform(get("/api/users/operators")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode operators = objectMapper.readTree(ops.getResponse().getContentAsString());
        Long operatorId = Long.valueOf(operators.get(0).get("id").asString());

        MvcResult created = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest(1L, operatorId))))
                .andExpect(status().isCreated()).andReturn();

        return Long.parseLong(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asString());
    }

    @Test
    void resolve_withValidData_returns200Resolved() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        Long incidentId = createAssignedIncidentId(token);

        mockMvc.perform(patch("/api/incidents/" + incidentId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/incidents/" + incidentId + "/resolve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutes\": 45, \"note\": \"Cambiada la resistencia del termo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.minutesSpent").value(45))
                .andExpect(jsonPath("$.resolutionNote").value("Cambiada la resistencia del termo"));
    }

    @Test
    void resolve_withoutNote_returns409() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        Long incidentId = createAssignedIncidentId(token);

        mockMvc.perform(patch("/api/incidents/" + incidentId + "/resolve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutes\": 30}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("note"))
                .andExpect(jsonPath("$.errors[0].message").value("Debes describir cómo se ha resuelto"));
    }

    @Test
    void resolve_withoutMinutes_returns409() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        Long incidentId = createAssignedIncidentId(token);

        mockMvc.perform(patch("/api/incidents/" + incidentId + "/resolve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": \"Arreglado\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("minutes"))
                .andExpect(jsonPath("$.errors[0].message").value("Debes indicar cuánto tiempo has dedicado"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, 1441})
    void resolve_withMinutesOutOfRange_returns409(int invalidMinutes) throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        Long incidentId = createAssignedIncidentId(token);

        mockMvc.perform(patch("/api/incidents/" + incidentId + "/resolve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutes\": " + invalidMinutes + ", \"note\": \"Arreglado\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].field").value("minutes"))
                .andExpect(jsonPath("$.errors[0].message").value("El tiempo debe estar entre 1 y 1440 minutos"));
    }
}