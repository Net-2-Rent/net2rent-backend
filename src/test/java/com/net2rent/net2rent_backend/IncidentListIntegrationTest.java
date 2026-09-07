package com.net2rent.net2rent_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.net2rent.net2rent_backend.dto.LoginRequest;
import com.net2rent.net2rent_backend.dto.request.CreatePhoneIncidentRequest;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import com.net2rent.net2rent_backend.repository.IncidentRepository;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
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
class IncidentListIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private IncidentRepository incidentRepository;

    @BeforeEach
    void cleanIncidents() {
        incidentRepository.deleteAll();
    }

    // ---------- helpers ----------

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asString();
    }

    private Long firstOperatorId(String token) throws Exception {
        MvcResult ops = mockMvc.perform(get("/api/users/operators")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode operators = objectMapper.readTree(ops.getResponse().getContentAsString());
        return Long.valueOf(operators.get(0).get("id").asString());
    }

    // Creates an incident via the phone endpoint and returns its id.
    private long createIncident(String token, Long lodgingId, IncidentPriority priority, Long assigneeId)
            throws Exception {
        CreatePhoneIncidentRequest req = new CreatePhoneIncidentRequest(
                lodgingId,
                LocalDateTime.now().minusHours(1),
                "Ana", "López",
                null,
                IncidentCategory.ELECTRICITY,
                priority,
                assigneeId,
                "Incidencia de prueba con descripción suficientemente larga");
        MvcResult res = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return Long.parseLong(
                objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asString());
    }

    // ---------- Pagination (20/page, here size=2 to keep it cheap) ----------

    @Test
    void list_paginates() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        createIncident(token, 1L, IncidentPriority.NORMAL, null);
        createIncident(token, 1L, IncidentPriority.NORMAL, null);
        createIncident(token, 1L, IncidentPriority.NORMAL, null);

        mockMvc.perform(get("/api/incidents?size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/api/incidents?page=1&size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.content.length()").value(1));
    }

    // ---------- Priority ordering is by SEVERITY, not alphabetical ----------

    @Test
    void list_sortByPriority_ordersBySeverityNotAlphabetically() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        createIncident(token, 1L, IncidentPriority.NORMAL, null);
        createIncident(token, 1L, IncidentPriority.URGENT, null);
        createIncident(token, 1L, IncidentPriority.LOW, null);
        createIncident(token, 1L, IncidentPriority.HIGH, null);

        mockMvc.perform(get("/api/incidents?sort=priority")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.content[0].priority").value("URGENT"))
                .andExpect(jsonPath("$.page.content[1].priority").value("HIGH"))
                .andExpect(jsonPath("$.page.content[2].priority").value("NORMAL"))
                .andExpect(jsonPath("$.page.content[3].priority").value("LOW"));
    }

    // ---------- Combinable filters ----------

    @Test
    void list_filtersByPriority() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        createIncident(token, 1L, IncidentPriority.URGENT, null);
        createIncident(token, 1L, IncidentPriority.LOW, null);

        mockMvc.perform(get("/api/incidents?priority=URGENT")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.content[0].priority").value("URGENT"));
    }

    // ---------- "sin asignar" ----------

    @Test
    void list_filtersUnassigned() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        Long operatorId = firstOperatorId(token);
        createIncident(token, 1L, IncidentPriority.NORMAL, null);        // pool (NEW, no operator)
        createIncident(token, 1L, IncidentPriority.NORMAL, operatorId);  // assigned

        mockMvc.perform(get("/api/incidents?unassigned=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.content[0].assigneeName").doesNotExist());
    }

    // ---------- Header counters, literal interpretation ----------

    @Test
    void list_headerCounters_countByStatus() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        Long operatorId = firstOperatorId(token);
        createIncident(token, 1L, IncidentPriority.NORMAL, null);        // NEW
        createIncident(token, 1L, IncidentPriority.NORMAL, null);        // NEW
        createIncident(token, 1L, IncidentPriority.NORMAL, operatorId);  // ASSIGNED

        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counters.NEW").value(2))
                .andExpect(jsonPath("$.counters.ASSIGNED").value(1))
                .andExpect(jsonPath("$.counters.IN_PROGRESS").value(0))
                .andExpect(jsonPath("$.counters.PAUSED").value(0))
                .andExpect(jsonPath("$.counters.RESOLVED").value(0));
    }

    @Test
    void list_headerCounters_respectStatusFilter_literalInterpretation() throws Exception {
        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        Long operatorId = firstOperatorId(token);
        createIncident(token, 1L, IncidentPriority.NORMAL, null);
        createIncident(token, 1L, IncidentPriority.NORMAL, operatorId);

        mockMvc.perform(get("/api/incidents?status=NEW")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counters.NEW").value(1))
                .andExpect(jsonPath("$.counters.ASSIGNED").value(0));
    }

    // ---------- Multi-account isolation (never leaks another account's rows) ----------

    @Test
    void list_neverShowsAnotherAccountsIncidents() throws Exception {
        String otherToken = loginAndGetToken("admin@otraempresa.com", "Test1234");
        createIncident(otherToken, 2L, IncidentPriority.NORMAL, null);

        String token = loginAndGetToken("admin@net2rent.com", "Test1234");
        createIncident(token, 1L, IncidentPriority.NORMAL, null);

        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.content[0].lodgingRef").value("APT-1001"));
    }
}