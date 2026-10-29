package com.hospital.smart_hospital;

import com.hospital.smart_hospital.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AnalyticsIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Login as admin
        var adminLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "TestAdminPassword@123"))))
                .andExpect(status().isOk())
                .andReturn();

        adminSession = (MockHttpSession) adminLoginRes.getRequest().getSession();
    }

    @Test
    @DisplayName("Admin can fetch hospital utilization analytics summary")
    void testGetAnalyticsSummary_Success() throws Exception {
        mockMvc.perform(get("/api/analytics/summary").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalRooms", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.totalBeds", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.overallOccupancyRate", notNullValue()))
                .andExpect(jsonPath("$.data.wardBreakdown", notNullValue()))
                .andExpect(jsonPath("$.data.roomTypeBreakdown", notNullValue()));
    }

    @Test
    @DisplayName("Unauthenticated user cannot access analytics summary")
    void testGetAnalyticsSummary_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isUnauthorized());
    }
}
